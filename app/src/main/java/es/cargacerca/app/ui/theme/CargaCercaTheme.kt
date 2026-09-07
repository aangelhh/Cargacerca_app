package es.cargacerca.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CargaCercaColors = darkColorScheme(
    primary = Color(0xFF2FA9FF),
    secondary = Color(0xFF36D6FF),
    tertiary = Color(0xFF37E68A),
    background = Color(0xFF07111F),
    surface = Color(0xFF0D1B2B),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F8FC),
    onSurface = Color(0xFFF5F8FC)
)

@Composable
fun CargaCercaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CargaCercaColors,
        content = content
    )
}
