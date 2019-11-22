package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Fødselsnummer(verdi: String, private val startDato: LocalDate, private val sluttDato: LocalDate) {

    private val maksSykepengedager = 248
    private val maksSykepengedagerEtter67 = 60
    private val ytesIkkeSykepengerAlder: Long = 70
    private val ytesMindreSykepengedagerAlder: Long = 67
    private val individnummer = verdi.substring(6, 9).toInt()
    private val fødselsdag = LocalDate.of(verdi.substring(4, 6).toInt().toYear(individnummer), verdi.substring(2, 4).toInt(), verdi.substring(0, 2).toInt().toDay())

    internal val navBurdeBetale get(): BurdeBetale {
        return when {
            fødselsdag.plusYears(ytesMindreSykepengedagerAlder).isAfter(sluttDato) -> { antallDager: Int, _: Int, _: LocalDate -> antallDager < maksSykepengedager}
            fødselsdag.plusYears(ytesIkkeSykepengerAlder).isBefore(startDato) -> {_: Int, _: Int, _: LocalDate -> false}
            fødselsdag.plusYears(ytesMindreSykepengedagerAlder).isAfter(startDato) -> {antallDager: Int, antallDagerEtter60: Int, dagen: LocalDate -> fyller67IPerioden(antallDager, antallDagerEtter60, dagen)}
            else -> {antallDager: Int, _: Int, dagen: LocalDate -> antallDager < maksSykepengedagerEtter67 && dagen.isBefore(fødselsdag.plusYears(ytesIkkeSykepengerAlder))}
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
        if (dagen.isBefore(fødselsdag.plusYears(ytesMindreSykepengedagerAlder)) && antallDager < maksSykepengedager) return true
        return antallDager < maksSykepengedager && antallDagerEtter67 < maksSykepengedagerEtter67
    }

    internal fun harFylt67(dagen: LocalDate) = dagen.isAfter(fødselsdag.plusYears(ytesMindreSykepengedagerAlder))

}

internal typealias BurdeBetale = (Int, Int, LocalDate) -> Boolean