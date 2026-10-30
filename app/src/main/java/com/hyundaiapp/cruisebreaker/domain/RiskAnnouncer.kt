package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.cruisebreaker.data.SegmentRisk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 지금 화면에 띄울 안내(F4). 배너 한 줄 + TTS 한 문장 */
data class Announcement(val bannerText: String, val speechText: String, val risk: SegmentRisk)

/**
 * 진입 전 음성 안내(F4). 담당: 이민하.
 * 현재 위치·방향 앞 0.8~1.2km, ±30° 안의 위험 구간을 찾아 1회 안내(같은 구간은 한 주행에 1회, 최소 3분 간격).
 * 거리·방향 계산은 core GeoMath. TTS는 TTS SDK 확인 후 연결(안 되면 Android TextToSpeech).
 */
class RiskAnnouncer(private val rules: DisengageRules) {
    private val _current = MutableStateFlow<Announcement?>(null)
    val current: StateFlow<Announcement?> = _current.asStateFlow()

    // TODO(이민하): start(scope, signals, risks)에서 위치를 구독해 안내 판정
}
