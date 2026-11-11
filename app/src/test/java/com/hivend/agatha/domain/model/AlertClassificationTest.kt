package com.hivend.agatha.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertClassificationTest {

    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val app = Origin.MobileApp("CEL-01")

    @Test
    fun tagListsMatchTheWeb() {
        assertEquals(
            listOf(
                AlertTag.LEAK_CONFIRMED_IN_FIELD,
                AlertTag.GROUND_MOVEMENT,
                AlertTag.THIRD_PARTY_MACHINERY,
                AlertTag.OTHER_CONFIRMED,
            ),
            AlertTag.forClassification(AlertClassification.CONFIRMED),
        )
        assertEquals(
            listOf(
                AlertTag.EXTREME_WEATHER,
                AlertTag.HEAVY_VEHICLE_TRAFFIC,
                AlertTag.SCHEDULED_MAINTENANCE,
                AlertTag.OTHER_FALSE_ALARM,
            ),
            AlertTag.forClassification(AlertClassification.FALSE_ALARM),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun otherTagRequiresDetail() {
        TagRecord(AlertTag.OTHER_CONFIRMED, otherDetail = " ", taggedAt = now, origin = app)
    }

    @Test(expected = IllegalArgumentException::class)
    fun otherDetailIsLimitedTo100Characters() {
        TagRecord(AlertTag.OTHER_FALSE_ALARM, otherDetail = "x".repeat(101), taggedAt = now, origin = app)
    }

    @Test
    fun otherDetailAcceptsUpTo100Characters() {
        val tag = TagRecord(AlertTag.OTHER_FALSE_ALARM, "x".repeat(100), now, app)
        assertEquals(100, tag.otherDetail?.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tagMustBelongToItsClassification() {
        ClassificationRecord(
            AlertClassification.FALSE_ALARM, now, app,
            tag = TagRecord(AlertTag.GROUND_MOVEMENT, null, now, app),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun tagCannotBeReplaced() {
        ClassificationRecord(AlertClassification.CONFIRMED, now, app)
            .withTag(TagRecord(AlertTag.GROUND_MOVEMENT, null, now, app))
            .withTag(TagRecord(AlertTag.THIRD_PARTY_MACHINERY, null, now, app))
    }
}
