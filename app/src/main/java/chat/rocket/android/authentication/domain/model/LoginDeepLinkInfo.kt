package chat.rocket.android.authentication.domain.model

import android.annotation.SuppressLint
import android.content.Intent
import androidx.core.net.toUri
import android.net.Uri
import android.os.Parcelable
import chat.rocket.android.util.extensions.decodeUrl
import chat.rocket.android.util.extensions.isNotNullNorEmpty
import chat.rocket.android.util.extensions.toJsonObject
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

private const val JSON_CREDENTIAL_TOKEN = "credentialToken"
private const val JSON_CREDENTIAL_SECRET = "credentialSecret"
private const val OAUTH_STATE = "state"

@SuppressLint("ParcelCreator")
@Parcelize
data class LoginDeepLinkInfo(
    val url: String,
    val userId: String?,
    val token: String?,
    val credentialToken: String?,
    val credentialSecret: String?,
    val oauthState: String?
) : Parcelable

fun Intent.getLoginDeepLinkInfo(): LoginDeepLinkInfo? {
    val uri = data
    return if (action == Intent.ACTION_VIEW && uri != null) {
        if (uri.isAuthenticationDeepLink()) {
            val host = uri.getQueryParameter("host")
            val url = if (host.startsWith("http")) host else "https://$host"
            val userId = uri.getQueryParameter("userId")
            val token = uri.getQueryParameter("token")
            try {
                LoginDeepLinkInfo(url, userId, token, null, null, null)
            } catch (ex: Exception) {
                Timber.d(ex, "Error parsing login deeplink")
                null
            }
        } else if (uri.isOauthCallbackDeepLink()) {
            val url = uri.toString().toUri().scheme + "://" + uri.toString().toUri().host
            val jsonResult = uri
                    .toString()
                    .decodeUrl()
                    .substringAfter("#")
                    .toJsonObject()
            val credentialToken = jsonResult.optString(JSON_CREDENTIAL_TOKEN)
            val credentialSecret = jsonResult.optString(JSON_CREDENTIAL_SECRET)
            val oauthState = uri
                    .toString()
                    .substringBefore("#")
                    .toUri()
                    .getQueryParameter(OAUTH_STATE)
            try {
                LoginDeepLinkInfo(url, null, null, credentialToken, credentialSecret, oauthState)
            } catch (ex: Exception) {
                Timber.d(ex, "Error parsing login deeplink")
                null
            }
        } else {
            Timber.d("Error parsing login deeplink")
            null
        }
    } else null
}

private inline fun Uri.isAuthenticationDeepLink(): Boolean {
    if (host == "auth")
        return true
    else if (host == "go.rocket.chat" && path == "/auth")
        return true
    return false
}

private inline fun Uri.isOauthCallbackDeepLink(): Boolean {
    if (toString().contains(JSON_CREDENTIAL_TOKEN) && toString().contains(JSON_CREDENTIAL_SECRET)) {
        return true
    }
    return false
}