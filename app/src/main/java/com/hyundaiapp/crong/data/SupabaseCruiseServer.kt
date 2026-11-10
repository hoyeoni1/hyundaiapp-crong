package com.hyundaiapp.crong.data

import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.backend.SupabaseBackend
import com.hyundaiapp.crong.domain.SegmentKey
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class SupabaseCruiseServer(
    private val backend: SupabaseBackend,
    private val vehicleId: () -> String,
    private val consented: () -> Boolean,
) : CruiseEventRepository, SegmentRiskRepository {
    override suspend fun upload(event: DisengageUpload) {
        backend.enqueue(event.vehicleId, "pleos_upload_disengage", "event:${event.segmentId}:${event.heading8}:${event.hourEpochMs}",
            JSONObject().put("p_segment_id", event.segmentId).put("p_heading8", event.heading8)
                .put("p_hour", Instant.ofEpochMilli(event.hourEpochMs).toString()))
    }
    override suspend fun upload(pass: PassUpload) {
        backend.enqueue(pass.vehicleId, "pleos_upload_pass", "pass:${pass.segmentId}:${pass.heading8}:${pass.date}",
            JSONObject().put("p_segment_id", pass.segmentId).put("p_heading8", pass.heading8).put("p_date", pass.date))
    }
    override suspend fun deleteMine(vehicleId: String) {
        backend.withdraw(vehicleId)
        backend.awaitWithdrawal(vehicleId)
    }
    override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> {
        if (!consented()) return emptyList()
        val raw = backend.query(vehicleId(), "pleos_nearby_risk", JSONObject().put("p_lat", center.latitude)
            .put("p_lon", center.longitude).put("p_radius_m", radiusM).put("p_demo", backend.config.simulated))
        val array = JSONArray(raw)
        return (0 until array.length()).map { index -> array.getJSONObject(index).let { row ->
            SegmentRisk(SegmentKey(row.getString("segment_id"), row.getInt("heading8")),
                GeoPoint(row.getDouble("center_lat"), row.getDouble("center_lon")),
                if (row.isNull("road_name")) "" else row.getString("road_name"), row.getInt("disengages"),
                row.getInt("pass_vehicles"), row.getDouble("rate"), 0.0, row.getBoolean("is_risk"))
        } }
    }
}
