package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-33 ledd 3`
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit.YEARS

internal class Alder(private val fødselsdato: LocalDate) {
    internal val søttiårsdagen = fødselsdato.plusYears(70)
    internal val datoForØvreAldersgrense = søttiårsdagen.trimHelg()
    internal val sisteVirkedagFørFylte70år = datoForØvreAldersgrense.minusDays(1)
    internal val redusertYtelseAlder = fødselsdato.plusYears(67)
    private val forhøyetInntektskravAlder = fødselsdato.plusYears(67)

    private companion object {
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59
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

    internal fun minimumInntekt(dato: LocalDate) = (if (forhøyetInntektskrav(dato)) Grunnbeløp.`2G` else Grunnbeløp.halvG).beløp(dato)

    // Forhøyet inntektskrav gjelder fra dagen _etter_ 67-årsdagen - se §8-51 andre ledd der det spesifiseres _mellom_.
    internal fun forhøyetInntektskrav(dato: LocalDate) = dato > forhøyetInntektskravAlder

    internal fun begrunnelseForMinimumInntekt(skjæringstidspunkt: LocalDate) =
        if (forhøyetInntektskrav(skjæringstidspunkt)) Begrunnelse.MinimumInntektOver67 else Begrunnelse.MinimumInntekt

    internal fun beregnFeriepenger(opptjeningsår: Year, beløp: Int): Double {
        val alderVedSluttenAvÅret = alderVedSluttenAvÅret(opptjeningsår)
        val prosentsats = if (alderVedSluttenAvÅret < ALDER_FOR_FORHØYET_FERIEPENGESATS) 0.102 else 0.125
        val feriepenger = beløp * prosentsats
        Aktivitetslogg().`§8-33 ledd 3`(beløp, opptjeningsår, prosentsats, alderVedSluttenAvÅret, feriepenger)
        return feriepenger
    }
}
