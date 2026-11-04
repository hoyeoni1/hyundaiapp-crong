package com.hyundaiapp.core.backend

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SupabaseBackendTest {
    private val vehicle = "10000000-0000-4000-8000-000000000001"
    private val nextVehicle = "10000000-0000-4000-8000-000000000002"
    private val config = BackendConfig("https://example.supabase.co", "sb_publishable_test", "CHACHELIN", true)
    private class Memory : BackendStore {
        var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
    }

    @Test fun projectAppAndDemoModeDoNotShareStoredCredentials() {
        assertNotEquals(config.storageNamespace, config.copy(simulated = false).storageNamespace)
        assertNotEquals(config.storageNamespace, config.copy(app = "CRUISEBREAKER").storageNamespace)
        assertNotEquals(config.storageNamespace, config.copy(url = "https://other.supabase.co").storageNamespace)
        assertEquals(config.storageNamespace, config.copy(url = config.url + "/").storageNamespace)
    }
    private class Server : BackendTransport {
        val calls = mutableListOf<String>()
        val bodies = mutableListOf<JSONObject>()
        var signupCount = 0
        var refreshCount = 0
        var failMethod = ""
        var failureStatus = 500
        var callback: ((String) -> Unit)? = null
        override fun post(path: String, body: String, accessToken: String?): BackendResponse {
            calls += path
            bodies += JSONObject(body)
            callback?.invoke(path)
            if (path.endsWith(failMethod) && failMethod.isNotEmpty()) return BackendResponse(failureStatus, "{\"code\":\"test_error\"}")
            return when {
                path.contains("signup") -> { signupCount++; BackendResponse(200, "{\"access_token\":\"access-$signupCount\",\"refresh_token\":\"refresh-$signupCount\",\"expires_in\":3600}") }
                path.contains("grant_type=refresh_token") -> { refreshCount++; BackendResponse(200, "{\"access_token\":\"new-access\",\"refresh_token\":\"rotated-refresh\",\"expires_in\":3600}") }
                path.endsWith("pleos_register_installation") -> BackendResponse(200, "\"20000000-0000-4000-8000-00000000000$signupCount\"")
                path.endsWith("pleos_my_charges") -> BackendResponse(200, "[]")
                else -> BackendResponse(204, "")
            }
        }
    }

    @Test fun unreadableOrCorruptStoreStartsEmptyInsteadOfCrashing() = runTest {
        val throwing = object : BackendStore {
            override fun read(): String = throw IllegalStateException("decrypt failed")
            override fun write(value: String) {}
        }
        val backend = SupabaseBackend(config, throwing, Server(), backgroundScope, startWorker = false)
        assertEquals("local-storage-reset", backend.status.value)
        backend.activate(vehicle)

        val corrupt = Memory().apply { value = "not json" }
        val restarted = SupabaseBackend(config, corrupt, Server(), backgroundScope, startWorker = false)
        assertEquals("local-storage-reset", restarted.status.value)
        restarted.activate(vehicle)
        assertTrue(corrupt.value!!.contains(vehicle))

        val healthy = SupabaseBackend(config, Memory(), Server(), backgroundScope, startWorker = false)
        assertEquals("idle", healthy.status.value)
    }

    @Test fun invalidBuildConfigBecomesNullWithoutLeakingTheKey() {
        val reasons = mutableListOf<String>()
        val log: (String) -> Unit = { reasons += it }
        assertNull(BackendConfig.fromBuildConfigOrNull("", "", "CHACHELIN", true, "v1", log))
        assertTrue(reasons.isEmpty()) // URL 없음 = 서버 없이 쓰는 빌드, 오류 아님
        assertNull(BackendConfig.fromBuildConfigOrNull("http://example.supabase.co", "sb_publishable_x", "CHACHELIN", true, "v1", log))
        assertNull(BackendConfig.fromBuildConfigOrNull("https://example.supabase.co", "eyJhbGciOi.legacy.jwt", "CHACHELIN", true, "v1", log))
        assertNull(BackendConfig.fromBuildConfigOrNull("not a url", "sb_publishable_x", "CHACHELIN", true, "v1", log))
        assertEquals(3, reasons.size)
        assertFalse(reasons.any { it.contains("eyJhbGciOi") })

        val ok = BackendConfig.fromBuildConfigOrNull(" https://example.supabase.co ", "sb_publishable_x\n", "CHACHELIN", true, "v1", log)
        assertEquals("https://example.supabase.co", ok?.url)
        assertEquals("sb_publishable_x", ok?.publishableKey)
        assertEquals(3, reasons.size)
    }

    @Test fun noSignupBeforeConsentAndNoSignupAgainOnRestart() = runTest {
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        assertFalse(backend.syncOnce()); assertEquals(0, server.signupCount)
        backend.activate(vehicle); backend.syncOnce()
        val restarted = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        restarted.query(vehicle, "pleos_my_charges", JSONObject())
        assertEquals(1, server.signupCount)
    }

    @Test fun registrationSendsConfiguredConsentVersion() = runTest {
        val server = Server()
        val backend = SupabaseBackend(config.copy(consentVersion = "chachelin-terms-test"), Memory(), server, backgroundScope, startWorker = false)
        backend.activate(vehicle); backend.query(vehicle, "pleos_my_charges", JSONObject())
        val register = server.bodies[server.calls.indexOfFirst { it.endsWith("pleos_register_installation") }]
        assertEquals("chachelin-terms-test", register.getString("p_consent_version"))
    }

    @Test fun queuedWriteSurvivesRestartAndAccepts204() = runTest {
        val store = Memory(); val server = Server()
        val first = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        first.activate(vehicle)
        first.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject().put("p_id", "one"))
        val restarted = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        assertTrue(restarted.syncOnce())
        assertTrue(server.calls.last().endsWith("pleos_upload_charge"))
        assertEquals(0, JSONObject(store.value!!).getJSONArray("generations").getJSONObject(0).getJSONArray("tasks").length())
    }

    @Test fun retriesPreservePayloadAndWaitForBackoff() = runTest {
        var now = 1_000_000L
        val store = Memory(); val server = Server().apply { failMethod = "pleos_upload_charge" }
        val backend = SupabaseBackend(config, store, server, backgroundScope, { now }, false)
        backend.activate(vehicle)
        backend.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject().put("p_id", "one").put("p_soc_start", 40))
        assertFalse(backend.syncOnce())
        val calls = server.calls.size
        assertFalse(backend.syncOnce()); assertEquals(calls, server.calls.size)
        now += 5_000; server.failMethod = ""
        assertTrue(backend.syncOnce())
        assertEquals(40, server.bodies.last().getInt("p_soc_start"))
    }

    @Test fun reviewIsSentAfterItsChargeAndDuplicatePendingWritesDoNotMultiply() = runTest {
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        backend.activate(vehicle)
        repeat(2) { backend.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject().put("p_id", "one")) }
        backend.enqueue(vehicle, "pleos_submit_review", "review:one", JSONObject().put("p_session_id", "one"))
        while (backend.syncOnce()) { }
        assertEquals(1, server.calls.count { it.endsWith("pleos_upload_charge") })
        assertTrue(server.calls.indexOfLast { it.endsWith("pleos_upload_charge") } < server.calls.indexOfLast { it.endsWith("pleos_submit_review") })
    }

    @Test fun expiryRefreshesAndPersistsRotatingTokenInsteadOfCreatingNewUser() = runTest {
        var now = 1_000_000L
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, { now }, false)
        backend.activate(vehicle); backend.syncOnce(); now += 3_600_000
        backend.query(vehicle, "pleos_my_charges", JSONObject())
        assertEquals(1, server.signupCount); assertEquals(1, server.refreshCount)
        assertEquals("rotated-refresh", JSONObject(store.value!!).getJSONArray("generations").getJSONObject(0).getString("refresh"))
    }

    @Test fun retractRemovesUnsentChargeAndUsesServerReceiptForLateRequests() = runTest {
        val server = Server(); val backend = SupabaseBackend(config, Memory(), server, backgroundScope, startWorker = false)
        backend.activate(vehicle)
        backend.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject().put("p_id", "one"))
        backend.enqueue(vehicle, "pleos_retract_failure", "retract:one", JSONObject().put("p_session_id", "one"))
        while (backend.syncOnce()) { }
        assertFalse(server.calls.any { it.endsWith("pleos_upload_charge") })
        assertTrue(server.calls.last().endsWith("pleos_retract_failure"))
    }

    @Test fun withdrawalPersistedBeforeRestartRejectsNewWritesAndDeletesOldIdentity() = runTest {
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        backend.activate(vehicle); backend.syncOnce(); backend.withdraw(vehicle)
        val restarted = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        try { restarted.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject()); fail("Revoked write accepted") } catch (_: IllegalStateException) { }
        restarted.syncOnce()
        assertTrue(server.calls.last().endsWith("pleos_withdraw_installation"))
        assertEquals(0, JSONObject(store.value!!).getJSONArray("generations").length())
    }

    @Test fun deletionReceiptWaitsForAckAndSurvivesTransientFailure() = runTest {
        var now = 1_000_000L
        val server = Server()
        val backend = SupabaseBackend(config, Memory(), server, backgroundScope, { now }, false)
        backend.activate(vehicle); backend.syncOnce(); backend.withdraw(vehicle)
        val receipt = async(start = CoroutineStart.UNDISPATCHED) { backend.awaitWithdrawal(vehicle) }
        assertFalse(receipt.isCompleted)
        server.failMethod = "pleos_withdraw_installation"
        assertFalse(backend.syncOnce()); assertFalse(receipt.isCompleted)
        server.failMethod = ""; now += 5_000; backend.syncOnce()
        receipt.await()
        // Already acknowledged / never-registered legacy identities are also safe to retry.
        backend.awaitWithdrawal(vehicle)
        backend.awaitWithdrawal(nextVehicle)
    }

    @Test fun newConsentDoesNotEraseOfflineDeletionCredentials() = runTest {
        var now = 1_000_000L
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, { now }, false)
        backend.activate(vehicle); backend.syncOnce(); backend.withdraw(vehicle)
        server.failMethod = "pleos_withdraw_installation"; assertFalse(backend.syncOnce())
        backend.activate(nextVehicle); backend.syncOnce()
        assertEquals(2, JSONObject(store.value!!).getJSONArray("generations").length())
        server.failMethod = ""; now += 5_000; backend.syncOnce()
        assertEquals(nextVehicle, JSONObject(store.value!!).getJSONArray("generations").getJSONObject(0).getString("vehicle"))
    }

    @Test fun permanentRejectionIsKeptForDiagnosisWithoutAnInfiniteRetry() = runTest {
        val store = Memory(); val server = Server().apply { failMethod = "pleos_submit_review"; failureStatus = 400 }
        val backend = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        backend.activate(vehicle); backend.enqueue(vehicle, "pleos_submit_review", "review:one", JSONObject())
        backend.syncOnce(); val calls = server.calls.size
        assertFalse(backend.syncOnce()); assertEquals(calls, server.calls.size)
        assertTrue(JSONObject(store.value!!).getJSONArray("generations").getJSONObject(0).getJSONArray("tasks").getJSONObject(0).getBoolean("failed"))
    }

    @Test fun inFlightWriteCannotEraseDurableWithdrawalIntent() = runTest {
        val store = Memory(); val server = Server()
        val backend = SupabaseBackend(config, store, server, backgroundScope, startWorker = false)
        backend.activate(vehicle); backend.enqueue(vehicle, "pleos_upload_charge", "charge:one", JSONObject())
        server.callback = { path -> if (path.endsWith("pleos_upload_charge")) backend.withdraw(vehicle) }
        backend.syncOnce(); backend.syncOnce()
        assertTrue(server.calls.last().endsWith("pleos_withdraw_installation"))
        assertEquals(0, JSONObject(store.value!!).getJSONArray("generations").length())
    }
}
