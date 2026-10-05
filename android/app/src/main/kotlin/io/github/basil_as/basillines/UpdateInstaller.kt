package io.github.basil_as.basillines

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.basil_as.basillines.engine.UpdateInfo
import io.github.basil_as.basillines.engine.Updates
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Updating without leaving the app: download the release APK into the cache, prove it is the file the release
 * published (SHA-256 from the same release) and that it is this app signed with the project key and newer, and only
 * then hand it to the system installer, which still asks the user and refuses anything signed with another key.
 */
object UpdateInstaller {
    private const val MAX_APK_BYTES = 150L * 1024 * 1024
    private const val MAX_SUMS_BYTES = 64 * 1024
    private const val MAX_REDIRECTS = 5

    enum class Failure { NETWORK, CHECKSUM, WRONG_APP }

    sealed interface Outcome {
        data class Ready(val file: File) : Outcome
        data class Failed(val why: Failure) : Outcome
    }

    private fun updatesDir(context: Context) = File(context.cacheDir, "updates")

    /** Opens [start], following GitHub's redirects to its CDN, but never leaving GitHub or plain HTTPS. */
    private fun open(start: String, agent: String): HttpURLConnection? {
        var url = start
        repeat(MAX_REDIRECTS + 1) {
            if (!Updates.isAllowedDownloadUrl(url)) return null
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", agent)
            }
            when (conn.responseCode) {
                200 -> return conn
                301, 302, 303, 307, 308 -> {
                    val next = conn.getHeaderField("Location")
                    conn.disconnect()
                    url = next ?: return null
                }
                else -> { conn.disconnect(); return null }
            }
        }
        return null
    }

    private fun fetchSums(url: String, agent: String): String? {
        val conn = open(url, agent) ?: return null
        return try {
            conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                while (out.size() < MAX_SUMS_BYTES) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads and checks the update; [onProgress] gets 0..1 (or -1 when the size is unknown). */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): Outcome = withContext(Dispatchers.IO) {
        val agent = "ColorLines/" + UpdateClient.versionName(context)
        val dir = updatesDir(context).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "ColorLines.apk.part")
        val apk = File(dir, "ColorLines.apk")
        try {
            val expected = runCatching {
                fetchSums(Updates.sumsUrl(info.url), agent)?.let { Updates.checksumFor(it, Updates.ASSET_NAME) }
            }.getOrNull() ?: return@withContext Outcome.Failed(Failure.CHECKSUM)
            val conn = runCatching { open(info.url, agent) }.getOrNull() ?: return@withContext Outcome.Failed(Failure.NETWORK)
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                val total = conn.contentLengthLong
                if (total > MAX_APK_BYTES) return@withContext Outcome.Failed(Failure.NETWORK)
                var done = 0L
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            done += n
                            if (done > MAX_APK_BYTES) return@withContext Outcome.Failed(Failure.NETWORK)
                            digest.update(buf, 0, n)
                            out.write(buf, 0, n)
                            onProgress(if (total > 0) done.toFloat() / total else -1f)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected) return@withContext Outcome.Failed(Failure.CHECKSUM)
            if (!part.renameTo(apk)) return@withContext Outcome.Failed(Failure.NETWORK)
            if (!isOurNewerBuild(context, apk)) {
                apk.delete()
                return@withContext Outcome.Failed(Failure.WRONG_APP)
            }
            Outcome.Ready(apk)
        } catch (e: java.io.IOException) {
            part.delete()
            Outcome.Failed(Failure.NETWORK)
        }
    }

    /** The package inside the file is this app, signed with the project key, and a higher version than the running one. */
    private fun isOurNewerBuild(context: Context, apk: File): Boolean = runCatching {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val pkg = pm.getPackageArchiveInfo(apk.path, flags) ?: return false
        if (pkg.packageName != context.packageName) return false
        val cert = if (Build.VERSION.SDK_INT >= 28) pkg.signingInfo?.apkContentsSigners?.firstOrNull()
        else @Suppress("DEPRECATION") pkg.signatures?.firstOrNull()
        val sha = cert?.let { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) } ?: return false
        Updates.isProjectKey(sha) && Updates.isNewer(pkg.versionName ?: return false, UpdateClient.versionName(context))
    }.getOrDefault(false)

    /** Android 8+ asks for a per-app "install unknown apps" permission the first time. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    /** The system screen where the user allows this app to install updates. */
    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the system installer on the verified file. */
    fun install(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess
}
