package io.github.basil_as.basillines

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.basil_as.basillines.engine.UpdateInfo
import io.github.basil_as.basillines.engine.Updates
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The update check: one HTTPS request to GitHub, a few seconds at most, never blocks or crashes the game. */
object UpdateClient {
    private const val LATEST = "https://api.github.com/repos/Basil-AS/color-lines-98/releases/latest"

    sealed interface Result {
        data class Newer(val info: UpdateInfo) : Result
        data object UpToDate : Result
        data object Failed : Result
    }

    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        val current = versionName(context)
        if (System.getProperty("colorlines.offline") == "true") return@withContext Result.Failed
        runCatching {
            val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ColorLines/$current")
            }
            try {
                if (conn.responseCode != 200) return@runCatching Result.Failed
                // A release answer is a few kilobytes; refuse anything much bigger.
                val body = conn.inputStream.use { it.readNBytes(512 * 1024) }.toString(Charsets.UTF_8)
                Updates.fromReleaseJson(body, current)?.let { Result.Newer(it) } ?: Result.UpToDate
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(Result.Failed)
    }

    /** SHA-256 of the certificate this app was signed with, or null if it cannot be read. */
    fun signatureSha256(context: Context): ByteArray? = runCatching {
        val pm = context.packageManager
        val cert = if (android.os.Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
        }
        cert?.let { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) }
    }.getOrNull()

    /** Hands the APK link to the browser, which downloads it; the system installer then updates the app in place. */
    fun download(context: Context, info: UpdateInfo) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
