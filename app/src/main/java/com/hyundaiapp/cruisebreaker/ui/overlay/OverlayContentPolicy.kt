package com.hyundaiapp.cruisebreaker.ui.overlay

import android.car.drivingstate.CarUxRestrictions
import com.hyundaiapp.cruisebreaker.ui.RiskRowUi

/** Only limits relevant to this read-only, non-scrolling card. Other flags prohibit absent features. */
internal data class OverlayLimits(val maxItems: Int = Int.MAX_VALUE, val maxStringLength: Int = Int.MAX_VALUE) {
    val canDisplay get() = maxItems > 0 && maxStringLength >= 8

    companion object {
        fun from(value: CarUxRestrictions?): OverlayLimits? {
            value ?: return null
            if (!value.isRequiresDistractionOptimization) return OverlayLimits()
            val flags = value.activeRestrictions
            return OverlayLimits(
                maxItems = if (flags and CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT != 0) {
                    if (value.maxContentDepth < 1) 0 else value.maxCumulativeContentItems.coerceAtLeast(0)
                } else Int.MAX_VALUE,
                maxStringLength = if (flags and CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH != 0) {
                    value.maxRestrictedStringLength.coerceAtLeast(0)
                } else Int.MAX_VALUE,
            )
        }
    }
}

/** Count distinct segments across a restricted session, not just the three currently visible rows. */
internal class OverlayContentPolicy {
    private val admitted = linkedSetOf<String>()

    fun rows(rows: List<RiskRowUi>, limits: OverlayLimits): List<RiskRowUi> {
        if (limits.maxItems == Int.MAX_VALUE) {
            admitted.clear()
            return rows.take(3)
        }
        rows.forEach { if (admitted.size < limits.maxItems) admitted.add(it.id) }
        val allowed = admitted.take(limits.maxItems).toSet()
        return rows.filter { it.id in allowed }.take(3)
    }

    fun reset() = admitted.clear()
}

/** Count UTF-16 units conservatively, without leaving a dangling surrogate before the ellipsis. */
internal fun limitedOverlayText(value: String, maxLength: Int): String {
    if (value.length <= maxLength) return value
    if (maxLength <= 0) return ""
    var end = maxLength - 1
    if (end > 0 && value[end - 1].isHighSurrogate()) end--
    return value.take(end) + "…"
}
