package io.github.riberrr.spinroom.core.data

/** A live stream (radio) has no meaningful length: MA sends no duration or 0. */
fun isLive(durationMs: Long): Boolean = durationMs <= 0
