package io.ionic.libs.ionfilesystemlib.helper.common

import android.os.Build

/**
 * Build config wrapper object
 */
internal object IONFILEBuildConfig {
    fun getAndroidSdkVersionCode(): Int = Build.VERSION.SDK_INT
}