package io.github.khoryz666.timer.update

/**
 * A MAJOR.MINOR.PATCH version, optionally with a pre-release suffix ("2.0.0-debug",
 * "v2.1.0-rc1"). A pre-release sorts before the plain release of the same number.
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int, val preRelease: String? = null) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
        // No suffix (a release) sorts after any suffix (a pre-release).
        { it.preRelease == null },
        { it.preRelease },
    )

    override fun toString() = "$major.$minor.$patch" + (preRelease?.let { "-$it" } ?: "")

    companion object {
        private val Pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$""")

        fun parse(text: String): AppVersion? {
            val match = Pattern.matchEntire(text.trim()) ?: return null
            val (major, minor, patch, pre) = match.destructured
            return AppVersion(major.toInt(), minor.toInt(), patch.toInt(), pre.ifEmpty { null })
        }
    }
}
