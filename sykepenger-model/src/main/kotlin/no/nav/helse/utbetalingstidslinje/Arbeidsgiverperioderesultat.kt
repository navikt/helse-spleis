package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

data class Arbeidsgiverperioderesultat(
    // perioden som dekkes av arbeidsgiverperioden, fra første kjente dag til siste kjente dag
    val omsluttendePeriode: Periode,
    // dager som tolkes som del av arbeidsgiverperioden
    val arbeidsgiverperiode: Set<LocalDate>,
    // perioder hvor det er registrert utbetaling
    val utbetalingsperioder: Set<LocalDate>,
    // perioder hvor det er registrert oppholdsdager
    val oppholdsperioder: Set<LocalDate>,
    // hvorvidt arbeidsgiverperiodetellingen er komplett, aka. 16 dager.
    // hvis tellingen er fullstendig så er arbeidsgiverperiode.last() dag nr. 16.
    val fullstendig: Boolean,
    // oppholdsdag nr 16. som gjør at arbeidsgiverperioden er ferdig, og ny telling påstartes
    val sisteDag: LocalDate?
) {

    fun utvideMed(
        dato: LocalDate,
        arbeidsgiverperiode: LocalDate? = null,
        utbetalingsperiode: LocalDate? = null,
        oppholdsperiode: LocalDate? = null,
        fullstendig: Boolean? = null,
        sisteDag: LocalDate? = null
    ): Arbeidsgiverperioderesultat {
        return this.copy(
            omsluttendePeriode = this.omsluttendePeriode.oppdaterTom(dato),
            arbeidsgiverperiode = this.arbeidsgiverperiode.plus(setOfNotNull(arbeidsgiverperiode)),
            utbetalingsperioder = this.utbetalingsperioder.plus(setOfNotNull(utbetalingsperiode)),
            oppholdsperioder = this.oppholdsperioder.plus(setOfNotNull(oppholdsperiode)),
            fullstendig = fullstendig ?: this.fullstendig,
            sisteDag = sisteDag ?: this.sisteDag
        )
    }

    /**
     * subsummerer arbeidsgiverperioden, men bare dersom dagene overlapper med vedtaksperioden
     */
    internal fun subsummering(subsumsjonslogg: Subsumsjonslogg, sykdomstidslinje: Sykdomstidslinje) {
        if (arbeidsgiverperiode.isEmpty()) return // subsummerer ikke Infotrygd-ting

        val vedtaksperiode = sykdomstidslinje.periode() ?: return
        val sykdomstidslinjesubsumsjon = sykdomstidslinje.subsumsjonsformat()

        // første agp-dag i hver brudd-periode skal subsummeres med § 8-19 tredje ledd
        arbeidsgiverperiode.grupperSammenhengendePerioder().drop(1).forEach {
            if (it.start in vedtaksperiode) subsumsjonslogg.`§ 8-19 tredje ledd`(it.start, sykdomstidslinjesubsumsjon)
        }

        // siste 16. agp-dag skal subsummeres med § 8-19 første ledd
        if (fullstendig && arbeidsgiverperiode.last() in vedtaksperiode)
            subsumsjonslogg.`§ 8-19 første ledd`(arbeidsgiverperiode.last(), sykdomstidslinjesubsumsjon)

        // første utbetalingsdag skal subsummeres med § 8-17 ledd 1 bokstav a (oppfylt = true)
        utbetalingsperioder.firstOrNull { !it.erHelg() }?.takeIf { it in vedtaksperiode }?.also {
            subsumsjonslogg.`§ 8-17 ledd 1 bokstav a`(oppfylt = true, dagen = it, sykdomstidslinjesubsumsjon)
        }

        // siste oppholdsdag som medfører at agp avbrytes subsummeres med § 8-19 fjerde ledd
        if (fullstendig && sisteDag != null && sisteDag in vedtaksperiode)
            subsumsjonslogg.`§ 8-19 fjerde ledd`(sisteDag, sykdomstidslinjesubsumsjon)
    }

    internal fun somArbeidsgiverperiode(): Arbeidsgiverperiode {
        val agp =
            if (arbeidsgiverperiode.isEmpty() && utbetalingsperioder.isNotEmpty())
                Arbeidsgiverperiode.fiktiv(utbetalingsperioder.first())
            else
                Arbeidsgiverperiode(arbeidsgiverperiode.grupperSammenhengendePerioder())
        utbetalingsperioder.forEach { agp.utbetalingsdag(it) }
        oppholdsperioder.forEach { agp.oppholdsdag(it) }
        omsluttendePeriode.forEach { agp.kjentDag(it) }
        return agp
    }

    companion object {
        internal fun Iterable<Arbeidsgiverperioderesultat>.finn(periode: Periode) = lastOrNull { arbeidsgiverperiode ->
            periode.overlapperMed(arbeidsgiverperiode.omsluttendePeriode)
        }
    }
}