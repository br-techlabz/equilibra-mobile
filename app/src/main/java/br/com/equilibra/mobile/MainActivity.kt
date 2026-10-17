package br.com.equilibra.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.equilibra.mobile.navigation.AppNavHost
import br.com.equilibra.mobile.ui.theme.EquilibraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EquilibraTheme {
                AppNavHost()
            }
        }
    }
}
