package io.ionic.libs.ionfilesystemlib.helper.internal

import android.os.Build

/**
 * Build config wrapper object
 */
internal object IONFLSTBuildConfig {
    fun getAndroidSdkVersionCode(): Int = Build.VERSION.SDK_INT
}