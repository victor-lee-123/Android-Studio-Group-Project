package sg.edu.sit.attendance.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
//  Shimmer brush helper
// ─────────────────────────────────────────────
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFFE8EDF5),
        Color(0xFFF5F7FC),
        Color(0xFFE8EDF5),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1200f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start  = Offset(translateAnim - 400f, 0f),
        end    = Offset(translateAnim, 0f)
    )
}

// ─────────────────────────────────────────────
//  Session card skeleton
// ─────────────────────────────────────────────
@Composable
fun SessionCardSkeleton() {
    val brush = shimmerBrush()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Column(Modifier.padding(20.dp)) {

            // Title row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    // Title placeholder
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Subtitle placeholder
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                }
                // Badge placeholder
                Box(
                    Modifier
                        .width(56.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Time box placeholder
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush)
            )

            Spacer(Modifier.height(16.dp))

            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5EAF2)))
            Spacer(Modifier.height(16.dp))

            // Section label placeholder
            Box(
                Modifier
                    .width(100.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(14.dp))

            // Step rows placeholders
            repeat(3) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    // Circle
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(brush)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(13.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.55f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
                if (it < 2) Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(22.dp))

            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5EAF2)))
            Spacer(Modifier.height(18.dp))

            // Actions label
            Box(
                Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(12.dp))

            // Two buttons placeholder
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Check In button placeholder
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}
