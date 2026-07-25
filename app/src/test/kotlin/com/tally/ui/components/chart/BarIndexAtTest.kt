package com.tally.ui.components.chart

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BarIndexAtTest {
    // 7 bars across 700px: gap = 700/28 = 25, barWidth = (700 - 25*6)/7 = 78.57, slot = 103.57
    private val width = 700f
    private val count = 7

    @Test fun `the far left is the first bar and the far right is the last`() {
        assertThat(barIndexAt(0f, width, count)).isEqualTo(0)
        assertThat(barIndexAt(width, width, count)).isEqualTo(count - 1)
    }

    @Test fun `a touch lands on the bar it is over`() {
        // Centre of bar 3 (0-based) sits within its slot.
        val slot = width / count
        assertThat(barIndexAt(slot * 3 + slot / 2, width, count)).isEqualTo(3)
        assertThat(barIndexAt(slot * 5 + slot / 2, width, count)).isEqualTo(5)
    }

    @Test fun `out-of-bounds touches clamp instead of throwing`() {
        assertThat(barIndexAt(-50f, width, count)).isEqualTo(0)
        assertThat(barIndexAt(width * 2, width, count)).isEqualTo(count - 1)
    }

    @Test fun `every position maps inside the value range`() {
        var x = 0f
        while (x <= width) {
            val index = barIndexAt(x, width, count)
            assertThat(index).isNotNull()
            assertThat(index!!).isAtLeast(0)
            assertThat(index).isAtMost(count - 1)
            x += 7f
        }
    }

    @Test fun `a single bar always resolves to index zero`() {
        assertThat(barIndexAt(0f, width, 1)).isEqualTo(0)
        assertThat(barIndexAt(width / 2, width, 1)).isEqualTo(0)
    }

    @Test fun `no bars or no width resolves to null`() {
        assertThat(barIndexAt(10f, width, 0)).isNull()
        assertThat(barIndexAt(10f, 0f, count)).isNull()
    }

    @Test fun `a 31-day month resolves distinct days across the width`() {
        val days = 31
        assertThat(barIndexAt(0f, width, days)).isEqualTo(0)
        assertThat(barIndexAt(width, width, days)).isEqualTo(days - 1)
        val slot = width / days
        assertThat(barIndexAt(slot * 15 + slot / 2, width, days)).isEqualTo(15)
    }
}
