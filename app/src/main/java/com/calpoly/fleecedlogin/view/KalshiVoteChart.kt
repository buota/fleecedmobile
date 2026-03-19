package com.calpoly.fleecedlogin.view

import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.viewmodel.VoteHistoryPoint
import java.text.SimpleDateFormat
import java.util.*

private val KChartBlue    = Color(0xFF5B7FD4)
private val KChartOrange  = Color(0xFFFF9E64)
private val KChartBgDeep  = Color(0xFF0D1B2A)

/**
 * Kalshi-style poll vote trend chart.
 *
 * X-axis = time  (domainStartMs → domainEndMs)
 * Y-axis = option 1 vote percentage  (0 % → 100 %)
 *
 * Option 2 is implicitly 100 - option1Pct (complementary).
 */
@Composable
fun KalshiVoteChart(
    points: List<VoteHistoryPoint>,
    option1Label: String,
    option2Label: String,
    domainStartMs: Long,
    domainEndMs: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // ── draw-in animation ─────────────────────────────────────────────
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        drawProgress.snapTo(0f)
        if (points.size >= 2) {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
    }

    // ── shimmer for loading state ─────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    // ── derived ───────────────────────────────────────────────────────
    val lastPct1   = if (points.isNotEmpty()) points.last().option1Pct.toInt().coerceIn(0, 100) else 50
    val lastPct2   = 100 - lastPct1
    val domainMs   = (domainEndMs - domainStartMs).coerceAtLeast(60_000L).toFloat()
    val midMs      = domainStartMs + (domainEndMs - domainStartMs) / 2L
    val pollActive = System.currentTimeMillis() < domainEndMs

    Column(modifier = modifier) {

        // ── legend header ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option1Label,
                    style = MaterialTheme.typography.labelSmall,
                    color = KChartBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$lastPct1%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = option2Label,
                    style = MaterialTheme.typography.labelSmall,
                    color = KChartOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$lastPct2%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── chart canvas ──────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val yAxisW = 34.dp.toPx()
            val xAxisH = 18.dp.toPx()
            val padTop = 6.dp.toPx()

            val chartL = yAxisW
            val chartT = padTop
            val chartR = size.width
            val chartB = size.height - xAxisH
            val chartW = (chartR - chartL).coerceAtLeast(1f)
            val chartH = (chartB - chartT).coerceAtLeast(1f)

            fun xFor(ts: Long)   = chartL + ((ts - domainStartMs).toFloat() / domainMs).coerceIn(0f, 1f) * chartW
            fun yFor(pct: Float) = chartB - (pct / 100f) * chartH

            // ── guide lines + Y-axis labels ───────────────────────────
            val labelPaint = NativePaint().apply {
                isAntiAlias = true
                textSize    = 9.sp.toPx()
                textAlign   = NativePaint.Align.RIGHT
                color       = android.graphics.Color.argb(140, 160, 176, 200)
            }

            listOf(0f, 25f, 50f, 75f, 100f).forEach { pct ->
                val y     = yFor(pct)
                val isMid = pct == 50f

                drawLine(
                    color       = if (isMid) Color.White.copy(alpha = 0.16f)
                                  else       Color.White.copy(alpha = 0.06f),
                    start       = Offset(chartL, y),
                    end         = Offset(chartR, y),
                    strokeWidth = if (isMid) 1.5f else 0.8f,
                    pathEffect  = if (isMid)
                        PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    else
                        PathEffect.dashPathEffect(floatArrayOf(4f, 8f))
                )

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%",
                        chartL - 4.dp.toPx(),
                        y + labelPaint.textSize / 3f,
                        labelPaint
                    )
                }
            }

            // ── vertical axis border ───────────────────────────────────
            drawLine(
                color       = Color.White.copy(alpha = 0.08f),
                start       = Offset(chartL, chartT),
                end         = Offset(chartL, chartB),
                strokeWidth = 1f
            )

            // ── loading shimmer ────────────────────────────────────────
            if (isLoading) {
                // faint baseline at 50 %
                drawLine(
                    color       = Color(0xFF2A3A55),
                    start       = Offset(chartL, yFor(50f)),
                    end         = Offset(chartR, yFor(50f)),
                    strokeWidth = 2f
                )
                // moving highlight strip
                val shimX = chartL + shimmerPhase * chartW
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startX = shimX - 60f,
                        endX   = shimX + 60f
                    ),
                    topLeft = Offset(chartL, chartT),
                    size    = Size(chartW, chartH)
                )
            }

            // ── chart lines (animated clip left → right) ───────────────
            if (!isLoading && points.size >= 2) {
                val prog = drawProgress.value

                clipRect(
                    left   = chartL,
                    top    = chartT - 10f,       // slight over-clip so dots draw flush
                    right  = chartL + prog * chartW,
                    bottom = chartB + 2f
                ) {
                    // ── gradient fill under option 1 ──────────────────
                    val linePath1 = buildSmoothPath(points) { p ->
                        Offset(xFor(p.timestampMs), yFor(p.option1Pct))
                    }
                    val fillPath = Path().apply {
                        addPath(linePath1)
                        lineTo(xFor(points.last().timestampMs),  chartB)
                        lineTo(xFor(points.first().timestampMs), chartB)
                        close()
                    }
                    drawPath(
                        path  = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                KChartBlue.copy(alpha = 0.30f),
                                KChartBlue.copy(alpha = 0.00f)
                            ),
                            startY = chartT,
                            endY   = chartB
                        )
                    )

                    // ── option 1 line (blue, heavier) ─────────────────
                    drawPath(
                        path  = linePath1,
                        color = KChartBlue,
                        style = Stroke(
                            width = 2.5f,
                            cap   = StrokeCap.Round,
                            join  = StrokeJoin.Round
                        )
                    )

                    // ── option 2 line (orange, lighter) + fill ────────
                    val linePath2 = buildSmoothPath(points) { p ->
                        Offset(xFor(p.timestampMs), yFor(100f - p.option1Pct))
                    }
                    val fillPath2 = Path().apply {
                        addPath(linePath2)
                        lineTo(xFor(points.last().timestampMs),  chartB)
                        lineTo(xFor(points.first().timestampMs), chartB)
                        close()
                    }
                    drawPath(
                        path  = fillPath2,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                KChartOrange.copy(alpha = 0.30f),
                                KChartOrange.copy(alpha = 0.00f)
                            ),
                            startY = chartT,
                            endY   = chartB
                        )
                    )
                    drawPath(
                        path  = linePath2,
                        color = KChartOrange,
                        style = Stroke(
                            width = 1.8f,
                            cap   = StrokeCap.Round,
                            join  = StrokeJoin.Round
                        )
                    )
                }

                // ── endpoint dots (appear once animation finishes) ─────
                if (prog >= 0.99f) {
                    val endX  = xFor(points.last().timestampMs)
                    val endY1 = yFor(points.last().option1Pct)
                    val endY2 = yFor(100f - points.last().option1Pct)

                    drawCircle(KChartBgDeep,  radius = 7f, center = Offset(endX, endY1))
                    drawCircle(KChartBlue,    radius = 5f, center = Offset(endX, endY1))
                    drawCircle(KChartBgDeep,  radius = 7f, center = Offset(endX, endY2))
                    drawCircle(KChartOrange,  radius = 5f, center = Offset(endX, endY2))
                }
            }

            // ── no-data message ────────────────────────────────────────
            if (!isLoading && points.size < 2) {
                drawIntoCanvas { canvas ->
                    val msgPaint = NativePaint().apply {
                        isAntiAlias = true
                        textSize    = 12.sp.toPx()
                        textAlign   = NativePaint.Align.CENTER
                        color       = android.graphics.Color.argb(100, 160, 176, 200)
                    }
                    canvas.nativeCanvas.drawText(
                        "No vote data yet",
                        chartL + chartW / 2f,
                        chartT + chartH / 2f + msgPaint.textSize / 3f,
                        msgPaint
                    )
                }
            }

            // ── X-axis time labels ─────────────────────────────────────
            val xPaint = NativePaint().apply {
                isAntiAlias = true
                textSize    = 9.sp.toPx()
                color       = android.graphics.Color.argb(130, 160, 176, 200)
            }
            val liveColor = android.graphics.Color.argb(
                200,
                (VoteGreen.red   * 255).toInt(),
                (VoteGreen.green * 255).toInt(),
                (VoteGreen.blue  * 255).toInt()
            )
            val durationMs = domainEndMs - domainStartMs

            drawIntoCanvas { canvas ->
                // start label
                xPaint.textAlign = NativePaint.Align.LEFT
                canvas.nativeCanvas.drawText(
                    formatXLabel(domainStartMs, durationMs),
                    chartL,
                    size.height - 2.dp.toPx(),
                    xPaint
                )
                // mid label
                xPaint.textAlign = NativePaint.Align.CENTER
                canvas.nativeCanvas.drawText(
                    formatXLabel(midMs, durationMs),
                    chartL + chartW / 2f,
                    size.height - 2.dp.toPx(),
                    xPaint
                )
                // end label — green "LIVE" when poll is still active
                xPaint.textAlign = NativePaint.Align.RIGHT
                if (pollActive) xPaint.color = liveColor
                canvas.nativeCanvas.drawText(
                    if (pollActive) "LIVE" else formatXLabel(domainEndMs, durationMs),
                    chartR,
                    size.height - 2.dp.toPx(),
                    xPaint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Builds a smooth cubic-bezier path through [points] using Catmull-Rom → Bezier conversion.
 * [toOffset] maps each data point to canvas coordinates.
 */
private fun buildSmoothPath(
    points: List<VoteHistoryPoint>,
    toOffset: (VoteHistoryPoint) -> Offset
): Path {
    val offsets = points.map(toOffset)
    val path    = Path()
    if (offsets.isEmpty()) return path

    path.moveTo(offsets[0].x, offsets[0].y)
    if (offsets.size == 1) return path

    val tension = 0.4f   // 0 = linear, higher = more overshoot
    for (i in 0 until offsets.size - 1) {
        val p0 = offsets.getOrElse(i - 1) { offsets[i] }
        val p1 = offsets[i]
        val p2 = offsets[i + 1]
        val p3 = offsets.getOrElse(i + 2) { offsets[i + 1] }

        val cp1x = p1.x + (p2.x - p0.x) * tension / 2f
        val cp1y = p1.y + (p2.y - p0.y) * tension / 2f
        val cp2x = p2.x - (p3.x - p1.x) * tension / 2f
        val cp2y = p2.y - (p3.y - p1.y) * tension / 2f

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    return path
}

/**
 * Formats a timestamp for the X-axis, scaled to the poll's total duration.
 */
private fun formatXLabel(timestampMs: Long, durationMs: Long): String {
    val fmt = when {
        durationMs <= 3_600_000L   -> SimpleDateFormat("h:mm a", Locale.US)
        durationMs <= 172_800_000L -> SimpleDateFormat("EEE ha", Locale.US)
        else                       -> SimpleDateFormat("MMM d",  Locale.US)
    }
    return fmt.format(Date(timestampMs))
}
