package com.hyundaiapp.cruisebreaker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.data.CruiseEventRepository
import com.hyundaiapp.cruisebreaker.data.FakeCruiseServer
import com.hyundaiapp.cruisebreaker.data.PendingDeletions
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.data.SharedPrefsAppPrefs
import com.hyundaiapp.cruisebreaker.data.SupabaseCruiseServer
import com.hyundaiapp.cruisebreaker.data.toUpload
import com.hyundaiapp.cruisebreaker.data.UploadQueue
import com.hyundaiapp.cruisebreaker.domain.DisengageDetector
import com.hyundaiapp.cruisebreaker.domain.DisengageRules
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.cruisebreaker.domain.RiskSpeaker
import com.hyundaiapp.cruisebreaker.domain.TripStats
import com.hyundaiapp.cruisebreaker.domain.TripTracker
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.PleosVehicleSignals
import com.hyundaiapp.core.backend.BackendConfig
import com.hyundaiapp.core.backend.EncryptedBackendStore
import com.hyundaiapp.core.backend.HttpsBackendTransport
import com.hyundaiapp.core.backend.SupabaseBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 하나만 쓰는 객체 모음(간단한 수동 DI).
 * 서버가 준비되면 [FakeCruiseServer]를 실제 구현으로 바꾼다(황준용). 업로드에는 [AppPrefs.vehicleId]가 붙는다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** 화면과 무관하게 앱 수명 동안 도는 작업(해제 감지 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 매니페스트 퍼미션과 맞출 것: CAR_DRIVE → DRIVE, CAR_SPEED → SPEED, NAVI_ROUTE → NAVI
    val signals = PleosVehicleSignals(context, setOf(CoreFeature.DRIVE, CoreFeature.SPEED, CoreFeature.NAVI))

    /** S4 설정·S5 동의 */
    val prefs: AppPrefs = SharedPrefsAppPrefs(context)

    /** 동의한 약관 판 번호(서버에 남김). 약관 문구를 바꾸면 함께 올린다. 지금은 승인 전 초안 docs/terms-draft.md의 번호 */
    private val termsVersion = "cruisebreaker-terms-2026-11-v1"

    // 서버 설정이 없으면 Fake, 있는데 형식이 틀리면(구형 키·http·공백) 죽지 않고 로그만 남기고 Fake(core 0.5.2)
    val backend: SupabaseBackend? = BackendConfig.fromBuildConfigOrNull(
        BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY, "CRUISEBREAKER", BuildConfig.SUPABASE_SIMULATED, termsVersion,
    ) { Log.e("SupabaseConfig", it) }?.let { config ->
        SupabaseBackend(config, EncryptedBackendStore(context, config.storageNamespace), HttpsBackendTransport(config), appScope)
    }
    private val fakeServer = FakeCruiseServer()
    private val remoteServer = backend?.let { SupabaseCruiseServer(it, prefs::vehicleId) { prefs.onboarded } }
    val risks: SegmentRiskRepository = remoteServer ?: fakeServer
    val events: CruiseEventRepository = remoteServer ?: fakeServer

    init {
        if (prefs.onboarded) backend?.activate(prefs.vehicleId())
        backend?.let { b -> appScope.launch { b.status.collect { Log.i("SupabaseSync", it) } } }
    }

    private val rules = DisengageRules()
    val disengageDetector = DisengageDetector(signals, rules)
    val trips = TripTracker(signals)
    val tripStats = TripStats()
    val announcer = RiskAnnouncer(rules)

    /** 동의 철회 때 서버 기록 삭제. 앱 수명 동안 돈다(기록이 멈춰도 삭제는 끝까지 보냄) */
    private val deletions = PendingDeletions(prefs, events, onFailure = { Log.w(TAG, "server delete failed, retrying: $it") })

    /** 시스템 음성 서비스에 붙으므로 기록을 시작할 때 만들고 멈출 때 놓는다(이슈 #11) */
    private var speaker: RiskSpeaker? = null

    /** 앱 화면이 보이는지([MainActivity]가 바꾼다). 안 보일 때(내비가 앞일 때)만 안내를 헤드업 알림으로 띄운다 */
    val appVisible = MutableStateFlow(false)
    private val riskNotification = RiskNotification(context)

    /** 기록 중일 때만 있다. [stopRecording]이 취소하면 기록 관련 코루틴이 모두 멈춘다 */
    private var recordingJob: Job? = null
    private var uploadQueue: UploadQueue? = null

    /**
     * 해제 감지·업로드·주행 구분·안내·음성을 시작한다. [RecordingService]가 부른다. 여러 번 불러도 한 번만 시작.
     * 차량·위치 신호 수신도 여기서 시작한다(동의 전에는 받지 않음, 이슈 #9).
     * 확인: adb logcat -s CruiseRecord
     */
    fun startRecording() {
        if (recordingJob?.isActive == true) return
        backend?.activate(prefs.vehicleId())
        val job = SupervisorJob(appScope.coroutineContext[Job])
        recordingJob = job
        val scope = CoroutineScope(appScope.coroutineContext + job)
        signals.start()

        // 업로드는 대기열을 거친다: 감지기 쪽 수집은 넣기만 해서 늦어지지 않고(PR #3 리뷰), 실제 전송은 한 줄로 차례대로,
        // 실패하면 다시 보내되 영구 거절은 건너뛰고 500건을 넘으면 오래된 것부터 버린다(이슈 #19). 좌표가 없는 형태로 넣는다
        val uploads = UploadQueue(log = { Log.w(TAG, it) }).also { uploadQueue = it }
        scope.launch { uploads.run() }
        // 감지기보다 먼저 구독해야 첫 이벤트를 놓치지 않는다(같은 Main 디스패처에서 순서대로 시작)
        scope.launch {
            disengageDetector.events.collect {
                Log.i(TAG, "disengage ${it.segment} speed=${it.speedKph}")
                val record = it.toUpload(prefs.vehicleId())
                uploads.offer { events.upload(record) }
            }
        }
        scope.launch {
            disengageDetector.passes.collect {
                Log.i(TAG, "pass ${it.segment}")
                val record = it.toUpload(prefs.vehicleId())
                uploads.offer { events.upload(record) }
            }
        }
        disengageDetector.start(scope)

        trips.start(scope)
        scope.launch {
            announcer.announcements.collect {
                Log.i(TAG, "announce ${it.risk.key} ${it.bannerText}")
                if (!appVisible.value) riskNotification.show(it, rules.bannerMs)
            }
        }
        tripStats.start(scope, trips, announcer, disengageDetector)
        RiskSpeaker(appContext).also { speaker = it }.start(scope, announcer.announcements) { prefs.voiceEnabled }
        announcer.start(scope, signals, risks, trips)
    }

    /** 기록을 멈춘다(동의 철회, 이슈 #7). 신호 수신도 끊고 배너를 내린다. [RecordingService.onDestroy]가 부른다 */
    fun stopRecording() {
        val job = recordingJob ?: return
        recordingJob = null
        job.cancel()
        announcer.reset()
        riskNotification.hide()
        speaker?.release()
        speaker = null
        signals.stop()
        if (disengageDetector.dropped > 0) Log.w(TAG, "dropped records: ${disengageDetector.dropped}")
        uploadQueue?.let { q ->
            if (q.dropped + q.rejected + q.size > 0) {
                Log.w(TAG, "uploads not sent: over capacity=${q.dropped} rejected=${q.rejected} left=${q.size}")
            }
        }
        uploadQueue = null
        Log.i(TAG, "recording stopped")
    }

    /**
     * 동의 철회(이슈 #7, 앱 개발 규정 "철회 시 즉시 활용 중단·저장 데이터 삭제"):
     * 기록 서비스를 멈추고, 기기에 남은 동의·가명 차량 ID·주행 요약을 지운다. 다시 동의하면 새 차량 ID로 시작한다.
     * 서버에 올라간 이 차량의 기록은 [PendingDeletions]가 삭제를 요청한다(성공할 때까지, PRD 5.6).
     */
    fun withdrawConsent() {
        val vehicleId = prefs.vehicleId()
        appContext.stopService(Intent(appContext, RecordingService::class.java))
        stopRecording()
        tripStats.reset()
        // Persist the backend revocation before the legacy preferences forget this identity.
        try { backend?.withdraw(vehicleId) } finally {
            prefs.clearConsentAndData()
            deletions.resume(appScope)
        }
        Log.i(TAG, "consent withdrawn, server deletions pending=${prefs.pendingDeletions.size}")
    }

    /** 앱이 켜질 때 부른다: 지난번에 못 끝낸 서버 삭제 요청을 이어서 보낸다 */
    fun resumePendingDeletions() = deletions.resume(appScope)

    private companion object {
        const val TAG = "CruiseRecord"
    }
}
