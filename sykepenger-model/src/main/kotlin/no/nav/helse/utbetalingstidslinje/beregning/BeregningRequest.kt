package no.nav.helse.utbetalingstidslinje.beregning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.sortedBy
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt

data class BeregningRequest(
    val yrkesaktiviteter: List<Beregningsperioder>
) {

    class Builder {
        private val yrkesaktiviteter = mutableSetOf<Yrkesaktivitet>()
        private val inntekter: MutableMap<Yrkesaktivitet, Inntekt> = mutableMapOf()
        private val inntektsjusteringer: MutableMap<Yrkesaktivitet, MutableList<Triple<LocalDate, LocalDate?, Inntekt>>> = mutableMapOf()
        private val vedtaksperioder: MutableMap<Yrkesaktivitet, MutableList<VedtaksperiodeForBeregning>> = mutableMapOf()

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

        fun vedtaksperiode(vedtaksperiodeId: UUID, periode: Periode, sykdomstidslinje: Sykdomstidslinje, dataForBeregning: VedtaksperiodeForBeregning.DataForBeregning) = apply {
            val yrkesaktivitet = dataForBeregning.yrkesaktivitet
            yrkesaktiviteter.add(yrkesaktivitet)
            vedtaksperioder.getOrPut(yrkesaktivitet) { mutableListOf() }.add(VedtaksperiodeForBeregning(vedtaksperiodeId, periode, sykdomstidslinje, dataForBeregning, null, Beløpstidslinje()))
        }

        /**
         * lager en oversikt over alle yrkesaktiviteter som skal beregnes sammen, enten
         * de har vedtaksperioder, er i sykepengegrunnlaget eller har inntektsjusteringer.
         */
        fun build(): BeregningRequest {
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
                    inntektsjusteringer
                        ?.filterIsInstance<Beløpsdag>()
                        ?.map { it.dato }
                        ?.grupperSammenhengendePerioder()
                        ?: emptyList()

                val andreInntektskilder = ghostOgAndreInntektskilderperioder
                    .flatMap { brytOppGhostperiode(it, vedtaksperioder) }
                    .map { it to (inntektsjusteringer?.subset(it) ?: Beløpstidslinje()) }
                    .map { (periode, inntektsjustering) ->
                        if (inntekt != null) Ghostperiode(periode, inntekt, inntektsjustering)
                        else AnnenInntektsperiode(periode, inntektsjustering)
                    }

                val perioder = (vedtaksperioder + andreInntektskilder).sortedBy { it.periode.start }
                Beregningsperioder(
                    yrkesaktivitet = yrkesaktivitet,
                    perioder = perioder
                )
            }
            return BeregningRequest(resultat)
        }

        private fun vedtaksperioder(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt?, inntektsjusteringer: Beløpstidslinje?): List<VedtaksperiodeForBeregning> {
            return (vedtaksperioder[yrkesaktivitet]?.toList() ?: emptyList()).map {
                it.copy(
                    inntekt = inntekt,
                    inntektsjusteringer = inntektsjusteringer?.subset(it.periode) ?: Beløpstidslinje()
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

        private fun brytOppGhostperiode(ghostperiode: Periode, vedtaksperioder: List<VedtaksperiodeForBeregning>): List<Periode> {
            return vedtaksperioder
                .sortedBy { it.periode.start }
                .fold(listOf(ghostperiode)) { resultat, vedtaksperiode ->
                    val siste = resultat.last()
                    val oppsplittet = siste.uten(vedtaksperiode.periode)
                    resultat.dropLast(1) + oppsplittet
                }
        }
    }

    data class Beregningsperioder(
        val yrkesaktivitet: Yrkesaktivitet,
        val perioder: List<Beregningsperiode>
    )

    sealed interface Beregningsperiode {
        val periode: Periode
    }

    // en periode som ikke dekkes av en vedtaksperiode,
    // hvor yrkesaktiviteten inngår i sykepengegrunnlaget
    data class Ghostperiode(
        override val periode: Periode,
        val inntekt: Inntekt,
        val inntektsjusteringer: Beløpstidslinje
    ) : Beregningsperiode

    // en periode som ikke dekkes av en vedtaksperiode
    // hvor yrkesaktiviteten ikke inngår i sykepengegrunnlaget
    data class AnnenInntektsperiode(
        override val periode: Periode,
        val inntektsjusteringer: Beløpstidslinje
    ) : Beregningsperiode

    data class VedtaksperiodeForBeregning(
        val vedtaksperiodeId: UUID,
        override val periode: Periode,
        val sykdomstidslinje: Sykdomstidslinje,
        val dataForBeregning: DataForBeregning,
        val inntekt: Inntekt?,
        val inntektsjusteringer: Beløpstidslinje
    ) : Beregningsperiode {
        sealed interface DataForBeregning {
            val yrkesaktivitet get() = when (this) {
                is Arbeidstaker -> Yrkesaktivitet.Arbeidstaker(organisasjonsnummer)
                is Selvstendig -> Yrkesaktivitet.Selvstendig
            }

            data class Arbeidstaker(
                val organisasjonsnummer: String,
                val arbeidsgiverperiode: List<Periode>,
                val dagerNavOvertarAnsvar: List<Periode>,
                val refusjonstidslinje: Beløpstidslinje
            ) : DataForBeregning
            data class Selvstendig(val ventetid: Periode): DataForBeregning
        }
    }
}
