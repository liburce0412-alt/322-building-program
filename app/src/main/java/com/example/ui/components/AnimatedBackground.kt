package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

// Pastel rainbow colors for light theme background
private val GradientColors = listOf(
    Color(0xFFFEF2F2),
    Color(0xFFFFF7ED),
    Color(0xFFFEFCE8),
    Color(0xFFF0FDF4),
    Color(0xFFEFF6FF),
    Color(0xFFF5F3FF),
    Color(0xFFFDF2F8)
)
private val SparkleColors = listOf(
    Color(0xFFEA4335),  // Google Red
    Color(0xFFFBBC05),  // Google Yellow
    Color(0xFF34A853),  // Google Green
    Color(0xFF4285F4),  // Google Blue
    Color(0xFF8B5CF6),  // Purple
    Color(0xFFEC4899),  // Pink
)

private data class Sparkle(
    val id: Int,
    var x: Float,
    var y: Float,
    val color: Color,
    val scale: Float,
    val delay: Float,
    var lifespan: Float
)

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val radiusFactor by infiniteTransition.animateFloat(
        initialValue = 1.20f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius"
    )

    var sparkles by remember { mutableStateOf(listOf<Sparkle>()) }
    var nextId by remember { mutableStateOf(0) }
    var sparkleInitDone by remember { mutableStateOf(false) }
    
    if (!sparkleInitDone) {
        LaunchedEffect(Unit) {
            val initial = List(30) {
                val angle = Random.nextFloat() * 360f
                val dist = Random.nextFloat() * 0.8f
                Sparkle(
                    id = nextId++,
                    x = 0.5f + kotlin.math.cos(angle) * dist * 0.5f,
                    y = 0.2f + kotlin.math.sin(angle) * dist * 0.4f,
                    color = SparkleColors[Random.nextInt(SparkleColors.size)],
                    scale = Random.nextFloat() * 1.0f + 0.3f,
                    delay = Random.nextFloat() * 2f,
                    lifespan = Random.nextFloat() * 10f + 5f
                )
            }
            sparkles = initial
            sparkleInitDone = true
        }
    }

    LaunchedEffect(sparkleInitDone) {
        if (!sparkleInitDone) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(100)
            sparkles = sparkles.map { s ->
                if (s.lifespan <= 0) {
                    val angle = Random.nextFloat() * 360f
                    val dist = Random.nextFloat() * 0.8f
                    Sparkle(
                        id = nextId++,
                        x = 0.5f + kotlin.math.cos(angle) * dist * 0.5f,
                        y = 0.2f + kotlin.math.sin(angle) * dist * 0.4f,
                        color = SparkleColors[Random.nextInt(SparkleColors.size)],
                        scale = Random.nextFloat() * 1.0f + 0.3f,
                        delay = Random.nextFloat() * 2f,
                        lifespan = Random.nextFloat() * 10f + 5f
                    )
                } else {
                    s.copy(lifespan = s.lifespan - 0.1f).also {
                        it.x = s.x
                        it.y = s.y
                    }
                }
            }.sortedBy { it.delay }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.2f)
        val radius = w * radiusFactor

    val gradientBrush = Brush.radialGradient(
        colorStops = arrayOf(
            0.40f to Color(0xFFFFFFFF),
            0.55f to Color(0xFFFEF2F2),
            0.65f to Color(0xFFFFF7ED),
            0.75f to Color(0xFFFEFCE8),
            0.82f to Color(0xFFEFF6FF),
            0.90f to Color(0xFFF5F3FF),
            1.00f to Color(0xFFFDF2F8)
        ),
        center = center,
        radius = radius
    )
        drawRect(brush = gradientBrush, size = size)

        val timeMs = (System.currentTimeMillis() % 3000) / 1000f
        sparkles.forEach { s ->
            val t = ((timeMs + s.delay) % 3.0f) / 3.0f
            val alpha = when {
                t < 0.1f -> t / 0.1f
                t > 0.85f -> (1f - (t - 0.85f) / 0.15f)
                else -> 1f
            }.coerceIn(0f, 1f)

            if (alpha > 0.01f) {
                val sparkleSize = 12f * s.scale
                val px = s.x * w
                val py = s.y * h

                drawSparkle(
                    color = s.color.copy(alpha = alpha),
                    size = sparkleSize,
                    center = Offset(px, py),
                    rotation = t * 360f
                )
            }
        }
    }
}

private fun DrawScope.drawSparkle(
    color: Color,
    size: Float,
    center: Offset,
    rotation: Float
) {
    withTransform({
        translate(center.x, center.y)
        rotate(rotation)
        scale(size / 20f, size / 20f)
    }) {
        val path = Path().apply {
            moveTo(10f, 0f)
            cubicTo(10.5f, 5f, 15f, 9.5f, 20f, 10f)
            cubicTo(15f, 10.5f, 10.5f, 15f, 10f, 20f)
            cubicTo(9.5f, 15f, 5f, 10.5f, 0f, 10f)
            cubicTo(5f, 9.5f, 9.5f, 5f, 10f, 0f)
            close()
        }
        drawPath(path, color)
    }
}
