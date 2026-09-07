package es.cargacerca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import es.cargacerca.app.ui.home.CargaCercaHome
import es.cargacerca.app.ui.theme.CargaCercaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CargaCercaTheme {
                CargaCercaHome()
            }
        }
    }
}
