package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Yrkesaktivitet
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi

internal class ArbeidsgiverberegningBuilder {
    private val yrkesaktiviteter = mutableSetOf<Yrkesaktivitet>()
    private val inntekter: MutableMap<Yrkesaktivitet, Inntekt> = mutableMapOf()
    private val inntektsjusteringer: MutableMap<Yrkesaktivitet, MutableList<Triple<LocalDate, LocalDate?, Inntekt>>> = mutableMapOf()
    private val vedtaksperioder: MutableMap<Yrkesaktivitet, MutableList<UberegnetVedtaksperiode>> = mutableMapOf()

    fun fastsattÅrsinntekt(yrkesaktivitet: Yrkesaktivitet.Arbeidstaker, inntekt: Inntekt) = apply {
        leggTilInntekt(yrkesaktivitet, inntekt)
    }

    fun selvstendigNæringsdrivende(inntekt: Inntekt) = apply {
        val yrkesaktivitet = Yrkesaktivitet.Selvstendig
        leggTilInntekt(yrkesaktivitet, inntekt)
    }

    private fun leggTilInntekt(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt) {
        yrkesaktiviteter.add(yrkesaktivitet)
        inntekter[yrkesaktivitet] = inntekt
    }

    fun inntektsjusteringer(yrkesaktivitet: Yrkesaktivitet, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt) = apply {
        yrkesaktiviteter.add(yrkesaktivitet)
        inntektsjusteringer.getOrPut(yrkesaktivitet) { mutableListOf() }.add(Triple(fom, tom, inntekt))
    }

    fun vedtaksperiode(yrkesaktivitet: Yrkesaktivitet, vedtaksperiodeId: UUID, sykdomstidslinje: Sykdomstidslinje, builder: UtbetalingstidslinjeBuilder) = apply {
        val periode = checkNotNull(sykdomstidslinje.periode()) { "Sykdomstidslinjen kan ikke være tom" }
        yrkesaktiviteter.add(yrkesaktivitet)
        vedtaksperioder.getOrPut(yrkesaktivitet) { mutableListOf() }.add(UberegnetVedtaksperiode(vedtaksperiodeId, yrkesaktivitet, periode, sykdomstidslinje, builder))
    }

    /**
     * lager en oversikt over alle yrkesaktiviteter som skal beregnes sammen, enten
     * de har vedtaksperioder, er i sykepengegrunnlaget eller har inntektsjusteringer.
     */
    fun build(): List<Arbeidsgiverberegning> {
        val beregningsperiode = vedtaksperioder
            .flatMap { it.value }
            .map { it.periode }
            .reduce(Periode::plus)

        val resultat = yrkesaktiviteter.map { yrkesaktivitet ->
            val inntektsjusteringer = inntektsjusteringer(yrkesaktivitet, beregningsperiode)
            val inntekt = inntekter[yrkesaktivitet]
            val vedtaksperioder = vedtaksperioder(yrkesaktivitet, inntekt, inntektsjusteringer)

            val ghostOgAndreInntektskilderperioder = if (inntekt != null)
                listOf(beregningsperiode)
            else
                this.inntektsjusteringer[yrkesaktivitet]
                    ?.map { (fom, tom, _) -> fom til (listOfNotNull(tom, beregningsperiode.endInclusive).min()) }
                    ?: emptyList()

            val andreInntektskilder = ghostOgAndreInntektskilderperioder
                .flatMap { brytOppGhostperiode(it, vedtaksperioder) }
                .map { it to (inntektsjusteringer?.subset(it) ?: Beløpstidslinje()) }
                .map { (periode, inntektsjustering) ->
                    when (inntekt) {
                        // tilkommet
                        null -> arbeidsdager(periode, inntektsjustering)
                        // ghost
                        else -> arbeidsdager(periode, inntektsjustering, inntekt)
                    }
                }

            Arbeidsgiverberegning(
                yrkesaktivitet = yrkesaktivitet,
                vedtaksperioder = vedtaksperioder,
                ghostOgAndreInntektskilder = andreInntektskilder
            )
        }
        return resultat
    }

    private fun vedtaksperioder(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt?, inntektsjusteringer: Beløpstidslinje?): List<Vedtaksperiodeberegning> {
        return (vedtaksperioder[yrkesaktivitet]?.toList() ?: emptyList()).map {
            Vedtaksperiodeberegning(
                vedtaksperiodeId = it.vedtaksperiodeId,
                utbetalingstidslinje = it.utbetalingstidslinjeBuilder.result(
                    sykdomstidslinje = it.sykdomstidslinje,
                    inntekt = inntekt ?: Inntekt.INGEN,
                    inntektjusteringer = inntektsjusteringer?.subset(it.periode) ?: Beløpstidslinje()
                )
            )
        }
    }

    private fun inntektsjusteringer(yrkesaktivitet: Yrkesaktivitet, beregningsperiode: Periode): Beløpstidslinje? {
        return this.inntektsjusteringer[yrkesaktivitet]?.fold(Beløpstidslinje()) { resultat, (fom, tom, inntekt) ->
            // beløpstidslinje er en lukket tidslinje som ikke støtter åpen ende, så vi begrenser "null" til siste beregningsdato
            val reellPeriode = fom.til(tom ?: beregningsperiode.endInclusive)
            val kilde = Kilde(MeldingsreferanseId( UUID.randomUUID()), SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
            resultat.erstatt(Beløpstidslinje.fra(reellPeriode, inntekt, kilde))
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
