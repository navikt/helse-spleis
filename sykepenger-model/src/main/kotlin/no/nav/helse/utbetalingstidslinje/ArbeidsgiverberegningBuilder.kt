package no.nav.helse.utbetalingstidslinje

import java.util.*
import no.nav.helse.erHelg
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi

internal class ArbeidsgiverberegningBuilder(private val beregningsperiode: Periode) {
    private val inntektskilder = mutableSetOf<Inntektskilde>()
    private val inntekter: MutableMap<Yrkesaktivitet, Inntekt> = mutableMapOf()
    private val inntektsjusteringer: MutableMap<Inntektskilde, Beløpstidslinje> = mutableMapOf()
    private val vedtaksperioder: MutableMap<Yrkesaktivitet, MutableList<UberegnetVedtaksperiode>> = mutableMapOf()
    private var sykepengegrunnlag: Inntekt = Inntekt.INGEN

    fun fastsattÅrsinntekt(yrkesaktivitet: Yrkesaktivitet.Arbeidstaker, inntekt: Inntekt) = apply {
        leggTilInntekt(yrkesaktivitet, inntekt)
    }

    fun selvstendigNæringsdrivende(inntekt: Inntekt) = apply {
        leggTilInntekt(Yrkesaktivitet.Selvstendig, inntekt)
    }

    fun sykepengegrunnlag(sykepengegrunnlag: Inntekt) = apply {
        this.sykepengegrunnlag = sykepengegrunnlag
    }

    private fun leggTilInntekt(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt) {
        inntektskilder.add(yrkesaktivitet)
        inntekter[yrkesaktivitet] = inntekt
    }

    private fun InntekterForBeregning.Inntektsperioder.beløpstidslinje() = inntektsperioder.fold(Beløpstidslinje()) { resultat, inntektsperiode ->
        val aktuellPeriode = inntektsperiode.periode.subset(beregningsperiode)

        val beløpstidslinje = when (inntektsperiode) {
            is InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag -> error("TODO: Hvis det viser seg at siste tolkning av andre ytelser fungerer så fjern meg, og slutt at build() retunrerer to ting")
            is InntekterForBeregning.Inntektsperiode.Beløp -> Beløpstidslinje.fra(aktuellPeriode, inntektsperiode.beløp, kilde)
        }
        resultat + beløpstidslinje
    }

    fun inntektsjusteringer(inntektskilde: Inntektskilde, inntektsperioder: InntekterForBeregning.Inntektsperioder) = apply {
        inntektskilder.add(inntektskilde)
        inntektsjusteringer[inntektskilde] = (inntektsjusteringer[inntektskilde] ?: Beløpstidslinje()) + inntektsperioder.beløpstidslinje()
    }

    fun vedtaksperiode(yrkesaktivitet: Yrkesaktivitet, vedtaksperiodeId: UUID, sykdomstidslinje: Sykdomstidslinje, builder: UtbetalingstidslinjeBuilder) = apply {
        val aktuellSykdomstidslinje = sykdomstidslinje.subset(beregningsperiode)
        val aktuellPeriode = checkNotNull(aktuellSykdomstidslinje.periode()) { "Sykdomstidslinjen kan ikke være tom" }

        inntektskilder.add(yrkesaktivitet)
        vedtaksperioder.getOrPut(yrkesaktivitet) { mutableListOf() }.add(UberegnetVedtaksperiode(vedtaksperiodeId, yrkesaktivitet, aktuellPeriode, aktuellSykdomstidslinje, builder))
    }

    private fun perioderMedInntektjustring(inntektskilde: Inntektskilde): List<Periode> {
        val inntektsjustering =  inntektsjusteringer[inntektskilde] ?: return emptyList()
        return inntektsjustering.filterIsInstance<Beløpsdag>().map(Beløpsdag::dato).grupperSammenhengendePerioder()
    }

    /**
     * lager en oversikt over alle yrkesaktiviteter som skal beregnes sammen, enten
     * de har vedtaksperioder, er i sykepengegrunnlaget eller har inntektsjusteringer.
     */
    fun build(): List<Arbeidsgiverberegning> {
        val resultat = inntektskilder.map { inntektskilde ->
            val inntektsjusteringer = inntektsjusteringer[inntektskilde] ?: Beløpstidslinje()
            val inntekt = inntekter[inntektskilde]
            val vedtaksperioder = when (inntektskilde) {
                is Yrkesaktivitet -> vedtaksperioder(inntektskilde, inntekt, inntektsjusteringer)
                else -> emptyList()
            }

            val ghostOgAndreInntektskilderperioder = if (inntekt != null)
                listOf(beregningsperiode)
            else
                perioderMedInntektjustring(inntektskilde)

            val andreInntektskilder = ghostOgAndreInntektskilderperioder
                .flatMap { brytOppGhostperiode(it, vedtaksperioder) }
                .map { it to (inntektsjusteringer.subset(it)) }
                .map { (periode, inntektsjustering) ->
                    when (inntekt) {
                        // tilkommet
                        null -> arbeidsdager(periode, inntektsjustering)
                        // ghost
                        else -> arbeidsdager(periode, inntektsjustering, inntekt)
                    }
                }

            Arbeidsgiverberegning(
                inntektskilde = inntektskilde,
                vedtaksperioder = vedtaksperioder,
                ghostOgAndreInntektskilder = andreInntektskilder
            )
        }
        return resultat
    }
    fun inntektsendringer()= inntektsjusteringer.toMap()

    private fun vedtaksperioder(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt?, inntektsjusteringer: Beløpstidslinje): List<Vedtaksperiodeberegning> {
        return (vedtaksperioder[yrkesaktivitet]?.toList() ?: emptyList()).map {
            Vedtaksperiodeberegning(
                vedtaksperiodeId = it.vedtaksperiodeId,
                utbetalingstidslinje = it.utbetalingstidslinjeBuilder.result(
                    sykdomstidslinje = it.sykdomstidslinje,
                    inntekt = inntekt ?: Inntekt.INGEN,
                    inntektjusteringer = inntektsjusteringer.subset(it.periode)
                )
            )
        }
    }

    private fun brytOppGhostperiode(ghostperiode: Periode, vedtaksperioder: List<Vedtaksperiodeberegning>): List<Periode> {
        return ghostperiode.uten(vedtaksperioder.map { it.periode })
    }

    private fun arbeidsdager(periodeMedArbeid: Periode, inntektsjusteringer: Beløpstidslinje, inntekt: Inntekt? = null): Utbetalingstidslinje {
        return with(Utbetalingstidslinje.Builder()) {
            periodeMedArbeid.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.ikkeBetalt(
                        aktuellDagsinntekt = inntekt ?: Inntekt.INGEN,
                        inntektjustering = (inntektsjusteringer[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN
                    ),
                )
            }
            build()
        }
    }

    private data class UberegnetVedtaksperiode(
        val vedtaksperiodeId: UUID,
        val yrkesaktivitet: Yrkesaktivitet,
        val periode: Periode,
        val sykdomstidslinje: Sykdomstidslinje,
        val utbetalingstidslinjeBuilder: UtbetalingstidslinjeBuilder
    )
}
