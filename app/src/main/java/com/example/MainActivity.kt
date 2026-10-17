package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.lock.LockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.vault.VaultScreen
import com.example.viewmodel.AuthStep
import com.example.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    PsbApp()
                }
            }
        }
    }
}

@Composable
fun PsbApp(viewModel: VaultViewModel = viewModel()) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val authStep by viewModel.currentAuthStep.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()

    BackHandler {
        if (isUnlocked) {
            if (currentFolderId != "root" && breadcrumbs.size > 1) {
                val previousBreadcrumb = breadcrumbs[breadcrumbs.size - 2]
                viewModel.navigateToBreadcrumb(previousBreadcrumb.id)
            } else {
                viewModel.lockVault()
            }
        } else {
            if (authStep != AuthStep.GUARDIAN) {
                viewModel.stepBack()
            }
        }
    }

    if (isUnlocked) {
        VaultScreen(viewModel = viewModel)
    } else {
        LockScreen(viewModel = viewModel)
    }
}
