package io.ionic.libs.ionfilesystemlib.controller.internal

import android.os.Build

/**
 * Build config wrapper object
 */
internal object IONFLSTBuildConfig {
    fun getAndroidSdkVersionCode(): Int = Build.VERSION.SDK_INT
}