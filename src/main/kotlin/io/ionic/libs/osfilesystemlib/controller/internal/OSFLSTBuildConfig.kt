package io.ionic.libs.osfilesystemlib.controller.internal

import android.os.Build

/**
 * Build config wrapper object
 */
internal object OSFLSTBuildConfig {
    fun getAndroidSdkVersionCode(): Int = Build.VERSION.SDK_INT
}