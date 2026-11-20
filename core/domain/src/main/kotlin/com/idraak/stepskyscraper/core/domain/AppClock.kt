package com.idraak.stepskyscraper.core.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Injected time source so every calculation is deterministic in tests. */
interface AppClock {
    fun now(): Instant

    fun zone(): ZoneId

    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())

    fun localNow(): LocalDateTime = LocalDateTime.ofInstant(now(), zone())
}
