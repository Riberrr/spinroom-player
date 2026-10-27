package io.github.riberrr.spinroom.core.audio.buffer

/**
 * FIFO of decoded [PcmBlock]s in server-time order. Not thread-safe; the engine guards it with a lock.
 * Consumption is tracked with a frame offset into the head block, so the head's current server time is
 * `head.timestampUs + framesToUs(headOffset)`.
 */
class TimedPcmBuffer {
    private val blocks = ArrayDeque<PcmBlock>()
    private var headOffset = 0
    private var bufferedFramesTotal = 0L

    val isEmpty: Boolean get() = blocks.isEmpty()
    val bufferedFrames: Long get() = bufferedFramesTotal

    fun add(block: PcmBlock) {
        if (block.frames == 0) return
        blocks.addLast(block)
        bufferedFramesTotal += block.frames
    }

    fun headBlock(): PcmBlock? = blocks.firstOrNull()

    /** Format of the next frame to play, or null when empty. */
    fun headFormat(): PcmFormat? = blocks.firstOrNull()?.format

    /** Server time of the next frame to play, or null when empty. */
    fun headTimestampUs(): Long? {
        val head = blocks.firstOrNull() ?: return null
        return head.timestampUs + head.format.framesToUs(headOffset.toLong())
    }

    /** Frames left in the head block. */
    fun headRemainingFrames(): Int = blocks.firstOrNull()?.let { it.frames - headOffset } ?: 0

    /** Byte offset of the next frame inside [headBlock]'s data. */
    fun headByteOffset(): Int = headOffset * (blocks.firstOrNull()?.format?.bytesPerFrame ?: 0)

    /** Consumes up to [frames] frames from the head block only; returns how many were consumed. */
    fun advanceHead(frames: Int): Int {
        val head = blocks.firstOrNull() ?: return 0
        val n = minOf(frames, head.frames - headOffset)
        headOffset += n
        bufferedFramesTotal -= n
        if (headOffset >= head.frames) {
            blocks.removeFirst()
            headOffset = 0
        }
        return n
    }

    /** Drops whole blocks that end at or before [serverUs]; returns dropped frames. */
    fun dropBlocksEndingBefore(serverUs: Long): Long {
        var dropped = 0L
        while (true) {
            val head = blocks.firstOrNull() ?: break
            val end = head.timestampUs + head.format.framesToUs(head.frames.toLong())
            if (end > serverUs) break
            dropped += advanceHead(head.frames)
        }
        return dropped
    }

    fun clear() {
        blocks.clear()
        headOffset = 0
        bufferedFramesTotal = 0
    }

    /** Server time just after the last buffered frame, or null when empty. */
    fun endTimestampUs(): Long? {
        val last = blocks.lastOrNull() ?: return null
        return last.timestampUs + last.format.framesToUs(last.frames.toLong())
    }
}
