package com.hyundaiapp.cruisebreaker

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.hyundaiapp.core.backend.BackendConfig
import com.hyundaiapp.core.backend.EncryptedBackendStore
import com.hyundaiapp.core.backend.HttpsBackendTransport
import com.hyundaiapp.core.backend.SupabaseBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Shell-only debug integration fixture. Uses production client/store; never changes app consent. */
class BackendConnectionCheckActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Checking Supabase connection..." })
        val resultFile = File(filesDir, "backend-check-result.json")
        resultFile.writeText("{\"running\":true}")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val report = JSONObject()
            val checks = JSONArray()
            val vehicle = UUID.randomUUID().toString()
            report.put("vehicle_id", vehicle)
            var backend: SupabaseBackend? = null
            try {
                val app = if (packageName.endsWith("chachelin")) "CHACHELIN" else "CRUISEBREAKER"
                val config = BackendConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY, app, true)
                val store = EncryptedBackendStore(applicationContext, config.storageNamespace + "-check")
                fun client() = SupabaseBackend(config, store, HttpsBackendTransport(config), scope, startWorker = false)
                backend = client()
                backend.activate(vehicle)
                val query = JSONObject().put("p_lat", 37.4127).put("p_lon", 127.0947).put("p_radius_m", 10000).put("p_demo", true)
                val method = if (app == "CHACHELIN") "pleos_nearby_stations" else "pleos_nearby_risk"
                check(JSONArray(backend.query(vehicle, method, query)).length() > 0)
                checks.put("anonymous-auth-and-shared-query")
                val installation = JSONObject(store.read()!!).getJSONArray("generations").let { a ->
                    (0 until a.length()).map { a.getJSONObject(it) }.first { it.getString("vehicle") == vehicle }.getString("installation")
                }
                report.put("installation_id", installation)
                backend = client() // Real encrypted-file reload, same owner/installation.
                check(JSONArray(backend.query(vehicle, method, query)).length() > 0)
                checks.put("android-keystore-encrypted-session-reload")
                if (app == "CHACHELIN") {
                    val id = UUID.randomUUID().toString()
                    backend.enqueue(vehicle, "pleos_upload_charge", "charge:$id", JSONObject()
                        .put("p_id", id).put("p_station_id", "st-003")
                        .put("p_started_at", Instant.now().minusSeconds(3600).toString())
                        .put("p_ended_at", Instant.now().minusSeconds(1800).toString())
                        .put("p_soc_start", 40).put("p_soc_end", 60).put("p_avg_kw", 30)
                        .put("p_result", "SUCCESS").put("p_time_scale", 120))
                    repeat(10) { backend.syncOnce() }
                    check(JSONArray(backend.query(vehicle, "pleos_my_charges", JSONObject())).length() == 1)
                    backend.enqueue(vehicle, "pleos_submit_review", "review:$id", JSONObject()
                        .put("p_session_id", id).put("p_rating", 5).put("p_tags", JSONArray().put("FAST")))
                    repeat(10) { backend.syncOnce() }
                } else {
                    backend.enqueue(vehicle, "pleos_upload_pass", "pass:test", JSONObject().put("p_segment_id", "wydku4g")
                        .put("p_heading8", 0).put("p_date", LocalDate.now(ZoneId.of("Asia/Seoul")).toString()))
                    repeat(10) { backend.syncOnce() }
                    backend.enqueue(vehicle, "pleos_upload_disengage", "event:test", JSONObject().put("p_segment_id", "wydku4g")
                        .put("p_heading8", 0).put("p_hour", Instant.ofEpochMilli(System.currentTimeMillis() / 3600000 * 3600000).toString()))
                    repeat(10) { backend.syncOnce() }
                }
                checks.put("persistent-outbox-and-real-rpc-writes")
                backend.withdraw(vehicle)
                repeat(10) { backend.syncOnce() }
                val remains = JSONObject(store.read()!!).getJSONArray("generations")
                check((0 until remains.length()).none { remains.getJSONObject(it).getString("vehicle") == vehicle })
                checks.put("withdrawal-acknowledged-and-local-credentials-removed")
                report.put("ok", true)
            } catch (e: Exception) {
                report.put("backend_status", backend?.status?.value).put("ok", false).put("error", e.javaClass.simpleName + ":" + e.message.orEmpty().take(160))
                try { backend?.withdraw(vehicle); backend?.syncOnce() } catch (_: Exception) { report.put("cleanup_pending", true) }
            } finally {
                report.put("checks", checks)
                resultFile.writeText(report.toString())
                android.util.Log.i("BackendCheck", report.toString())
                runOnUiThread { finish() }
            }
        }
    }
}