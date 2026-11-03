package com.hyundaiapp.cruisebreaker.data

import android.annotation.SuppressLint
import android.content.Context
import java.util.UUID

/** 앱 설정·동의 상태·가명 차량 ID(S4·S5). 기기에만 저장한다 */
interface AppPrefs {
    /** S4 음성 안내 */
    var voiceEnabled: Boolean

    /** S4 자동 기록(시동 때 기록 서비스 자동 시작). S5 동의 후 기본 켜짐(PRD 5.4) */
    var autoRecordEnabled: Boolean

    /** S5 필수 동의 2개 */
    var locationAgreed: Boolean
    var privacyAgreed: Boolean

    /** 필수 동의를 마쳤는지. 기록은 이게 true일 때만 한다 */
    val onboarded: Boolean get() = locationAgreed && privacyAgreed

    /**
     * 가명 차량 ID(docs/disengage-rules.md "업로드와 차량 ID"). 설치마다 무작위 UUID, 처음 부를 때 만든다.
     * 기기 정보·차대번호(VIN)·계정은 쓰지 않는다. 서버는 이 값으로 차량-일 중복을 제거한다.
     */
    fun vehicleId(): String

    /**
     * 동의 철회(이슈 #7, 앱 개발 규정 "철회 시 즉시 중단·삭제"): 동의 2개와 가명 차량 ID를 지운다.
     * 다시 동의하면 새 차량 ID가 만들어져 이전 기록과 이어지지 않는다. 음성·자동 기록 설정은 기본값으로 돌린다.
     * 지운 차량 ID는 같은 저장에서 [pendingDeletions]로 옮긴다 — 서버 기록 삭제(PRD 5.6, 이슈 #12)가 성공할 때까지
     * 남겨 두기 위해서다. 중간에 앱이 죽어도 ID를 잃지 않는다.
     */
    fun clearConsentAndData()

    /** 서버 기록 삭제를 아직 못 끝낸 가명 차량 ID. 철회 뒤에 남는 유일한 값 */
    val pendingDeletions: Set<String>

    /** 서버 삭제가 끝나면 부른다 */
    fun removePendingDeletion(vehicleId: String)
}

class SharedPrefsAppPrefs(context: Context) : AppPrefs {
    private val prefs = context.getSharedPreferences("cruisebreaker", Context.MODE_PRIVATE)

    override var voiceEnabled: Boolean
        get() = prefs.getBoolean(VOICE, true)
        set(v) = prefs.edit().putBoolean(VOICE, v).apply()
    override var autoRecordEnabled: Boolean
        get() = prefs.getBoolean(AUTO_RECORD, true)
        set(v) = prefs.edit().putBoolean(AUTO_RECORD, v).apply()
    override var locationAgreed: Boolean
        get() = prefs.getBoolean(LOCATION, false)
        set(v) = prefs.edit().putBoolean(LOCATION, v).apply()
    override var privacyAgreed: Boolean
        get() = prefs.getBoolean(PRIVACY, false)
        set(v) = prefs.edit().putBoolean(PRIVACY, v).apply()

    @Synchronized
    override fun vehicleId(): String {
        prefs.getString(VEHICLE_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(VEHICLE_ID, id).apply()
        return id
    }

    @SuppressLint("ApplySharedPref")
    @Synchronized
    override fun clearConsentAndData() {
        val pending = pendingDeletions + listOfNotNull(prefs.getString(VEHICLE_ID, null))
        // 철회는 바로 반영돼야 하므로 commit(동기 저장). clear()는 같은 commit 안의 put보다 먼저 적용된다
        prefs.edit().clear().putStringSet(PENDING_DELETIONS, pending).commit()
    }

    override val pendingDeletions: Set<String>
        get() = prefs.getStringSet(PENDING_DELETIONS, emptySet()).orEmpty().toSet()

    @SuppressLint("ApplySharedPref")
    @Synchronized
    override fun removePendingDeletion(vehicleId: String) {
        prefs.edit().putStringSet(PENDING_DELETIONS, pendingDeletions - vehicleId).commit()
    }

    private companion object {
        const val VOICE = "voice_enabled"
        const val AUTO_RECORD = "auto_record_enabled"
        const val LOCATION = "location_agreed"
        const val PRIVACY = "privacy_agreed"

        // 옛 VehicleId 객체와 같은 키(이미 설치된 앱의 ID를 그대로 쓴다)
        const val VEHICLE_ID = "vehicle_id"
        const val PENDING_DELETIONS = "pending_server_deletions"
    }
}

/** 테스트·미리보기용 */
class InMemoryAppPrefs(
    override var voiceEnabled: Boolean = true,
    override var autoRecordEnabled: Boolean = true,
    override var locationAgreed: Boolean = false,
    override var privacyAgreed: Boolean = false,
) : AppPrefs {
    private var id: String? = null
    private val pending = mutableSetOf<String>()

    override fun vehicleId(): String = id ?: UUID.randomUUID().toString().also { id = it }

    override fun clearConsentAndData() {
        id?.let { pending += it }
        voiceEnabled = true
        autoRecordEnabled = true
        locationAgreed = false
        privacyAgreed = false
        id = null
    }

    override val pendingDeletions: Set<String> get() = pending.toSet()

    override fun removePendingDeletion(vehicleId: String) {
        pending -= vehicleId
    }
}
