package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit.YEARS

internal class Alder(fødselsnummer: String) {
    private val individnummer = fødselsnummer.substring(6, 9).toInt()
    private val fødselsdato = LocalDate.of(
        fødselsnummer.substring(4, 6).toInt().toYear(individnummer),
        fødselsnummer.substring(2, 4).toInt(),
        fødselsnummer.substring(0, 2).toInt().toDay()
    )
    internal val datoForØvreAldersgrense = fødselsdato.plusYears(70).trimHelg()
    internal val redusertYtelseAlder = fødselsdato.plusYears(67)
    private val forhøyetInntektskravAlder = fødselsdato.plusYears(67)

    private fun Int.toDay() = if (this > 40) this - 40 else this

    private fun Int.toYear(individnummer: Int): Int {
        return this + when {
            this in (54..99) && individnummer in (500..749) -> 1800
            this in (0..99) && individnummer in (0..499) -> 1900
            this in (40..99) && individnummer in (900..999) -> 1900
            else -> 2000
        }
    }

    private fun LocalDate.trimHelg() = this.minusDays(
        when (this.dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            else -> 0
        }
    )

    internal fun alderPåDato(dato: LocalDate) = YEARS.between(fødselsdato, dato).toInt()
    private fun alderVedSluttenAvÅret(year: Year) = YEARS.between(Year.from(fødselsdato), year).toInt()

    internal fun minimumInntekt(dato: LocalDate) = (if (forhøyetInntektskrav(dato)) Grunnbeløp.`2G` else Grunnbeløp.halvG).dagsats(dato)

    internal fun forhøyetInntektskrav(dato: LocalDate) = dato > forhøyetInntektskravAlder

    internal fun beregnFeriepenger(opptjeningsår: Year, beløp: Int) =
        beløp * if (alderVedSluttenAvÅret(opptjeningsår) < 59) 0.102 else 0.125
}
