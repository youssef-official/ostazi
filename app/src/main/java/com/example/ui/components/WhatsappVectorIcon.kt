package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val WhatsappVectorIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Whatsapp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Draw speech bubble
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF25D366))
        ) {
            moveTo(12.01f, 2.0f)
            curveTo(6.49f, 2.00f, 2.0f, 6.49f, 2.0f, 12.01f)
            curveTo(2.0f, 13.91f, 2.53f, 15.69f, 3.46f, 17.22f)
            lineTo(2.05f, 22.0f)
            lineTo(6.98f, 20.62f)
            curveTo(8.46f, 21.51f, 10.18f, 22.02f, 12.01f, 22.02f)
            curveTo(17.53f, 22.02f, 22.02f, 17.53f, 22.02f, 12.01f)
            curveTo(22.02f, 6.49f, 17.53f, 2.0f, 12.01f, 2.0f)
            close()
        }
        // Draw inner phone receiver (white)
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White)
        ) {
            moveTo(16.5f, 14.12f)
            curveTo(16.25f, 14.0f, 15.0f, 13.38f, 14.75f, 13.25f)
            curveTo(14.5f, 13.12f, 14.38f, 13.12f, 14.25f, 13.25f)
            curveTo(14.0f, 13.62f, 13.38f, 14.38f, 13.25f, 14.5f)
            curveTo(13.12f, 14.62f, 13.0f, 14.62f, 12.75f, 14.5f)
            curveTo(12.0f, 14.12f, 11.0f, 13.5f, 10.25f, 12.75f)
            curveTo(9.5f, 12.0f, 8.88f, 11.0f, 8.5f, 10.25f)
            curveTo(8.38f, 10.0f, 8.5f, 9.88f, 8.62f, 9.75f)
            curveTo(8.75f, 9.62f, 8.88f, 9.38f, 9.0f, 9.25f)
            curveTo(9.12f, 9.12f, 9.12f, 9.0f, 9.25f, 8.88f)
            curveTo(9.38f, 8.75f, 9.25f, 8.62f, 9.12f, 8.5f)
            curveTo(9.0f, 8.38f, 7.88f, 5.62f, 7.75f, 5.38f)
            curveTo(7.5f, 4.88f, 7.38f, 4.88f, 7.25f, 4.88f)
            curveTo(7.12f, 4.88f, 7.0f, 4.88f, 6.88f, 4.88f)
            curveTo(6.5f, 4.88f, 6.0f, 5.25f, 5.75f, 5.5f)
            curveTo(5.38f, 6.0f, 4.88f, 6.88f, 4.88f, 8.5f)
            curveTo(4.88f, 10.5f, 6.12f, 12.38f, 6.62f, 13.0f)
            curveTo(6.88f, 13.38f, 11.25f, 19.38f, 17.5f, 17.12f)
            curveTo(18.25f, 16.88f, 18.5f, 16.25f, 18.62f, 15.75f)
            curveTo(18.75f, 15.0f, 18.75f, 14.38f, 18.62f, 14.25f)
            curveTo(18.5f, 14.12f, 18.25f, 14.12f, 18.0f, 14.12f)
            close()
        }
    }.build()
