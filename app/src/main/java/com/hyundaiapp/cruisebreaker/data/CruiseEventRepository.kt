package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.CruisePass
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent

/** 해제 이벤트·통과 기록 업로드. 실제 구현(서버)은 황준용. 좌표(point)는 보내지 않는다 */
interface CruiseEventRepository {
    suspend fun upload(event: DisengageEvent)
    suspend fun upload(pass: CruisePass)
}

class FakeCruiseEventRepository : CruiseEventRepository {
    val events = mutableListOf<DisengageEvent>()
    val passes = mutableListOf<CruisePass>()

    override suspend fun upload(event: DisengageEvent) {
        events += event
    }

    override suspend fun upload(pass: CruisePass) {
        passes += pass
    }
}
