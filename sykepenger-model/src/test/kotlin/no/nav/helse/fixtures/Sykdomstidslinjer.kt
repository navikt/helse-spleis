package no.nav.helse.fixtures

import java.time.LocalDate

private val seed = LocalDate.of(2018, 1, 1)
private var currentDate = seed
private val nextDate get() = currentDate.also { currentDate = currentDate.plusDays(1) }

fun resetSeed() { currentDate = seed }

val Int.S get() = (1..this).map { Sykdomsdag('S', nextDate) }
val Int.F get() = (1..this).map { Sykdomsdag('F', nextDate) }

data class Sykdomsdag(val type: Any, val dato: LocalDate)
