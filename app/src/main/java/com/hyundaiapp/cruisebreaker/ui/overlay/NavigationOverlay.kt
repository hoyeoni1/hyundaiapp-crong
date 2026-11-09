package com.hyundaiapp.cruisebreaker.ui.overlay

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.ui.CarUxRestrictionSource
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.roundToInt

/** 기록 서비스 소유. Activity가 사라져도 유지되며 서비스 종료/동의 철회 시 즉시 분리한다. */
class NavigationOverlay(
    private val context: Context,
    private val appPrefs: AppPrefs,
    private val signals: VehicleSignals,
    private val repository: SegmentRiskRepository,
) {
    private val prefs = OverlayPrefs(context)
    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var recording = false
    private var scope: CoroutineScope? = null
    private var nearby: NearbyCards? = null
    private var dataJob: Job? = null
    private var state = NearbyCardState()
    private var foreground = false
    private var restricted: Boolean? = null
    private var limits: OverlayLimits? = null
    private val contentPolicy = OverlayContentPolicy()
    private var card: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var lastRendered: Any? = null
    private var dragging = false
    private var holding = false
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private val beginDrag = Runnable { if (holding && movable()) dragging = true }
    private val ux = CarUxRestrictionSource(context,
        onChanged = { restricted = it; reconcile() },
        onDetails = { limits = OverlayLimits.from(it) },
    )
    private val changes = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == "overlay_enabled") syncEnabled() else reconcile()
    }
    private val ops = context.getSystemService(AppOpsManager::class.java)
    private val permissionChanges = AppOpsManager.OnOpChangedListener { _, packageName ->
        if (packageName == null || packageName == context.packageName) handler.post { reconcile() }
    }

    fun start() {
        if (recording) return
        recording = true
        prefs.storage.registerOnSharedPreferenceChangeListener(changes)
        syncEnabled()
    }

    private fun syncEnabled() {
        if (!recording || !appPrefs.onboarded || !prefs.enabled) {
            deactivate()
            return
        }
        if (scope != null) return
        val current = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = current
        ops.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, context.packageName, permissionChanges)
        ux.start()
        current.launch { signals.drive.collect { reconcile() } }
        current.launch { signals.sources.collect { reconcile() } }
        current.launch { while (isActive) { reconcile(); delay(1_000) } }
    }

    fun stop() {
        if (!recording) return
        recording = false
        prefs.storage.unregisterOnSharedPreferenceChangeListener(changes)
        deactivate()
    }

    private fun deactivate() {
        val current = scope ?: return
        scope = null
        current.cancel()
        stopData()
        nearby = null
        contentPolicy.reset()
        ops.stopWatchingMode(permissionChanges)
        ux.stop()
        remove()
    }

    fun setForeground(value: Boolean) { foreground = value; reconcile() }

    /** Compose가 실제 차지한 앱 영역만 저장. GNB/Driving view를 전체 화면으로 추정하지 않는다. */
    fun recordAppArea(left: Int, top: Int, right: Int, bottom: Int) {
        if (!appPrefs.onboarded || right <= left || bottom <= top) return
        val metrics = metrics()
        if (left < 0 || top < 0 || right > metrics.widthPixels || bottom > metrics.heightPixels) return
        val encoded = "$left,$top,$right,$bottom,${metrics.widthPixels},${metrics.heightPixels},${metrics.densityDpi}"
        if (prefs.storage.getString("overlay_area", null) != encoded) {
            prefs.storage.edit().putString("overlay_area", encoded).apply()
        }
    }

    @Suppress("DEPRECATION")
    private fun metrics() = DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }

    private fun area(): CardArea? {
        val saved = prefs.storage.getString("overlay_area", null)?.split(',')?.mapNotNull { it.toIntOrNull() } ?: return null
        if (saved.size != 7) return null
        val m = metrics()
        if (saved[4] != m.widthPixels || saved[5] != m.heightPixels || saved[6] != m.densityDpi) return null
        return CardArea(saved[0], saved[1], saved[2], saved[3]).takeIf {
            it.left >= 0 && it.top >= 0 && it.right <= m.widthPixels && it.bottom <= m.heightPixels && it.width > 0 && it.height > 0
        }
    }

    private fun movable() = overlayMovementAllowed(signals.drive.value, restricted, signals.sources.value.vehicle)

    private fun reconcile() {
        val current = scope ?: return
        // 카드에는 여러 줄이 있으므로 P단·정차·제한 해제가 확인된 동안만 표시하고 조회한다.
        if (foreground || !movable() || !Settings.canDrawOverlays(context) || area() == null || limits?.canDisplay != true) {
            stopData()
            remove()
            return
        }
        if (card == null) attach()
        layout()
        if (card == null) {
            stopData()
            return
        }
        if (dataJob == null) {
            val source = nearby ?: NearbyCards(signals, repository).also { nearby = it }
            dataJob = current.launch {
                coroutineScope {
                    source.start(this)
                    source.state.collect { state = it; render() }
                }
            }
        }
        render()
    }

    private fun stopData() {
        val job = dataJob ?: return
        dataJob = null
        job.cancel()
        state = NearbyCardState()
    }

    @SuppressLint("ClickableViewAccessibility", "RtlHardcoded") // 절대 화면 좌표로 배치하므로 LEFT 사용
    private fun attach() {
        val bounds = area() ?: return
        val density = context.resources.displayMetrics.density
        val width = (600 * density).roundToInt().coerceAtMost(bounds.width)
        val height = (268 * density * context.resources.configuration.fontScale.coerceAtLeast(1f)).roundToInt()
        if (width < (360 * density).roundToInt() || height > bounds.height) return
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 29, 35)); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(255, 176, 32))
            }
            setOnTouchListener { _, event -> touch(event) }
        }
        params = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags(), PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            alpha = .8f // Android 12+의 비신뢰 오버레이 터치 통과 상한 이내
            setTitle("CruiseBreakerNearbyCard")
            val position = bounds.position(prefs.x, prefs.y, width, height)
            x = position.first; y = position.second
        }
        try {
            wm.addView(view, params)
            card = view
            lastRendered = null
            Log.i(TAG, "attached movable=${movable()}")
        } catch (e: RuntimeException) {
            params = null
            Log.w(TAG, "overlay unavailable", e)
        }
    }

    private fun flags() = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        (if (movable()) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

    private fun layout() {
        val view = card ?: return
        val lp = params ?: return
        val bounds = area() ?: return
        if (lp.width > bounds.width || lp.height > bounds.height) { remove(); return }
        lp.flags = flags()
        if (!dragging) {
            val p = bounds.position(prefs.x, prefs.y, lp.width, lp.height)
            lp.x = p.first; lp.y = p.second
        }
        try { wm.updateViewLayout(view, lp) } catch (_: RuntimeException) { remove() }
    }

    private fun render() {
        val view = card ?: return
        val currentLimits = limits ?: return
        val rows = contentPolicy.rows(state.rows, currentLimits)
        val display = state.copy(rows = rows)
        val rendered = display to currentLimits
        if (rendered == lastRendered) return
        lastRendered = rendered
        view.removeAllViews()
        fun line(text: String, size: Float, color: Int) {
            view.addView(TextView(context).apply {
                this.text = limitedOverlayText(text, currentLimits.maxStringLength); textSize = size; setTextColor(color)
                isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(4), 0, dp(4))
            })
        }
        line("근처 해제 다발 구간", 26f, Color.WHITE)
        display.message?.let { line(it, 23f, Color.LTGRAY) }
        display.rows.forEach { row ->
            val distance = row.distanceM ?: return@forEach
            val label = if (distance < 1000) "${distance.roundToInt()}m" else String.format(Locale.KOREA, "%.1fkm", distance / 1000)
            // 거리/해제율을 고정 폭으로 확보해 긴 구간 이름이 수치를 밀어내지 않게 한다.
            val strip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4)) }
            fun cell(value: String, width: Int, weight: Float = 0f) {
                strip.addView(TextView(context).apply {
                    text = limitedOverlayText(value, currentLimits.maxStringLength); textSize = 23f; setTextColor(Color.WHITE); isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT, weight))
            }
            cell(label, dp(90)); cell(row.roadName, 0, 1f); cell("${(row.rate * 100).roundToInt()}%", dp(64))
            view.addView(strip)
        }
        line("직선거리 · 길게 눌러 이동", 18f, Color.LTGRAY)
    }

    private fun touch(event: MotionEvent): Boolean {
        if (!movable()) { cancelDrag(); return false }
        val lp = params ?: return false
        val bounds = area() ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                holding = true; dragging = false; downX = event.rawX; downY = event.rawY
                startX = lp.x; startY = lp.y
                handler.postDelayed(beginDrag, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && kotlin.math.hypot(event.rawX - downX, event.rawY - downY) > ViewConfiguration.get(context).scaledTouchSlop) cancelDrag()
                if (dragging) {
                    lp.x = (startX + (event.rawX - downX).roundToInt()).coerceIn(bounds.left, bounds.right - lp.width)
                    lp.y = (startY + (event.rawY - downY).roundToInt()).coerceIn(bounds.top, bounds.bottom - lp.height)
                    layout()
                }
            }
            MotionEvent.ACTION_UP -> {
                val save = dragging
                cancelDrag()
                if (save) {
                    prefs.position((lp.x - bounds.left).toFloat() / (bounds.width - lp.width).coerceAtLeast(1),
                        (lp.y - bounds.top).toFloat() / (bounds.height - lp.height).coerceAtLeast(1))
                    Log.i(TAG, "position saved x=${lp.x} y=${lp.y}")
                }
                layout()
            }
            MotionEvent.ACTION_CANCEL -> { cancelDrag(); layout() }
        }
        return true
    }

    private fun cancelDrag() { holding = false; dragging = false; handler.removeCallbacks(beginDrag) }
    private fun remove() {
        cancelDrag()
        card?.let { runCatching { wm.removeViewImmediate(it) }; Log.i(TAG, "removed") }
        card = null; params = null; lastRendered = null
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
    private companion object { const val TAG = "CruiseOverlay" }
}
