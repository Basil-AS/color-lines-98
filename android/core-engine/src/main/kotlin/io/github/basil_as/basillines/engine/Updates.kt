package io.github.basil_as.basillines.engine

/** What the newest GitHub release offers. */
data class UpdateInfo(val version: String, val url: String)

/** Reading the GitHub "latest release" answer and deciding whether it is newer; no network code here. */
object Updates {
    /** The only place an update may come from. The system installer additionally refuses a file not signed with this app's key. */
    const val ASSET_PREFIX = "https://github.com/Basil-AS/color-lines-98/releases/"
    const val ASSET_NAME = "ColorLines.apk"

    /** "v1.5.4" or "1.5.4" as numbers; null for anything else. */
    fun parse(version: String): List<Int>? {
        val parts = version.trim().removePrefix("v").split(".")
        if (parts.size !in 2..4) return null
        return parts.map { it.toIntOrNull()?.takeIf { n -> n in 0..99_999 } ?: return null }
    }

    /** True when [candidate] is a higher version than [current]; unreadable versions are never newer. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** From the JSON of /releases/latest: the version and the direct APK link, or null if the answer is not what we expect. */
    fun fromReleaseJson(text: String, current: String): UpdateInfo? {
        val root = Json.parseOrNull(text) as? Map<*, *> ?: return null
        val tag = root["tag_name"] as? String ?: return null
        if (!isNewer(tag, current)) return null
        val assets = root["assets"] as? List<*> ?: return null
        val url = assets.mapNotNull { it as? Map<*, *> }
            .firstOrNull { it["name"] == ASSET_NAME }
            ?.get("browser_download_url") as? String ?: return null
        if (!url.startsWith(ASSET_PREFIX)) return null
        return UpdateInfo(tag.removePrefix("v"), url)
    }

    /** SHA256SUMS.txt lives next to the APK in the same release. */
    const val SUMS_NAME = "SHA256SUMS.txt"

    fun sumsUrl(apkUrl: String): String = apkUrl.substringBeforeLast('/') + "/" + SUMS_NAME

    /** The hex digest listed for [name] in `sha256sum` output ("<hex>  <name>" or "<hex> *<name>"), lower-cased, or null. */
    fun checksumFor(sums: String, name: String): String? = sums.lineSequence()
        .map { it.trim() }
        .mapNotNull { line ->
            val hex = line.substringBefore(' ')
            val file = line.substringAfter(' ', "").trim().removePrefix("*")
            if (file == name && hex.length == 64 && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) hex.lowercase() else null
        }
        .firstOrNull()

    /** A download (also after a redirect) may only come from GitHub over HTTPS. */
    fun isAllowedDownloadUrl(url: String): Boolean {
        if (!url.startsWith("https://")) return false
        val host = url.removePrefix("https://").substringBefore('/').substringBefore('?').substringBefore(':').lowercase()
        return host == "github.com" || host.endsWith(".githubusercontent.com")
    }

    /** Hex fingerprint grouped in pairs: "5D:30:30:E9…". */
    fun fingerprint(sha256: ByteArray): String = sha256.joinToString(":") { "%02X".format(it) }

    /** Short form for display: the first 8 bytes. */
    fun shortFingerprint(sha256: ByteArray): String = fingerprint(sha256.copyOf(8)) + "…"

    /** SHA-256 of the project's release certificate (checked on the built APK with apksigner). */
    const val PROJECT_KEY_SHA256 = "5D3030E9363804C3F732D1A72BB683E1997F73D0454A1362C0FF5BF463271993"

    fun isProjectKey(sha256: ByteArray): Boolean = sha256.joinToString("") { "%02X".format(it) } == PROJECT_KEY_SHA256
}
