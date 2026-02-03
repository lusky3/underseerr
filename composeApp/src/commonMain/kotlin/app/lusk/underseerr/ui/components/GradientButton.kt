package app.lusk.underseerr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients

/**
 * A stylized button that uses the app's gradient theme.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    brush: Brush? = null,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val gradients = LocalUnderseerrGradients.current
    val finalBrush = brush ?: gradients.primary
    
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(finalBrush, alpha = alpha),
        color = Color.Transparent,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
