package com.harroyuz.iidxchartviewer.app

import java.io.File

internal sealed interface AppEvent {
    data object Login : AppEvent
    data object OpenProject : AppEvent
    data object Exit : AppEvent
    data class InstallApk(val file: File) : AppEvent
}
