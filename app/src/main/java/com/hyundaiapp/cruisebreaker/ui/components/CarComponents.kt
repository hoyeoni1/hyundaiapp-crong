package com.hyundaiapp.cruisebreaker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import java.util.Locale

/*
 * 두 앱 공통 화면 부품. 크루즈브레이커 CarComponents.kt와 같은 내용(패키지만 다름).
 * 부품은 상태를 갖지 않는다. 값과 콜백만 받는다.
 */

private val ButtonShape = RoundedCornerShape(CarDimens.ButtonRadius)

/** 파란 주 버튼 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = CarDimens.TouchTarget,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CarColors.Primary,
            contentColor = CarColors.OnAccent,
            disabledContainerColor = CarColors.SurfaceVariant,
            disabledContentColor = CarColors.Disabled,
        ),
        contentPadding = PaddingValues(horizontal = 36.dp),
        modifier = modifier.heightIn(min = height),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Box(Modifier.size(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 테두리만 있는 보조 버튼 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = CarDimens.TouchTarget,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(2.dp, CarColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarColors.OnSurface),
        contentPadding = PaddingValues(horizontal = 28.dp),
        modifier = modifier.heightIn(min = height),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 선택은 테두리·약한 배경으로 표시한다. 하나만 고르는 곳은 Role.RadioButton. */
@Composable
fun SelectableButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = CarDimens.TouchTarget,
    role: Role = Role.Checkbox,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(2.dp, if (selected) CarColors.Primary else CarColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) CarColors.SurfaceVariant else Color.Transparent,
            contentColor = CarColors.OnSurface,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
            .heightIn(min = height)
            .semantics { this.selected = selected; this.role = role },
        content = content,
    )
}

@Composable
fun SelectableButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Checkbox,
) {
    SelectableButton(selected, onClick, modifier, role = role) {
        Icon(CarIcons.Check, contentDescription = null, tint = if (selected) CarColors.Primary else Color.Transparent, modifier = Modifier.size(24.dp))
        Box(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 둥근 회색 카드 */
@Composable
fun CarCard(
    modifier: Modifier = Modifier,
    color: Color = CarColors.Surface,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(28.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = color,
        contentColor = CarColors.OnSurface,
        shape = RoundedCornerShape(CarDimens.CardRadius),
        border = border,
        modifier = modifier,
    ) {
        Column(Modifier.padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
    }
}

/** 라벨 + 큰 숫자. unit은 숫자보다 작게 붙는다 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueColor: Color = CarColors.OnSurface,
    valueMaxLines: Int = 1,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    caption: String? = null,
    color: Color = CarColors.Surface,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
) {
    CarCard(modifier, color = color, contentPadding = PaddingValues(20.dp), verticalArrangement = verticalArrangement) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = valueStyle, color = valueColor, maxLines = valueMaxLines)
            if (unit != null) {
                Text(
                    unit,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                )
            }
        }
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
    }
}

enum class PillKind { PRIMARY, WARNING, NEUTRAL, OUTLINED }

/** 상태 표시(인증·실패·해제율 등). 색만이 아니라 글자로 상태를 말한다 */
@Composable
fun StatusPill(text: String, kind: PillKind, modifier: Modifier = Modifier) {
    val (bg, fg) = when (kind) {
        PillKind.PRIMARY -> CarColors.Primary to CarColors.OnAccent
        PillKind.WARNING -> CarColors.Warning to CarColors.OnAccent
        PillKind.NEUTRAL -> CarColors.Track to CarColors.OnSurface
        PillKind.OUTLINED -> Color.Transparent to CarColors.OnSurfaceVariant
    }
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(999.dp),
        border = if (kind == PillKind.OUTLINED) BorderStroke(2.dp, CarColors.Outline) else null,
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
    }
}

/** 가로 막대. fraction 0~1 */
@Composable
fun MeterBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(CarColors.Track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(shape).background(color))
    }
}

/** 화면 제목줄. onBack이 있으면 왼쪽에 뒤로 버튼 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 840.dp
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(CarDimens.TouchTargetSmall)) {
                        Icon(CarIcons.ChevronLeft, contentDescription = "뒤로", modifier = Modifier.size(32.dp))
                    }
                    Box(Modifier.size(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
                }
                if (!compact && actions != null) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            if (compact && actions != null) Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/**
 * 동의 항목 카드. 앱 개발 규정: [동의] 버튼을 누르는 것만 동의로 본다(뒤로 가기·빈 곳 터치는 동의 아님).
 */
@Composable
fun ConsentRow(
    title: String,
    agreed: Boolean,
    onAgree: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    onShowTerms: (() -> Unit)? = null,
    agreeLabel: String = "동의",
    agreedLabel: String = "동의함",
) {
    CarCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val details: @Composable (Modifier) -> Unit = { textModifier ->
                Column(textModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (description != null) {
                        Text(description, style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
                    }
                    if (onShowTerms != null) {
                        TextButton(onClick = onShowTerms, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = CarDimens.TouchTargetSmall)) {
                            Text("내용 보기", style = MaterialTheme.typography.bodyMedium, color = CarColors.Primary)
                        }
                    }
                }
            }
            val action: @Composable () -> Unit = {
                if (agreed) {
                    Row(
                        Modifier.heightIn(min = CarDimens.TouchTargetSmall)
                            .clip(RoundedCornerShape(CarDimens.ButtonRadius))
                            .background(CarColors.SurfaceVariant)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(CarIcons.Check, contentDescription = null, tint = CarColors.Primary, modifier = Modifier.size(26.dp))
                        Text(agreedLabel, style = MaterialTheme.typography.bodyLarge, color = CarColors.Primary, maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = onAgree, shape = ButtonShape,
                        border = BorderStroke(2.dp, CarColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarColors.Primary),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        modifier = Modifier.heightIn(min = CarDimens.TouchTargetSmall),
                    ) {
                        Text(agreeLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
            // 좁은 열에서는 버튼을 아래로 옮겨 설명이 글자 중간에서 끊기지 않게 한다.
            if (maxWidth < 500.dp) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    details(Modifier.fillMaxWidth())
                    Box(Modifier.align(Alignment.End)) { action() }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    details(Modifier.weight(1f))
                    action()
                }
            }
        }
    }
}

/** 긴 비활성 사유는 버튼과 분리해, 시작 버튼은 항상 한 줄로 읽히게 한다. */
@Composable
fun OnboardingStartAction(enabled: Boolean, hint: String, onStart: () -> Unit, modifier: Modifier = Modifier, label: String = "시작하기") {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!enabled) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        PrimaryButton(label, onStart, Modifier.fillMaxWidth(), enabled = enabled)
    }
}

/* ---- 숫자 포맷: UiState에는 원래 값만 있고 포맷은 화면에서 한다 ---- */

fun formatKm(distanceM: Double?): String =
    distanceM?.let { String.format(Locale.KOREA, "%.1f km", it / 1000) } ?: "-"

fun formatPercent(ratio: Double): String = String.format(Locale.KOREA, "%.0f%%", ratio * 100)

fun formatOneDecimal(value: Double): String = String.format(Locale.KOREA, "%.1f", value)

fun formatWhole(value: Double?): String = value?.let { String.format(Locale.KOREA, "%.0f", it) } ?: "-"

/** 모달의 행동은 64dp 이상. 닫기·취소는 동의로 취급하지 않는다. */
@Composable
fun CarAlertDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
    onDismiss: () -> Unit, dismissLabel: String? = null, destructive: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarColors.Surface,
        titleContentColor = CarColors.OnSurface,
        textContentColor = CarColors.OnSurface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (dismissLabel != null) TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 64.dp)) {
                    Text(dismissLabel, style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurface)
                }
                TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 64.dp)) {
                    Text(confirmLabel, style = MaterialTheme.typography.labelLarge, color = if (destructive) CarColors.Warning else CarColors.Primary)
                }
            }
        },
    )
}

@Composable
fun EmptyState(title: String, message: String? = null, loading: Boolean = false, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(64.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, textAlign = TextAlign.Center)
            if (onRetry != null) PrimaryButton("다시 시도", onRetry)
        }
    }
}

@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, shape = ButtonShape, border = BorderStroke(2.dp, CarColors.Warning),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarColors.Warning),
        contentPadding = PaddingValues(horizontal = 24.dp), modifier = modifier.heightIn(min = 64.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
