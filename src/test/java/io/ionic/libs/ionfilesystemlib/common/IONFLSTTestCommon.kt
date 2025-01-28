package io.ionic.libs.ionfilesystemlib.common

import android.net.Uri

/**
 * Uri.parse expects a the uri string to already be encoded, so this is a small helper method
 *  to make sure that spaces and other characters get encoded
 */
internal fun fileUriWithEncodings(uriString: String): Uri =
    Uri.parse(Uri.encode(uriString, "/:"))