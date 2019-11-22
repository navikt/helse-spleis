package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Fødselsnummer(verdi: String, private val startDato: LocalDate, private val sluttDato: LocalDate) {

    private val individnummer = verdi.substring(6, 9).toInt()
    private val fødselsdag = LocalDate.of(verdi.substring(4, 6).toInt().toYear(individnummer), verdi.substring(2, 4).toInt(), verdi.substring(0, 2).toInt().toDay())

    internal val navBurdeBetale get(): BurdeBetale {
        return when {
            fødselsdag.plusYears(67).isAfter(sluttDato) -> { antallDager: Int, _: Int, _: LocalDate -> antallDager < 248}
            fødselsdag.plusYears(70).isBefore(startDato) -> {_: Int, _: Int, _: LocalDate -> false}
            fødselsdag.plusYears(67).isAfter(startDato) -> {antallDager: Int, antallDagerEtter60: Int, dagen: LocalDate -> fyller67IPerioden(antallDager, antallDagerEtter60, dagen)}
            else -> {antallDager: Int, _: Int, dagen: LocalDate -> antallDager < 60 && dagen.isBefore(fødselsdag.plusYears(70))}
        }
    }

    private fun Int.toDay() = if (this > 40) this - 40 else this
    private fun Int.toYear(individnummer: Int): Int {
        return this + when {
            this in (54..99) && individnummer in (500..749) -> 1800
            this in (0..99) && individnummer in (0..499) -> 1900
            this in (40..99) && individnummer in (900..999) -> 1900
            else -> 2000
        }
    }
    private fun fyller67IPerioden(antallDager: Int, antallDagerEtter67: Int, dagen: LocalDate): Boolean {
        if (dagen.isBefore(fødselsdag.plusYears(67)) && antallDager < 248) return true
        return antallDager < 248 && antallDagerEtter67 < 60
    }

}

internal typealias BurdeBetale = (Int, Int, LocalDate) -> Boolean