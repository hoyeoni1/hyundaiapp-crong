package com.hyundaiapp.cruisebreaker.domain

import android.content.Context
import android.media.AudioAttributes
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 안내 음성(F4). 담당: 이민하.
 * Pleos TTS SDK(기기 내 모드). Android 기본 TextToSpeech는 에뮬레이터에 엔진이 없어 초기화가 실패한다(11-01 확인).
 * SDK는 시스템 음성 서비스(`ai.pleos.playground.caas` 4.1.29 이상)에 붙는다. 퍼미션 `pleos.car.permission.TTS_SERVICE`.
 */
class RiskSpeaker(context: Context) {
    // SDK 스레드(onSuccess)에서 쓰고 코루틴에서 읽는다(이슈 #8)
    @Volatile
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)

    init {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                ready = true
                // "ko"만 주면 엔진이 "unsupported locale: ko-"로 실패한다(11-01 에뮬레이터). 국가까지 준다
                tts.setLocale(Locale.KOREA)
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                Log.i(TAG, "tts ready (sdk ${TextToSpeech.version})")
            }

            override fun onFailure(error: ErrorType) {
                Log.w(TAG, "tts init failed: $error")
            }
        })
        tts.addEventListener(object : EventListener {
            override fun onStart() = Log.i(TAG, "tts start").let { }
            override fun onReady() = Unit
            override fun onDone() = Log.i(TAG, "tts done").let { }
            override fun onError(error: ErrorType) = Log.w(TAG, "tts error: $error").let { }
            override fun onStop() = Unit
            override fun onUpdatedRms(rms: Double) = Unit
        })
    }

    /** [announcements]가 올 때마다 읽는다. [enabled]가 false면(S4 음성 끔) 읽지 않는다 */
    fun start(scope: CoroutineScope, announcements: Flow<Announcement>, enabled: () -> Boolean = { true }) {
        scope.launch {
            announcements.collect { a ->
                if (!ready || !enabled()) {
                    Log.i(TAG, "skip speech ready=$ready")
                    return@collect
                }
                tts.speak(a.speechText)
                Log.i(TAG, "speak ${a.speechText}")
            }
        }
    }

    /** 시스템 음성 서비스 연결을 놓는다(기록 중지·동의 철회). 다시 쓰려면 새로 만든다 */
    fun release() {
        ready = false
        tts.release()
    }

    private companion object {
        const val TAG = "CruiseRecord"
    }
}
