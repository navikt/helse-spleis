package no.nav.helse.util.interval

import java.time.LocalDate

interface Interval {
    fun startdato(): LocalDate
    fun sluttdato(): LocalDate
    fun antallSykedager(): Int

//    operator fun plus(other: Interval)
}
