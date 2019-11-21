package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Fødselsnummer(verdi: String, private val startDato: LocalDate, private val sluttDato: LocalDate) {

    private val fødselsdag = LocalDate.of(verdi.substring(4, 6).toInt().toYear(), verdi.substring(2, 4).toInt(), verdi.substring(0, 2).toInt().toDay())

    internal val burdeBetale get(): BurdeBetale {
        return when {
            fødselsdag.plusYears(67).isAfter(sluttDato) -> { antallDager: Int, _: LocalDate -> antallDager < 248}
            fødselsdag.plusYears(70).isBefore(startDato) -> {_: Int, _: LocalDate -> false}
            fødselsdag.plusYears(67).isAfter(startDato) -> {antallDager: Int, dagen: LocalDate -> antallDager < (if (dagen.isBefore(fødselsdag.plusYears(67))) 248 else 60)}
            else -> {antallDager: Int, dagen: LocalDate -> antallDager < 60 && dagen.isBefore(fødselsdag.plusYears(70))}
        }
    }

    private fun Int.toDay() = if (this > 40) this - 40 else this
    private fun Int.toYear(): Int {
        val iÅr = LocalDate.now().year
        return this + if (iÅr - 2000 - 10 < this) 1900 else 2000
    }

}

internal typealias BurdeBetale = (Int, LocalDate) -> Boolean