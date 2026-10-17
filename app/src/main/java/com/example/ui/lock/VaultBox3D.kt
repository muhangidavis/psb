package com.example.ui.lock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun VaultBox3D(
    modifier: Modifier = Modifier,
    isPadlockOpen: Boolean = false,
    isChainUnwound: Boolean = false
) {
    // Floating bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "vaultFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Shackle opening rotation (0deg to -35deg)
    val shackleAngle by animateFloatAsState(
        targetValue = if (isPadlockOpen) -36f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "shackle"
    )

    // Chain drop translation and fade
    val chainOffsetY by animateFloatAsState(
        targetValue = if (isChainUnwound) 350f else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "chainDrop"
    )
    val chainAlpha by animateFloatAsState(
        targetValue = if (isChainUnwound) 0f else 1f,
        animationSpec = tween(700),
        label = "chainAlpha"
    )

    Box(
        modifier = modifier
            .width(260.dp)
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = (size.height / 2f) + floatOffset

            // 1. Draw glowing floor shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44FF8F2E), Color(0x11FF8F2E), Color.Transparent),
                    center = Offset(cx, size.height - 24f),
                    radius = 90f
                ),
                topLeft = Offset(cx - 90f, size.height - 38f),
                size = Size(180f, 28f)
            )

            // 2. Draw 3D Isometric Safe Cuboid
            val safeWidth = 140f
            val safeHeight = 110f
            val depthX = 48f
            val depthY = 28f

            val frontLeft = cx - (safeWidth / 2f)
            val frontTop = cy - (safeHeight / 2f) + 10f

            // Top Face
            val topPath = Path().apply {
                moveTo(frontLeft, frontTop)
                lineTo(frontLeft + depthX, frontTop - depthY)
                lineTo(frontLeft + safeWidth + depthX, frontTop - depthY)
                lineTo(frontLeft + safeWidth, frontTop)
                close()
            }
            drawPath(
                path = topPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFC07A), Color(0xFFFF9436)),
                    startY = frontTop - depthY,
                    endY = frontTop
                )
            )
            drawPath(
                path = topPath,
                color = Color(0x663C1900),
                style = Stroke(width = 1.5f)
            )

            // Right Face
            val rightPath = Path().apply {
                moveTo(frontLeft + safeWidth, frontTop)
                lineTo(frontLeft + safeWidth + depthX, frontTop - depthY)
                lineTo(frontLeft + safeWidth + depthX, frontTop + safeHeight - depthY)
                lineTo(frontLeft + safeWidth, frontTop + safeHeight)
                close()
            }
            drawPath(
                path = rightPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFC95400), Color(0xFF9C3D00)),
                    startY = frontTop - depthY,
                    endY = frontTop + safeHeight
                )
            )
            drawPath(
                path = rightPath,
                color = Color(0x663C1900),
                style = Stroke(width = 1.5f)
            )

            // Front Face
            val frontRect = RectF(frontLeft, frontTop, safeWidth, safeHeight)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF9A3C), Color(0xFFF2740F), Color(0xFFD95C00)),
                    start = Offset(frontLeft, frontTop),
                    end = Offset(frontLeft + safeWidth, frontTop + safeHeight)
                ),
                topLeft = Offset(frontLeft, frontTop),
                size = Size(safeWidth, safeHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0x55000000),
                topLeft = Offset(frontLeft, frontTop),
                size = Size(safeWidth, safeHeight),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2f)
            )

            // Safe Door Inset Panel
            val insetMargin = 12f
            drawRoundRect(
                color = Color(0x2A000000),
                topLeft = Offset(frontLeft + insetMargin, frontTop + insetMargin),
                size = Size(safeWidth - (insetMargin * 2), safeHeight - (insetMargin * 2)),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )

            // Hinges on left
            drawRoundRect(
                color = Color(0xFF8C3600),
                topLeft = Offset(frontLeft + 4f, frontTop + 20f),
                size = Size(6f, 16f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = Color(0xFF8C3600),
                topLeft = Offset(frontLeft + 4f, frontTop + safeHeight - 36f),
                size = Size(6f, 16f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 3. Chains and Padlock
            if (chainAlpha > 0.02f) {
                drawChainsAndPadlock(
                    cx = cx,
                    cy = cy,
                    frontLeft = frontLeft,
                    frontTop = frontTop,
                    safeWidth = safeWidth,
                    safeHeight = safeHeight,
                    depthX = depthX,
                    depthY = depthY,
                    chainOffsetY = chainOffsetY,
                    chainAlpha = chainAlpha,
                    shackleAngle = shackleAngle
                )
            }
        }
    }
}

private class RectF(val left: Float, val top: Float, val width: Float, val height: Float)

private fun DrawScope.drawChainsAndPadlock(
    cx: Float,
    cy: Float,
    frontLeft: Float,
    frontTop: Float,
    safeWidth: Float,
    safeHeight: Float,
    depthX: Float,
    depthY: Float,
    chainOffsetY: Float,
    chainAlpha: Float,
    shackleAngle: Float
) {
    val chainYCenter = frontTop + (safeHeight / 2f) + chainOffsetY
    val chainXCenter = frontLeft + (safeWidth / 2f)

    // Horizontal chain strap across the front
    val strapColor = Color(0xFFD5DCE4).copy(alpha = chainAlpha)
    val linkInner = Color(0xFF8D99A6).copy(alpha = chainAlpha)
    val chainShadow = Color(0x66000000).copy(alpha = chainAlpha)

    // Draw horizontal chain links
    val numHLinks = 9
    val linkSpacing = safeWidth / numHLinks
    for (i in 0 until numHLinks) {
        val lx = frontLeft + (i * linkSpacing) + (linkSpacing / 2f)
        val ly = chainYCenter
        val isCross = i % 2 == 1
        val linkW = if (isCross) 10f else 18f
        val linkH = if (isCross) 18f else 10f

        // shadow
        drawOval(
            color = chainShadow,
            topLeft = Offset(lx - (linkW / 2f), ly - (linkH / 2f) + 3f),
            size = Size(linkW, linkH)
        )
        // outer steel
        drawOval(
            color = strapColor,
            topLeft = Offset(lx - (linkW / 2f), ly - (linkH / 2f)),
            size = Size(linkW, linkH)
        )
        // inner hole
        drawOval(
            color = linkInner,
            topLeft = Offset(lx - (linkW / 4f), ly - (linkH / 4f)),
            size = Size(linkW / 2f, linkH / 2f)
        )
    }

    // Vertical chain links down front center
    val numVLinks = 7
    val vSpacing = safeHeight / numVLinks
    for (i in 0 until numVLinks) {
        val lx = chainXCenter
        val ly = frontTop + (i * vSpacing) + (vSpacing / 2f) + chainOffsetY
        val isCross = i % 2 == 0
        val linkW = if (isCross) 18f else 10f
        val linkH = if (isCross) 10f else 18f

        drawOval(
            color = chainShadow,
            topLeft = Offset(lx - (linkW / 2f), ly - (linkH / 2f) + 3f),
            size = Size(linkW, linkH)
        )
        drawOval(
            color = strapColor,
            topLeft = Offset(lx - (linkW / 2f), ly - (linkH / 2f)),
            size = Size(linkW, linkH)
        )
        drawOval(
            color = linkInner,
            topLeft = Offset(lx - (linkW / 4f), ly - (linkH / 4f)),
            size = Size(linkW / 2f, linkH / 2f)
        )
    }

    // 4. Padlock
    val lockW = 44f
    val lockH = 34f
    val lockX = chainXCenter - (lockW / 2f)
    val lockY = chainYCenter - (lockH / 2f) + 6f

    // Shackle
    val shackleRadius = 14f
    val shackleStroke = 5f
    val shackleCenter = Offset(chainXCenter, lockY - 2f)

    rotate(degrees = shackleAngle, pivot = Offset(chainXCenter + 12f, lockY)) {
        drawArc(
            color = Color(0xFFE2E8F0).copy(alpha = chainAlpha),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(shackleCenter.x - shackleRadius, shackleCenter.y - shackleRadius),
            size = Size(shackleRadius * 2f, shackleRadius * 2f),
            style = Stroke(width = shackleStroke, cap = StrokeCap.Round)
        )
        // Vertical legs
        drawLine(
            color = Color(0xFFE2E8F0).copy(alpha = chainAlpha),
            start = Offset(shackleCenter.x - shackleRadius, shackleCenter.y),
            end = Offset(shackleCenter.x - shackleRadius, lockY + 2f),
            strokeWidth = shackleStroke
        )
        drawLine(
            color = Color(0xFFE2E8F0).copy(alpha = chainAlpha),
            start = Offset(shackleCenter.x + shackleRadius, shackleCenter.y),
            end = Offset(shackleCenter.x + shackleRadius, lockY + 2f),
            strokeWidth = shackleStroke
        )
    }

    // Padlock body shadow
    drawRoundRect(
        color = Color(0x88000000).copy(alpha = chainAlpha),
        topLeft = Offset(lockX, lockY + 4f),
        size = Size(lockW, lockH),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Padlock body with metallic chrome gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F4F9).copy(alpha = chainAlpha),
                Color(0xFFB9C3CE).copy(alpha = chainAlpha),
                Color(0xFF8D99A6).copy(alpha = chainAlpha)
            ),
            startY = lockY,
            endY = lockY + lockH
        ),
        topLeft = Offset(lockX, lockY),
        size = Size(lockW, lockH),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Padlock keyhole
    val keyholeY = lockY + (lockH / 2f)
    drawCircle(
        color = Color(0xFF1E222A).copy(alpha = chainAlpha),
        radius = 4.5f,
        center = Offset(chainXCenter, keyholeY - 2f)
    )
    val khPath = Path().apply {
        moveTo(chainXCenter - 2.5f, keyholeY - 2f)
        lineTo(chainXCenter + 2.5f, keyholeY - 2f)
        lineTo(chainXCenter + 3.5f, keyholeY + 7f)
        lineTo(chainXCenter - 3.5f, keyholeY + 7f)
        close()
    }
    drawPath(path = khPath, color = Color(0xFF1E222A).copy(alpha = chainAlpha))
}
