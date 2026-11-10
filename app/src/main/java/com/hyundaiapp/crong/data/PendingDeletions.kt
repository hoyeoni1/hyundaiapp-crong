package com.hyundaiapp.crong.data

import com.hyundaiapp.crong.domain.RETRY_DELAYS_MS
import com.hyundaiapp.crong.domain.retrying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 동의 철회 때 서버 기록 삭제(PRD 5.6, 11-02 결정, 이슈 #12). 담당: 이민하.
 * 삭제할 가명 차량 ID는 [AppPrefs.pendingDeletions]에 저장돼 있다(철회와 같은 저장). 성공할 때까지 5초·15초·1분·5분 간격으로
 * 다시 보내고, 앱이 죽었다 켜지면 [resume]으로 이어서 보낸다. 성공하면 저장에서 지운다.
 *
 * 업로드와 달리 영구 거절([isPermanentFailure])이어도 포기하지 않는다(이슈 #19 4번, 11-03 결정): 삭제는 앱 개발 규정상
 * 반드시 끝나야 하고, 포기하면 지울 ID가 기기에서 사라져 다시 지울 방법이 없다. 5분 간격으로 계속 보내며 로그를 남긴다.
 * 서버 삭제 함수는 지울 것이 없어도 성공해야 한다(같은 요청 반복 가능, 황준용).
 */
class PendingDeletions(
    private val prefs: AppPrefs,
    private val repository: CruiseEventRepository,
    private val delaysMs: List<Long> = RETRY_DELAYS_MS,
    private val onFailure: (Throwable) -> Unit = {},
) {
    private val running = mutableMapOf<String, Job>()

    /** 저장된 삭제 요청을 모두 보낸다(이미 보내는 중인 ID는 건너뜀). 철회 직후·앱 시작 때 부른다 */
    fun resume(scope: CoroutineScope) {
        for (id in prefs.pendingDeletions) {
            if (running[id]?.isActive == true) continue
            running[id] = scope.launch {
                retrying(delaysMs, onFailure) { repository.deleteMine(id) }
                prefs.removePendingDeletion(id)
                running.remove(id)
            }
        }
    }
}
