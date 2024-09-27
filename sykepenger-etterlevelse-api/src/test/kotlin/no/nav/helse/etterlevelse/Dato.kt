package no.nav.helse.etterlevelse

import java.time.LocalDate

private val frø = LocalDate.of(2018, 1, 1)
val Int.januar get() = frø.withMonth(1).withDayOfMonth(this)