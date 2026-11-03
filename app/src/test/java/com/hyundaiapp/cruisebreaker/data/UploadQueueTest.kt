package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.retrying
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UploadQueueTest {
    private val sent = mutableListOf<String>()

    private fun ok(name: String): suspend () -> Unit = { sent += name }

    @Test
    fun `영구 거절된 기록은 건너뛰고 뒤 기록은 올라간다`() = runTest(UnconfinedTestDispatcher()) {
        val queue = UploadQueue()
        backgroundScope.launch { queue.run() }

        queue.offer { throw PermanentFailureException("HTTP 400") }
        queue.offer(ok("b"))

        assertEquals(listOf("b"), sent)
        assertEquals(1, queue.rejected)
    }

    @Test
    fun `일시 실패는 다시 보내서 성공하고, 그동안 뒤 기록은 순서대로 기다린다`() = runTest(UnconfinedTestDispatcher()) {
        val queue = UploadQueue()
        backgroundScope.launch { queue.run() }
        var tries = 0

        queue.offer {
            if (++tries < 3) throw IOException("offline")
            sent += "a"
        }
        queue.offer(ok("b"))
        assertTrue(sent.isEmpty())

        advanceTimeBy(5_001)
        advanceTimeBy(15_001)
        assertEquals(listOf("a", "b"), sent)
        assertEquals(0, queue.rejected)
    }

    @Test
    fun `상한을 넘으면 가장 오래된 기록부터 버린다`() = runTest(UnconfinedTestDispatcher()) {
        val queue = UploadQueue(capacity = 2)
        // run을 띄우기 전에 3건 → 첫 건이 버려짐
        queue.offer(ok("a"))
        queue.offer(ok("b"))
        queue.offer(ok("c"))
        assertEquals(1, queue.dropped)

        backgroundScope.launch { queue.run() }
        assertEquals(listOf("b", "c"), sent)
        assertEquals(0, queue.size)
    }

    @Test
    fun `직렬화 오류는 영구 실패, 네트워크 오류는 아니다`() {
        assertTrue(isPermanentFailure(IllegalArgumentException("bad json")))
        assertTrue(isPermanentFailure(PermanentFailureException("HTTP 422")))
        assertEquals(false, isPermanentFailure(IOException("timeout")))
    }

    @Test
    fun `retrying은 영구 실패를 다시 시도하지 않고 던진다`() = runTest {
        var calls = 0
        val thrown = runCatching {
            retrying(isPermanent = ::isPermanentFailure) {
                calls++
                throw PermanentFailureException("HTTP 401")
            }
        }.exceptionOrNull()
        assertEquals(1, calls)
        assertTrue(thrown is PermanentFailureException)
    }
}
