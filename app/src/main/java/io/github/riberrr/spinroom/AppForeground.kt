package io.github.riberrr.spinroom

/** True while [MainActivity] is visible. The background key service stays out of the way then: the app handles its own keys. */
object AppForeground {
    @Volatile var visible: Boolean = false
}
