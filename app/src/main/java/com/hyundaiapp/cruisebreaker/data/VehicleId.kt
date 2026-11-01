package com.hyundaiapp.cruisebreaker.data

import android.content.Context
import java.util.UUID

/**
 * 가명 차량 ID(docs/disengage-rules.md "업로드와 차량 ID", 11-01 결정).
 * 앱 설치마다 무작위 UUID 하나. 기기 정보·차대번호(VIN)·계정은 쓰지 않는다. 앱을 지우면 새 ID가 된다.
 * 서버는 이 값으로 차량-일 중복을 제거한다. 업로드에 붙이는 것은 실제 저장소 구현(황준용)이 한다.
 */
object VehicleId {
    private const val PREFS = "cruisebreaker"
    private const val KEY = "vehicle_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
