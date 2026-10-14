package io.github.khoryz666.timer.update

import org.json.JSONException
import org.json.JSONObject

/** A published GitHub release with an installable APK. */
data class Release(val version: AppVersion, val notes: String, val apkUrl: String, val apkSize: Long) {

    fun isNewerThan(installed: String): Boolean {
        val current = AppVersion.parse(installed) ?: return false
        return version > current
    }

    companion object {
        /**
         * Parses the GitHub "latest release" JSON. Returns null when the tag isn't a version or no
         * .apk is attached.
         */
        fun parse(json: String): Release? = try {
            val root = JSONObject(json)
            val version = AppVersion.parse(root.getString("tag_name"))
            val assets = root.optJSONArray("assets")
            val apk = assets?.let { list ->
                (0 until list.length()).map(list::getJSONObject).firstOrNull { it.getString("name").endsWith(".apk") }
            }
            if (version == null || apk == null) {
                null
            } else {
                Release(
                    version = version,
                    notes = plainNotes(root.optString("body")),
                    apkUrl = apk.getString("browser_download_url"),
                    apkSize = apk.optLong("size", -1),
                )
            }
        } catch (e: JSONException) {
            null
        }

        /** Turns the release's Markdown into readable plain text: no heading marks, bullets as "•". */
        fun plainNotes(markdown: String): String = markdown
            .replace("\r\n", "\n")
            .lines()
            .joinToString("\n") { line ->
                line.trimEnd()
                    .replace(Regex("""^#+\s*"""), "")
                    .replace(Regex("""^(\s*)[-*]\s+"""), "$1• ")
                    .replace("**", "")
                    .replace("`", "")
            }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
