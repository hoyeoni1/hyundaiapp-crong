package com.hyundaiapp.cruisebreaker.data

import android.content.Context

/** 앱 설정·동의 상태(S4·S5). 기기에만 저장하고 서버로 보내지 않는다 */
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
}

class SharedPrefsAppPrefs(context: Context) : AppPrefs {
    private val prefs = context.getSharedPreferences("cruisebreaker", Context.MODE_PRIVATE)

    override var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(v) = prefs.edit().putBoolean("voice_enabled", v).apply()
    override var autoRecordEnabled: Boolean
        get() = prefs.getBoolean("auto_record_enabled", true)
        set(v) = prefs.edit().putBoolean("auto_record_enabled", v).apply()
    override var locationAgreed: Boolean
        get() = prefs.getBoolean("location_agreed", false)
        set(v) = prefs.edit().putBoolean("location_agreed", v).apply()
    override var privacyAgreed: Boolean
        get() = prefs.getBoolean("privacy_agreed", false)
        set(v) = prefs.edit().putBoolean("privacy_agreed", v).apply()
}

/** 테스트·미리보기용 */
class InMemoryAppPrefs(
    override var voiceEnabled: Boolean = true,
    override var autoRecordEnabled: Boolean = true,
    override var locationAgreed: Boolean = false,
    override var privacyAgreed: Boolean = false,
) : AppPrefs
