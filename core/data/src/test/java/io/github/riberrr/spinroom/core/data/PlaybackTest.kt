package io.github.riberrr.spinroom.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {
    @Test fun zeroAndNegativeDurationIsLive() {
        assertTrue(isLive(0))
        assertTrue(isLive(-1))
    }

    @Test fun positiveDurationIsNotLive() = assertFalse(isLive(221_000))
}
