package com.hyundaiapp.cruisebreaker.ui

import com.hyundaiapp.core.CruiseMode
import com.hyundaiapp.core.FakeVehicleSignals
import com.hyundaiapp.core.Gear
import com.hyundaiapp.cruisebreaker.data.FakeCruiseServer
import com.hyundaiapp.cruisebreaker.data.InMemoryAppPrefs
import com.hyundaiapp.cruisebreaker.data.toUpload
import com.hyundaiapp.cruisebreaker.domain.DisengageDetector
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent
import com.hyundaiapp.cruisebreaker.domain.DisengageRules
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.cruisebreaker.domain.TripStats
import com.hyundaiapp.cruisebreaker.domain.TripTracker
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingViewModel
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListViewModel
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsViewModel
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelsTest {
    private val fake = FakeVehicleSignals()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `S2 위험 구간만 가까운 순으로, 내가 남긴 해제가 다시 들어오면 반영된다`() = runTest {
        val server = FakeCruiseServer()
        val vm = RiskListViewModel(fake, server)
        vm.refresh()

        val items = vm.state.value.items
        assertFalse(vm.state.value.loading)
        assertTrue(items.first().roadName.startsWith("시연 지점"))
        assertEquals(items.sortedBy { it.distanceM }, items)

        val demo = server.nearby(fake.location.value!!.point, 1_000.0).first { it.roadName.startsWith("시연 지점") }
        server.upload(DisengageEvent(0L, demo.key, 100f, demo.center).toUpload("v1"))
        vm.refresh()
        assertEquals(demo.disengages + 1, server.nearby(demo.center, 10.0).single().disengages)
    }

    @Test
    fun `S3 주행 요약은 안내와 해제를 세고 새 주행이면 0부터`() = runTest(UnconfinedTestDispatcher()) {
        val rules = DisengageRules()
        val trips = TripTracker(fake)
        val detector = DisengageDetector(fake, rules, clock = { testScheduler.currentTime })
        val announcer = RiskAnnouncer(rules, clock = { testScheduler.currentTime })
        val stats = TripStats()
        trips.start(backgroundScope)
        stats.start(backgroundScope, trips, announcer, detector)
        detector.start(backgroundScope)
        announcer.start(backgroundScope, fake, FakeCruiseServer(), trips)
        val vm = TripSummaryViewModel(stats)
        backgroundScope.launch { vm.state.collect {} }

        fake.setDrive { copy(gear = Gear.DRIVE, speedKph = 100f) }
        fake.setCruise { copy(mode = CruiseMode.ACTIVATED) }
        fake.setLocation(37.41271, 127.0947) // 시연 지점 안내
        advanceTimeBy(5_000)
        fake.setCruise { copy(mode = CruiseMode.ENABLED) } // 해제 기록

        assertEquals(TripSummaryUiState(riskSegmentsPassed = 1, announcements = 1, myContributions = 1), vm.state.value)

        fake.setDrive { copy(gear = Gear.PARK) }
        assertEquals(1, vm.state.value.announcements) // 정차 중에는 마지막 주행 값 유지
        fake.setDrive { copy(gear = Gear.DRIVE) }
        assertEquals(TripSummaryUiState(), vm.state.value)
    }

    @Test
    fun `S4 설정은 바로 저장되고 안내 거리는 500m 고정`() {
        val prefs = InMemoryAppPrefs()
        val vm = SettingsViewModel(prefs)
        assertEquals(500, vm.state.value.announceDistanceM)

        vm.setVoiceEnabled(false)
        vm.setAutoRecordEnabled(false)
        assertFalse(prefs.voiceEnabled)
        assertFalse(prefs.autoRecordEnabled)
        assertFalse(vm.state.value.voiceEnabled)
    }

    @Test
    fun `S5 필수 동의 2개가 있어야 시작되고, 차속 권한은 없어도 된다`() {
        val prefs = InMemoryAppPrefs()
        var started = 0
        val vm = OnboardingViewModel(prefs, speedPermissionGranted = { false }, onStarted = { started++ })

        assertFalse(vm.start())
        vm.agreeLocation()
        assertFalse(vm.start())
        vm.agreePrivacy()
        assertTrue(vm.start())
        assertEquals(1, started)
        assertTrue(prefs.onboarded)
        assertFalse(vm.state.value.speedPermissionGranted)
    }

    @Test
    fun `S4 동의 철회하면 철회 동작을 부르고 S5는 빈 동의로 다시 보인다`() {
        val prefs = InMemoryAppPrefs(locationAgreed = true, privacyAgreed = true, voiceEnabled = false)
        val oldId = prefs.vehicleId()
        val onboarding = OnboardingViewModel(prefs, speedPermissionGranted = { true })
        var withdrawn = 0
        val settings = SettingsViewModel(prefs, onWithdrawConsent = { withdrawn++; prefs.clearConsentAndData() })

        settings.withdrawConsent()
        onboarding.reload()

        assertEquals(1, withdrawn)
        assertFalse(prefs.onboarded)
        assertTrue(settings.state.value.voiceEnabled) // 기본값으로
        assertEquals(OnboardingUiState(false, false, true), onboarding.state.value)
        assertTrue(prefs.vehicleId() != oldId) // 다시 동의하면 새 차량 ID
    }

    @Test
    fun `S5 다시 열면 저장된 동의가 그대로 보인다`() {
        val prefs = InMemoryAppPrefs(locationAgreed = true, privacyAgreed = true)
        val vm = OnboardingViewModel(prefs, speedPermissionGranted = { true })
        assertEquals(OnboardingUiState(true, true, true), vm.state.value)
    }

}
