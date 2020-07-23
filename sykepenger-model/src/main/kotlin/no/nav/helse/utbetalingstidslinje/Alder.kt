package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit.YEARS

internal class Alder(fødselsnummer: String) {
    private val individnummer = fødselsnummer.substring(6, 9).toInt()
    private val fødselsdag = LocalDate.of(
        fødselsnummer.substring(4, 6).toInt().toYear(individnummer),
        fødselsnummer.substring(2, 4).toInt(),
        fødselsnummer.substring(0, 2).toInt().toDay()
    )
    internal val øvreAldersgrense = fødselsdag.plusYears(70).øvreAldersgrense()
    internal val redusertYtelseAlder = fødselsdag.plusYears(67)

    private fun Int.toDay() = if (this > 40) this - 40 else this

    private fun Int.toYear(individnummer: Int): Int {
        return this + when {
            this in (54..99) && individnummer in (500..749) -> 1800
            this in (0..99) && individnummer in (0..499) -> 1900
            this in (40..99) && individnummer in (900..999) -> 1900
            else -> 2000
        }
    }

    private fun LocalDate.øvreAldersgrense() = this.minusDays(
        when (this.dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            else -> 0
        }
    )

    internal fun alderPåDato(dato: LocalDate) = YEARS.between(fødselsdag, dato).toInt();

    internal fun minimumInntekt(dato: LocalDate): Double {
        return (if (dato <= redusertYtelseAlder) Grunnbeløp.halvG else Grunnbeløp.`2G`).dagsats(dato).tilDagligInt().toDouble()
    }
}
