package io.github.riberrr.spinroom.core.data

/** Facts about the background key service (SUI9) that `:feature:settings` needs but `:app` owns. */
object GlobalKeysSupport {
    /** Class name of the accessibility service in `:app` (the package is the running application id). */
    const val SERVICE_CLASS = "io.github.riberrr.spinroom.GlobalKeysService"

    /** false in a build that ships without the service (Google Play); `:app` sets it from `BuildConfig` at start. */
    @Volatile var available: Boolean = true
}
