package com.hyundaiapp.cruisebreaker.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** 서버 요청 재시도 간격(차슐랭 `ChargeSessionRecorder`와 같음). 끝까지 쓰면 마지막 값으로 계속 시도한다 */
val RETRY_DELAYS_MS: List<Long> = listOf(5_000L, 15_000L, 60_000L, 300_000L)

/** [block]이 성공할 때까지 [delaysMs] 간격으로 다시 시도한다. 취소되면 멈춘다. 실패할 때마다 [onFailure] */
suspend fun retrying(
    delaysMs: List<Long> = RETRY_DELAYS_MS,
    onFailure: (Throwable) -> Unit = {},
    block: suspend () -> Unit,
) {
    var attempt = 0
    while (true) {
        try {
            block()
            return
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onFailure(e)
            delay(delaysMs[minOf(attempt, delaysMs.lastIndex)])
            attempt++
        }
    }
}
