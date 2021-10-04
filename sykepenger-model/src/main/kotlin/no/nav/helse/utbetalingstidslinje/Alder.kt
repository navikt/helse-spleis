package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Fødselsnummer
import no.nav.helse.Grunnbeløp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit.YEARS

internal class Alder(fødselsnummer: Fødselsnummer) {
    private val fødselsdato = fødselsnummer.fødselsdato
    internal val datoForØvreAldersgrense = fødselsdato.plusYears(70).trimHelg()
    internal val redusertYtelseAlder = fødselsdato.plusYears(67)
    private val forhøyetInntektskravAlder = fødselsdato.plusYears(67)

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
