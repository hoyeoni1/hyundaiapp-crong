package com.hyundaiapp.crong.data

import com.hyundaiapp.crong.domain.RETRY_DELAYS_MS
import com.hyundaiapp.crong.domain.retrying
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel

/**
 * 해제·통과 업로드 대기열(이슈 #19). 담당: 이민하. 기록 세션마다 하나 만들고 [run]을 띄운다.
 * - 한 줄로 차례대로 보낸다. 실패하면 5초·15초·1분·5분 간격으로 같은 기록을 다시 보낸다
 * - 서버가 영영 받지 않을 기록([isPermanentFailure])은 그 기록만 버리고 다음으로 넘어간다 → 한 건이 뒤를 막지 않음
 * - [capacity]를 넘으면 가장 오래된 기록부터 버린다 → 서버가 오래 안 돼도 메모리가 끝없이 늘지 않음
 * 버린 수는 [dropped]·[rejected]로 센다(기록을 멈출 때 로그).
 */
class UploadQueue(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val delaysMs: List<Long> = RETRY_DELAYS_MS,
    private val log: (String) -> Unit = {},
) {
    private val items = ArrayDeque<suspend () -> Unit>()
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** 상한을 넘어 보내지 못하고 버린 수 */
    var dropped = 0
        private set

    /** 서버가 영구 거절해 버린 수 */
    var rejected = 0
        private set

    val size: Int get() = synchronized(items) { items.size }

    /** 넣기만 하고 바로 돌아온다(감지기 쪽 수집이 늦어지지 않게) */
    fun offer(send: suspend () -> Unit) {
        synchronized(items) {
            if (items.size >= capacity) {
                items.removeFirst()
                dropped++
            }
            items.addLast(send)
        }
        wake.trySend(Unit)
    }

    /** 기록 세션 scope에서 띄운다. 취소되면 남은 기록과 함께 멈춘다 */
    suspend fun run() {
        while (true) {
            val next = synchronized(items) { items.removeFirstOrNull() }
            if (next == null) {
                wake.receive()
                continue
            }
            try {
                retrying(delaysMs, onFailure = { log("upload failed, retrying: $it") }, isPermanent = ::isPermanentFailure) { next() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                rejected++
                log("upload rejected permanently, skipped: $e")
            }
        }
    }

    companion object {
        /** 약 75km를 크루즈로 달린 통과 기록(150m 칸 기준). 서버가 오래 안 될 때 메모리 상한 */
        const val DEFAULT_CAPACITY = 500
    }
}
