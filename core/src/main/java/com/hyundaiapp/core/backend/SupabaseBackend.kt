package com.hyundaiapp.core.backend

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One instance per app. Each consent generation keeps its own identity until deletion is acknowledged. */
class SupabaseBackend(
    val config: BackendConfig,
    private val store: BackendStore,
    private val transport: BackendTransport,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    startWorker: Boolean = true,
) {
    private val guard = Any()
    private val network = Mutex()
    /** 상태 파일을 읽지 못했거나 내용이 깨져 빈 상태로 시작했는지. [status]의 첫 값이 "local-storage-reset"이 된다 */
    private var storageReset = false
    private val state = loadState()
    private val generations get() = state.getJSONArray("generations")
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()
    private val mutableStatus = MutableStateFlow(if (storageReset) "local-storage-reset" else "idle")
    val status = mutableStatus.asStateFlow()

    init {
        if (startWorker) scope.launch(Dispatchers.IO) {
            while (isActive) {
                try { while (syncOnce()) { /* drain ready work */ } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { mutableStatus.value = "local-storage-error" }
                withTimeoutOrNull(5_000) { wake.receive() }
            }
        }
    }

    /**
     * 저장된 상태를 읽는다. 읽기 예외(복호화 실패)나 JSON 오류면 빈 상태로 시작한다 — 복구할 수 없는 상태 때문에
     * 앱이 켜질 때마다 죽는 것을 막는다. 보내지 못한 작업·철회 대기는 그 경우 사라지므로 [status]로 알린다.
     */
    private fun loadState(): JSONObject = try {
        JSONObject(store.read() ?: EMPTY_STATE).also { it.getJSONArray("generations") }
    } catch (e: Exception) {
        storageReset = true
        JSONObject(EMPTY_STATE)
    }

    private fun find(vehicle: String): JSONObject? = (0 until generations.length())
        .map { generations.getJSONObject(it) }.firstOrNull { it.getString("vehicle") == vehicle }
    private fun save(changed: Boolean = false) {
        store.write(state.toString())
        if (changed) mutableRevision.value++
    }
    private fun copy(vehicle: String): JSONObject = synchronized(guard) {
        JSONObject(requireNotNull(find(vehicle)) { "Consent generation missing" }.toString())
    }
    private fun active(vehicle: String): JSONObject = copy(vehicle).also { check(!it.getBoolean("revoked")) { "Consent withdrawn" } }

    /** Called only after explicit app consent; does not block on network. */
    fun activate(vehicle: String) = synchronized(guard) {
        UUID.fromString(vehicle)
        if (find(vehicle) == null) {
            generations.put(JSONObject().put("vehicle", vehicle).put("revoked", false).put("installation", "")
                .put("tasks", JSONArray()).put("next", 0L).put("attempts", 0))
            save(true)
        } else check(!find(vehicle)!!.getBoolean("revoked")) { "Use a new identity after withdrawal" }
        wake.trySend(Unit)
        Unit
    }

    /** Durable revocation is saved before app preferences/vehicleId are cleared. */
    fun withdraw(vehicle: String) = synchronized(guard) {
        find(vehicle)?.let {
            it.put("revoked", true).put("tasks", JSONArray()).put("next", 0L).put("attempts", 0)
            save(true)
            wake.trySend(Unit)
        }
        Unit
    }

    /** Legacy deletion queues may remove their receipt only after the server ACK removed this generation. */
    suspend fun awaitWithdrawal(vehicle: String) {
        revision.first { synchronized(guard) { find(vehicle) == null } }
    }

    suspend fun enqueue(vehicle: String, method: String, key: String, payload: JSONObject) = withContext(Dispatchers.IO) {
        require(method in WRITES)
        synchronized(guard) {
            val g = requireNotNull(find(vehicle)) { "Explicit consent required" }
            check(!g.getBoolean("revoked")) { "Consent withdrawn" }
            val tasks = g.getJSONArray("tasks")
            if (method == "pleos_retract_failure") {
                val session = payload.getString("p_session_id")
                for (n in tasks.length() - 1 downTo 0) {
                    val p = tasks.getJSONObject(n).getJSONObject("payload")
                    if (p.optString("p_id") == session || p.optString("p_session_id") == session) tasks.remove(n)
                }
            }
            val existing = (0 until tasks.length()).map { tasks.getJSONObject(it) }.firstOrNull { it.getString("key") == key }
            if (existing == null) {
                check(tasks.length() < 10_000) { "Outbox full" }
                tasks.put(JSONObject().put("key", key).put("method", method).put("payload", JSONObject(payload.toString()))
                    .put("failed", false))
                g.put("next", 0L)
                save(true)
            }
            wake.trySend(Unit)
        }
    }

    /** Read a caller-owned RPC. Registration/refresh are serialized with writes. */
    suspend fun query(vehicle: String, method: String, payload: JSONObject): String = withContext(Dispatchers.IO) {
        require(method in READS)
        network.withLock {
            active(vehicle)
            ensureInstallation(vehicle)
            val p = JSONObject(payload.toString())
            if (method == "pleos_my_charges") p.put("p_installation_id", active(vehicle).getString("installation"))
            val response = authenticated(vehicle, method, p)
            active(vehicle) // A read finishing after withdrawal must not repopulate UI caches.
            response
        }
    }

    /** A step is public for deterministic persistence/retry tests; normally the worker drives this. */
    suspend fun syncOnce(): Boolean = withContext(Dispatchers.IO) {
        network.withLock {
            val candidate = synchronized(guard) {
                (0 until generations.length()).map { generations.getJSONObject(it) }.firstOrNull { g ->
                    g.optLong("next") <= clock() && (g.getBoolean("revoked") || g.optString("installation").isEmpty() ||
                        (0 until g.getJSONArray("tasks").length()).any { !g.getJSONArray("tasks").getJSONObject(it).getBoolean("failed") })
                }?.let { JSONObject(it.toString()) }
            } ?: return@withLock false
            val vehicle = candidate.getString("vehicle")
            try {
                mutableStatus.value = "syncing"
                ensureInstallation(vehicle)
                val current = copy(vehicle)
                if (current.getBoolean("revoked")) {
                    authenticated(vehicle, "pleos_withdraw_installation", JSONObject().put("p_installation_id", current.getString("installation")))
                    synchronized(guard) {
                        for (n in generations.length() - 1 downTo 0) if (generations.getJSONObject(n).getString("vehicle") == vehicle) generations.remove(n)
                        save(true)
                    }
                } else {
                    val task = current.getJSONArray("tasks").let { tasks ->
                        (0 until tasks.length()).map { tasks.getJSONObject(it) }.firstOrNull { !it.getBoolean("failed") }
                    }
                    if (task != null) {
                        val body = task.getJSONObject("payload").put("p_installation_id", current.getString("installation"))
                        // Withdrawal may have happened while registration was in flight.
                        active(vehicle)
                        try {
                            authenticated(vehicle, task.getString("method"), body)
                            synchronized(guard) {
                                find(vehicle)?.let { g ->
                                    val tasks = g.getJSONArray("tasks")
                                    for (n in tasks.length() - 1 downTo 0) if (tasks.getJSONObject(n).getString("key") == task.getString("key")) tasks.remove(n)
                                    g.put("attempts", 0).put("next", 0L)
                                    save(true)
                                }
                            }
                        } catch (e: BackendException) {
                            if (e.status in setOf(400, 403, 404, 409, 422)) {
                                synchronized(guard) {
                                    find(vehicle)?.getJSONArray("tasks")?.let { tasks ->
                                        for (n in 0 until tasks.length()) if (tasks.getJSONObject(n).getString("key") == task.getString("key"))
                                            tasks.getJSONObject(n).put("failed", true).put("error", "${e.status}:${e.code}")
                                        save(true)
                                    }
                                }
                                mutableStatus.value = "rejected:${e.status}:${e.code}"
                                return@withLock true
                            }
                            throw e
                        }
                    }
                }
                mutableStatus.value = "connected"
                true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                synchronized(guard) {
                    find(vehicle)?.let { g ->
                        val tries = g.optInt("attempts") + 1
                        g.put("attempts", tries).put("next", clock() + RETRY[minOf(tries - 1, RETRY.lastIndex)])
                        save()
                    }
                }
                mutableStatus.value = if (e is BackendException) "retry:${e.status}:${e.code}" else "retry:network-or-state"
                false
            }
        }
    }

    private fun ensureInstallation(vehicle: String) {
        if (copy(vehicle).optString("installation").isNotEmpty()) return
        val result = authenticated(vehicle, "pleos_register_installation", JSONObject()
            .put("p_app", config.app).put("p_vehicle_id", vehicle).put("p_consent_version", config.consentVersion)
            .put("p_simulated", config.simulated))
        val id = result.trim().trim('"')
        UUID.fromString(id)
        synchronized(guard) { find(vehicle)!!.put("installation", id); save() }
    }

    private fun authenticated(vehicle: String, method: String, payload: JSONObject): String {
        refresh(vehicle, false)
        var result = transport.post("/rest/v1/rpc/$method", payload.toString(), copy(vehicle).getString("access"))
        if (result.status == 401) {
            refresh(vehicle, true)
            result = transport.post("/rest/v1/rpc/$method", payload.toString(), copy(vehicle).getString("access"))
        }
        checkResponse(result)
        return result.body
    }

    private fun refresh(vehicle: String, force: Boolean) {
        val g = copy(vehicle)
        if (!force && g.optString("access").isNotEmpty() && g.optLong("expires") > clock() + 60_000) return
        val token = g.optString("refresh")
        val path = if (token.isEmpty()) "/auth/v1/signup" else "/auth/v1/token?grant_type=refresh_token"
        val payload = if (token.isEmpty()) JSONObject().put("data", JSONObject()) else JSONObject().put("refresh_token", token)
        val result = transport.post(path, payload.toString(), null)
        checkResponse(result)
        val json = JSONObject(result.body)
        synchronized(guard) {
            find(vehicle)!!.put("access", json.getString("access_token")).put("refresh", json.getString("refresh_token"))
                .put("expires", clock() + json.getLong("expires_in") * 1000)
            save() // Persist rotating refresh tokens before making another request.
        }
    }

    private fun checkResponse(response: BackendResponse) {
        if (response.status !in 200..299) {
            val code = runCatching { JSONObject(response.body).optString("code", "request_failed") }.getOrDefault("request_failed")
            throw BackendException(response.status, code.take(80))
        }
    }

    companion object {
        private const val EMPTY_STATE = "{\"generations\":[]}"
        private val RETRY = longArrayOf(5_000, 15_000, 60_000, 300_000)
        private val WRITES = setOf("pleos_upload_charge", "pleos_submit_review", "pleos_retract_failure", "pleos_upload_pass", "pleos_upload_disengage")
        private val READS = setOf("pleos_my_charges", "pleos_nearby_stations", "pleos_station_detail", "pleos_nearby_risk")
    }
}
