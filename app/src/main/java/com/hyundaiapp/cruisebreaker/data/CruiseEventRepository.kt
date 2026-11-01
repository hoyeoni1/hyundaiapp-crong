package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.CruisePass
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent

/**
 * 해제 이벤트·통과 기록 업로드. 실제 구현(서버)은 황준용. 좌표(point)는 보내지 않는다.
 * 규칙(docs/disengage-rules.md "업로드와 차량 ID"): 생길 때마다 실시간으로 부르고, 실패하면 구현이 쌓아 두었다가 다시 보낸다.
 * 가명 차량 ID는 [VehicleId]. 그 전에는 [FakeCruiseServer].
 */
interface CruiseEventRepository {
    suspend fun upload(event: DisengageEvent)
    suspend fun upload(pass: CruisePass)
}
