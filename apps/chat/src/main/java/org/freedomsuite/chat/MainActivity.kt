package org.freedomsuite.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import org.freedomsuite.core.applock.ui.AppLockGate
import org.freedomsuite.core.ui.FreedomTheme
import org.freedomsuite.core.ui.PlaceholderScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreedomTheme {
                AppLockGate {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        PlaceholderScreen(
                            appName = "Freedom Chat\nComing soon · Local LLM / OpenAI-compatible API",
                        )
                    }
                }
            }
        }
    }
}
