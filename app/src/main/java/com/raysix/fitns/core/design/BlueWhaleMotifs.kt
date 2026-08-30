package com.raysix.fitns.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** A quiet ocean layer used behind high-level screens without affecting layout. */
@Composable
fun OceanBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val wash = MaterialTheme.colorScheme.primaryContainer
    val line = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        wash.copy(alpha = 0.32f),
                        background,
                        background
                    )
                )
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val firstWave = Path().apply {
                moveTo(-size.width * 0.08f, size.height * 0.12f)
                cubicTo(
                    size.width * 0.18f,
                    size.height * 0.04f,
                    size.width * 0.34f,
                    size.height * 0.20f,
                    size.width * 0.58f,
                    size.height * 0.11f
                )
                cubicTo(
                    size.width * 0.76f,
                    size.height * 0.04f,
                    size.width * 0.9f,
                    size.height * 0.12f,
                    size.width * 1.08f,
                    size.height * 0.08f
                )
            }
            drawPath(
                path = firstWave,
                color = line.copy(alpha = 0.09f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            val lowerWave = Path().apply {
                moveTo(-size.width * 0.08f, size.height * 0.82f)
                cubicTo(
                    size.width * 0.22f,
                    size.height * 0.72f,
                    size.width * 0.38f,
                    size.height * 0.91f,
                    size.width * 0.64f,
                    size.height * 0.81f
                )
                cubicTo(
                    size.width * 0.82f,
                    size.height * 0.74f,
                    size.width * 0.94f,
                    size.height * 0.82f,
                    size.width * 1.08f,
                    size.height * 0.78f
                )
            }
            drawPath(
                path = lowerWave,
                color = line.copy(alpha = 0.06f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = line.copy(alpha = 0.07f),
                radius = size.minDimension * 0.012f,
                center = Offset(size.width * 0.9f, size.height * 0.2f)
            )
            drawCircle(
                color = line.copy(alpha = 0.05f),
                radius = size.minDimension * 0.007f,
                center = Offset(size.width * 0.85f, size.height * 0.24f)
            )
        }
        content()
    }
}

/** Decorative whale-tail mark. It intentionally has no accessibility semantics. */
@Composable
fun WhaleTailMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier) {
        val tail = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.67f)
            cubicTo(
                size.width * 0.43f,
                size.height * 0.51f,
                size.width * 0.25f,
                size.height * 0.2f,
                size.width * 0.06f,
                size.height * 0.27f
            )
            cubicTo(
                size.width * 0.06f,
                size.height * 0.5f,
                size.width * 0.22f,
                size.height * 0.7f,
                size.width * 0.46f,
                size.height * 0.66f
            )
            cubicTo(
                size.width * 0.48f,
                size.height * 0.73f,
                size.width * 0.48f,
                size.height * 0.83f,
                size.width * 0.5f,
                size.height * 0.92f
            )
            cubicTo(
                size.width * 0.52f,
                size.height * 0.83f,
                size.width * 0.52f,
                size.height * 0.73f,
                size.width * 0.54f,
                size.height * 0.66f
            )
            cubicTo(
                size.width * 0.78f,
                size.height * 0.7f,
                size.width * 0.94f,
                size.height * 0.5f,
                size.width * 0.94f,
                size.height * 0.27f
            )
            cubicTo(
                size.width * 0.75f,
                size.height * 0.2f,
                size.width * 0.57f,
                size.height * 0.51f,
                size.width * 0.5f,
                size.height * 0.67f
            )
            close()
        }
        drawPath(tail, tint)
    }
}

/** Subtle white waterline for gradient cards. */
@Composable
internal fun OceanWaveOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val wave = Path().apply {
            moveTo(-size.width * 0.1f, size.height * 0.72f)
            cubicTo(
                size.width * 0.16f,
                size.height * 0.48f,
                size.width * 0.34f,
                size.height * 0.9f,
                size.width * 0.58f,
                size.height * 0.66f
            )
            cubicTo(
                size.width * 0.76f,
                size.height * 0.48f,
                size.width * 0.91f,
                size.height * 0.72f,
                size.width * 1.1f,
                size.height * 0.56f
            )
        }
        drawPath(
            path = wave,
            color = color.copy(alpha = 0.13f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
    }
}
