package com.tally.domain.notify

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import org.junit.jupiter.api.Test

class ShareReplyTest {
    private val paid = Pence(3_840)

    @Test fun `plain and pound-prefixed amounts are read`() {
        assertThat(parseShareReply("12.50", paid)).isEqualTo(ShareReply.Valid(Pence(1_250)))
        assertThat(parseShareReply("£12.50", paid)).isEqualTo(ShareReply.Valid(Pence(1_250)))
        assertThat(parseShareReply(" 12 ", paid)).isEqualTo(ShareReply.Valid(Pence(1_200)))
        assertThat(parseShareReply(".5", paid)).isEqualTo(ShareReply.Valid(Pence(50)))
    }

    @Test fun `zero and the full amount are both valid`() {
        assertThat(parseShareReply("0", paid)).isEqualTo(ShareReply.Valid(Pence.ZERO))
        assertThat(parseShareReply("38.40", paid)).isEqualTo(ShareReply.Valid(paid))
    }

    @Test fun `a share above the amount paid is rejected, not capped`() {
        assertThat(parseShareReply("38.41", paid)).isEqualTo(ShareReply.TooHigh)
        assertThat(parseShareReply("100", paid)).isEqualTo(ShareReply.TooHigh)
    }

    @Test fun `text that is not an amount is unreadable`() {
        assertThat(parseShareReply(null, paid)).isEqualTo(ShareReply.Unreadable)
        assertThat(parseShareReply("", paid)).isEqualTo(ShareReply.Unreadable)
        assertThat(parseShareReply("half", paid)).isEqualTo(ShareReply.Unreadable)
        assertThat(parseShareReply("12.345", paid)).isEqualTo(ShareReply.Unreadable)
        assertThat(parseShareReply("-5", paid)).isEqualTo(ShareReply.Unreadable)
    }
}
