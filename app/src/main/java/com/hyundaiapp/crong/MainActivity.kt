package com.hyundaiapp.crong

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.hyundaiapp.core.Gear
import com.hyundaiapp.crong.ui.CrongNav
import com.hyundaiapp.crong.ui.theme.CrongTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// DO 선언 앱: 주행 중에도 화면이 보인다 → 주행 중 화면 규정(PRD 5.5) 준수
class MainActivity : ComponentActivity() {
    private val container get() = (application as CrongApp).container

    /** 안내 때문에 자동으로 앞에 올라온 상태([RiskNotification.bringToFront]). 배너가 끝나면 스스로 뒤로 간다 */
    private var autoRaised = false
    private var returnJob: Job? = null

    // 화면이 보이는 동안에는 헤드업 알림 대신 S1 배너로 안내한다
    override fun onStart() {
        super.onStart()
        container.appVisible.value = true
        // 안내 때문에 올라왔으면(RiskNotification.bringToFront) 배너가 끝날 때 스스로 뒤로 간다.
        // 인텐트는 전달되지 않을 수 있어(Pleos가 작업만 앞으로 올림, 11-05 실측) 앱 안의 상태로 알린다
        if (container.autoRaised.getAndSet(false)) scheduleReturn()
    }

    override fun onStop() {
        container.appVisible.value = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 동의를 마친 상태면 기록 서비스를 켠다(앞에 있을 때만 포그라운드 서비스를 시작할 수 있음)
        RecordingService.startIfAllowed(this)
        setContent {
            CrongTheme {
                // Pleos 에뮬레이터에서 아래 시스템 바(140px)가 앱 창 아래를 덮는데, 창은 바까지 그리고 inset도 주지 않는다.
                // 설정의 화면 높이(screenHeightDp)만 바를 뺀 값이라 그 높이로 맞춰 아래 고정 버튼이 바에 가리지 않게 한다(11-04 확인)
                Box(Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp)) {
                    CrongNav((application as CrongApp).container, onExit = { finish() })
                }
            }
        }
    }

    /**
     * 안내 때문에 올라왔으면 배너(10초)가 내려갈 때 내비 등 이전 화면으로 돌아간다(11-05 장호연 요청: 팝업처럼).
     * 그사이 정차(P)했으면 주행 요약을 보여 줘야 하므로 돌아가지 않는다.
     */
    private fun scheduleReturn() {
        autoRaised = true
        returnJob?.cancel()
        returnJob = lifecycleScope.launch {
            val current = container.announcer.current
            withTimeoutOrNull(RETURN_MAX_MS) {
                current.first { it != null }
                current.first { it == null }
            }
            val gear = container.signals.drive.value.gear
            // 내비(지도 앱)로 돌아간다. 앞에 있는 동안은 다른 앱을 띄울 수 있고, 이 액티비티는 뒤에 살아 있어 정차 때 다시 올라올 수 있다.
            // moveTaskToBack·finish는 Pleos 홈이 이전 앱을 안 보여 주거나(앞), 작업이 사라져 다시 못 올라온다(뒤)(11-05 실측)
            val moved = if (autoRaised && gear != Gear.PARK) returnToMaps() else false
            android.util.Log.i("CruiseRecord", "auto return: gear=$gear moved=$moved")
            autoRaised = false
        }
    }

    private fun returnToMaps(): Boolean = runCatching {
        startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse { moveTaskToBack(true) }

    companion object {
        /** 배너가 안 올라오거나 안 내려가도 이 시간 뒤에는 돌아간다 */
        private const val RETURN_MAX_MS = 15_000L
    }
}
