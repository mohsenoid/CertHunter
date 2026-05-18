package com.mohsenoid.certhunter.di

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

// Clock.systemDefaultZone() snapshots ZoneId.systemDefault() at construction, so a long-lived
// singleton would freeze certificate-date projection on the zone the process started in.
// This object resolves the zone on every access so a runtime timezone change is picked up
// without restarting the app.
internal object SystemDefaultZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
    override fun instant(): Instant = Instant.now()
}
