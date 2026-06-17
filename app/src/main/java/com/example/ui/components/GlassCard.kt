package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteLight
import com.example.ui.theme.RainbowPastel

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp),
    borderColors: List<Color> = RainbowPastel,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 2.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(
                colors = listOf(GlassCardSurface, GlassWhiteLight)
            ))
            .border(width = borderWidth, brush = Brush.linearGradient(colors = borderColors), shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassIconCircle(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    emoji: String
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .clip(CircleShape)
            .background(Brush.linearGradient(
                colors = listOf(GlassWhite, GlassWhiteLight)
            ))
            .border(width = 1.dp, brush = Brush.linearGradient(colors = RainbowPastel), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}

