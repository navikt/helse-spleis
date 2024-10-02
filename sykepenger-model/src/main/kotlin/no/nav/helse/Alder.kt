package no.nav.helse

import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit.YEARS
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.til
import no.nav.helse.dto.AlderDto
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class Alder(val fødselsdato: LocalDate, val dødsdato: LocalDate?) {
    internal val syttiårsdagen: LocalDate = fødselsdato.plusYears(70)
    internal val redusertYtelseAlder: LocalDate = fødselsdato.plusYears(67)
    private val forhøyetInntektskravAlder: LocalDate = fødselsdato.plusYears(67)

    companion object {
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59
        private const val MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE = 18

        internal fun gjenopprett(dto: AlderDto): Alder {
            return Alder(
                fødselsdato = dto.fødselsdato,
                dødsdato = dto.dødsdato
            )
        }

        val LocalDate.alder get() = Alder(this, null)
    }

    internal fun medDød(dødsdato: LocalDate): Alder {
        if (this.dødsdato != null) return this
        return Alder(this.fødselsdato, dødsdato)
    }

    internal fun innenfor67årsgrense(dato: LocalDate) = dato <= redusertYtelseAlder

    internal fun mistetSykepengerett(dato: LocalDate) = dato >= syttiårsdagen

    internal fun alderPåDato(dato: LocalDate): Int {
        val dagen = if (dødsdato != null) minOf(dødsdato, dato) else dato
        return YEARS.between(fødselsdato, dagen).toInt()
    }

    private fun alderVedSluttenAvÅret(year: Year) = YEARS.between(Year.from(fødselsdato), year).toInt()

    // Forhøyet inntektskrav gjelder fra dagen _etter_ 67-årsdagen - se § 8-51 andre ledd der det spesifiseres _mellom_.
    internal fun forhøyetInntektskrav(dato: LocalDate) = dato > forhøyetInntektskravAlder

    internal fun begrunnelseForMinimumInntekt(skjæringstidspunkt: LocalDate) =
        if (forhøyetInntektskrav(skjæringstidspunkt)) Begrunnelse.MinimumInntektOver67 else Begrunnelse.MinimumInntekt

    internal fun forUngForÅSøke(søknadstidspunkt: LocalDate) = alderPåDato(søknadstidspunkt) < MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE

    internal fun beregnFeriepenger(opptjeningsår: Year, beløp: Int): Double {
        val alderVedSluttenAvÅret = alderVedSluttenAvÅret(opptjeningsår)
        val prosentsats = if (alderVedSluttenAvÅret < ALDER_FOR_FORHØYET_FERIEPENGESATS) 0.102 else 0.125
        val feriepenger = beløp * prosentsats
        //TODO: subsumsjonObserver.`§8-33 ledd 3`(beløp, opptjeningsår, prosentsats, alderVedSluttenAvÅret, feriepenger)
        return feriepenger
    }

    internal fun fraOgMedFylte67(
        oppfylt: Boolean,
        utfallFom: LocalDate,
        utfallTom: LocalDate,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        beregningsgrunnlagÅrlig: Double,
        minimumInntektÅrlig: Double,
        jurist: Subsumsjonslogg
    ) {
        jurist.logg(`§ 8-51 ledd 2`(
            oppfylt = oppfylt,
            utfallFom = utfallFom,
            utfallTom = utfallTom,
            sekstisyvårsdag = redusertYtelseAlder,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            beregningsgrunnlagÅrlig = beregningsgrunnlagÅrlig,
            minimumInntektÅrlig = minimumInntektÅrlig
        ))
    }

    internal fun avvisDager(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
        if (dødsdato == null) return tidslinjer
        return Utbetalingstidslinje.avvis(tidslinjer, listOf(dødsdato.nesteDag til LocalDate.MAX), listOf(Begrunnelse.EtterDødsdato))
    }

    internal fun dto() = AlderDto(fødselsdato = fødselsdato, dødsdato = dødsdato)
}
