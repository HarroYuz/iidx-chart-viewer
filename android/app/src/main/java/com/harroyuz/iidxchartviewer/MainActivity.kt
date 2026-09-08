package com.harroyuz.iidxchartviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.harroyuz.iidxchartviewer.app.AppEvent
import com.harroyuz.iidxchartviewer.app.AppViewModel
import com.harroyuz.iidxchartviewer.ui.AppRoute
import com.harroyuz.iidxchartviewer.ui.auth.BjmLoginActivity
import com.harroyuz.iidxchartviewer.ui.theme.IidxTheme
import com.harroyuz.iidxchartviewer.ui.theme.configureSystemBars
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) viewModel.onLoginCompleted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        setContent { IidxTheme { AppRoute(viewModel) } }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    try {
                        when (event) {
                            AppEvent.Login -> loginLauncher.launch(Intent(this@MainActivity, BjmLoginActivity::class.java))
                            AppEvent.OpenProject -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HarroYuz/iidx-chart-viewer")))
                            AppEvent.Exit -> finishAndRemoveTask()
                            is AppEvent.InstallApk -> {
                                val uri = FileProvider.getUriForFile(this@MainActivity, "${BuildConfig.APPLICATION_ID}.fileprovider", event.file)
                                startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }
                    } catch (error: Exception) {
                        viewModel.onPlatformActionFailed(error)
                    }
                }
            }
        }
    }
}
