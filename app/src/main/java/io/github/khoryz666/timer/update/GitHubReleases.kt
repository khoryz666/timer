package io.github.khoryz666.timer.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

/** An update failure with a message fit to show the user. */
class UpdateException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Reads the latest release of [repo] ("owner/name") and downloads its APK. */
class GitHubReleases(private val repo: String, private val userAgent: String) {

    suspend fun latest(): Release = withContext(Dispatchers.IO) {
        request("https://api.github.com/repos/$repo/releases/latest") { connection ->
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    Release.parse(body) ?: throw UpdateException("The latest release has no APK attached.")
                }
                HttpURLConnection.HTTP_NOT_FOUND -> throw UpdateException("No release has been published yet.")
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> throw UpdateException("GitHub's rate limit was reached. Try again in a while.")
                else -> throw UpdateException("GitHub answered with HTTP $code.")
            }
        }
    }

    /** Downloads [release]'s APK to [target], reporting progress from 0 to 1. */
    suspend fun download(release: Release, target: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        request(release.apkUrl) { connection ->
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateException("Download failed with HTTP ${connection.responseCode}.")
            }
            val total = release.apkSize.takeIf { it > 0 } ?: connection.contentLengthLong
            var written = 0L
            target.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress(written.toFloat() / total)
                    }
                }
            }
            if (release.apkSize > 0 && written != release.apkSize) {
                target.delete()
                throw UpdateException("The download was incomplete. Try again.")
            }
        }
    }

    private inline fun <T> request(url: String, block: (HttpURLConnection) -> T): T {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", userAgent)
            }
        } catch (e: IOException) {
            throw e.asUpdateException()
        }
        return try {
            block(connection)
        } catch (e: IOException) {
            throw e.asUpdateException()
        } finally {
            connection.disconnect()
        }
    }

    private fun IOException.asUpdateException(): UpdateException = when (this) {
        is UpdateException -> this
        is UnknownHostException -> UpdateException("No internet connection.", this)
        else -> UpdateException("Couldn't reach GitHub: ${message ?: javaClass.simpleName}", this)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
    }
}
