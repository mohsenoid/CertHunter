package com.mohsenoid.certhunter.di

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemDefaultZoneClockTest {

    private lateinit var originalDefault: TimeZone

    @BeforeEach
    fun setUp() {
        originalDefault = TimeZone.getDefault()
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalDefault)
    }

    @Test
    fun `given system zone changes after first access when zone is read again then clock reports the new zone`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val first = SystemDefaultZoneClock.zone

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        val second = SystemDefaultZoneClock.zone

        assertEquals(ZoneId.of("America/Los_Angeles"), first)
        assertEquals(ZoneId.of("Asia/Tokyo"), second)
    }

    @Test
    fun `given default zone is fixed when instant is read then it tracks live time`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val first = SystemDefaultZoneClock.instant()
        val second = SystemDefaultZoneClock.instant()

        // instant() must be live, not snapshotted; ordering proves it's reading the clock each call.
        // kotlin.test.assertTrue is used instead of Kotlin's `assert` so the check runs even when
        // JVM assertions are disabled.
        assertTrue(!second.isBefore(first), "instant() must read live time on each call")
    }
}
