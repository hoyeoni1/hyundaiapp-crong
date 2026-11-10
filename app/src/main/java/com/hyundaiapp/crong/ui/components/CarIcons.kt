package com.hyundaiapp.crong.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * 두 앱 공통 아이콘(24 그리드, 선 아이콘). material-icons 의존성 없이 쓰려고 직접 그린다.
 * 색은 Icon(tint = ...)으로 바꾼다. 차슐랭 CarIcons.kt와 같은 내용.
 */
object CarIcons {
    val Warning = stroke("Warning", "M12 3 2 20h20L12 3z", "M12 10v4", "M12 17.5h.01")
    val Check = stroke("Check", "M5 12l5 5 9-10")
    val ChevronLeft = stroke("ChevronLeft", "M15 6l-6 6 6 6")
    val ChevronRight = stroke("ChevronRight", "M9 6l6 6-6 6")
    val Settings = stroke(
        "Settings",
        "M4 7h10", "M18 7h2", "M16 5v4",
        "M4 17h4", "M12 17h8", "M10 15v4",
    )
    val Bolt = stroke("Bolt", "M13 2 4 14h7l-1 8 9-12h-7l1-8z")
    val BoltOff = stroke("BoltOff", "M13 2 4 14h7l-1 8 9-12h-7l1-8z", "M3 3l18 18")
    val ShieldCheck = stroke("ShieldCheck", "M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6l-8-3z", "M8.5 12l2.5 2.5 4.5-5")
    val Navigate = stroke("Navigate", "M3 11 21 3l-8 18-2-8-8-2z")
    val Star = fill("Star", "M12 2.5l2.9 6.1 6.6.8-4.9 4.6 1.3 6.6L12 17.3l-5.9 3.3 1.3-6.6L2.5 9.4l6.6-.8z")
    val Chart = stroke("Chart", "M5 20V11", "M12 20V5", "M19 20v-6")
    val History = stroke("History", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 7v5l3 2")
    val Dot = fill("Dot", "M12 5a7 7 0 1 0 0 14a7 7 0 1 0 0-14z")

    private fun stroke(name: String, vararg paths: String): ImageVector =
        builder(name).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun fill(name: String, path: String): ImageVector =
        builder(name).apply { addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)) }.build()

    private fun builder(name: String) = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    )
}
