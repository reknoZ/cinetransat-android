package com.heewhack.cinetransat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heewhack.cinetransat.data.FestivalZone
import java.time.LocalDate

/**
 * Tab bar calendar glyph with today's day of month (Geneva), tinted like other tab icons.
 * Matches iOS [TodayTabBarIcon].
 */
@Composable
fun CalendarDayIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    day: Int = LocalDate.now(FestivalZone).dayOfMonth,
    tint: Color = LocalContentColor.current,
) {
    val clampedDay = day.coerceIn(1, 31)
    val dayText = clampedDay.toString()
    val fontSize = if (clampedDay >= 10) 8.sp else 9.sp

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 1.2.dp.toPx()
            val rect =
                Rect(
                    left = inset,
                    top = inset,
                    right = this.size.width - inset,
                    bottom = this.size.height - inset,
                )
            val cornerRadius = 3.2.dp.toPx()
            val headerHeight = rect.height * 0.28f
            val headerRect =
                Rect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.top + headerHeight,
                )

            drawRoundRect(
                color = tint,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.4.dp.toPx()),
            )
            drawRoundRect(
                color = tint,
                topLeft = headerRect.topLeft,
                size = headerRect.size,
                cornerRadius = CornerRadius(2.4.dp.toPx(), 2.4.dp.toPx()),
            )

            val ringRadius = 1.15.dp.toPx()
            val ringsY = rect.top - 1.6.dp.toPx()
            listOf(rect.left + rect.width * 0.28f, rect.left + rect.width * 0.72f).forEach { x ->
                drawCircle(
                    color = tint,
                    radius = ringRadius,
                    center = Offset(x, ringsY),
                )
            }
        }
        Text(
            text = dayText,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.5.dp),
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
