package no.nav.helse.utbetalingstidslinje.beregning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.sortedBy
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
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
        private val vedtaksperioder: MutableMap<Yrkesaktivitet, MutableList<Triple<UUID, Sykdomstidslinje, VedtaksperiodeForBeregning.DataForBeregning>>> = mutableMapOf()

        fun fastsattÅrsinntekt(yrkesaktivitet: Yrkesaktivitet.Arbeidstaker, inntekt: Inntekt) {
            leggTilInntekt(yrkesaktivitet, inntekt)
        }

        fun selvstendigNæringsdrivende(inntekt: Inntekt) {
            val yrkesaktivitet = Yrkesaktivitet.Selvstendig
            leggTilInntekt(yrkesaktivitet, inntekt)
        }

        private fun leggTilInntekt(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt) {
            yrkesaktiviteter.add(yrkesaktivitet)
            inntekter[yrkesaktivitet] = inntekt
        }

        fun inntektsjusteringer(yrkesaktivitet: Yrkesaktivitet, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt) {
            yrkesaktiviteter.add(yrkesaktivitet)
            inntektsjusteringer.getOrPut(yrkesaktivitet) { mutableListOf() }.add(Triple(fom, tom, inntekt))
        }

        fun vedtaksperiode(vedtaksperiodeId: UUID, sykdomstidslinje: Sykdomstidslinje, dataForBeregning: VedtaksperiodeForBeregning.DataForBeregning) {
            val yrkesaktivitet = dataForBeregning.yrkesaktivitet
            yrkesaktiviteter.add(yrkesaktivitet)
            vedtaksperioder.getOrPut(yrkesaktivitet) { mutableListOf() }.add(Triple(vedtaksperiodeId, sykdomstidslinje, dataForBeregning))
        }

        /**
         * lager en oversikt over alle yrkesaktiviteter som skal beregnes sammen, enten
         * de har vedtaksperioder, er i sykepengegrunnlaget eller har inntektsjusteringer.
         */
        fun build(): BeregningRequest {
            val beregningsperiode = vedtaksperioder
                .flatMap { it.value }
                .map { it.second.periode()!! }
                .reduce(Periode::plus)

            val resultat = yrkesaktiviteter.map { yrkesaktivitet ->
                val inntektsjusteringer = inntektsjusteringer(yrkesaktivitet, beregningsperiode)
                val inntekt = inntekter[yrkesaktivitet]
                val vedtaksperioder = vedtaksperioder(yrkesaktivitet, inntekt, inntektsjusteringer)
                val perioderUtenVedtak = brytOppGhostperiode(beregningsperiode, vedtaksperioder).map {
                    it to inntektsjusteringer.subset(it)
                }
                val andreInntektskilder = perioderUtenVedtak.map { (periode, inntektsjustering) ->
                    if (inntekt == null) AnnenInntektsperiode(periode, inntektsjustering)
                    else Ghostperiode(periode, inntekt, inntektsjustering)
                }

                val perioder = (vedtaksperioder + andreInntektskilder).sortedBy { it.periode.start }
                Beregningsperioder(
                    yrkesaktivitet = yrkesaktivitet,
                    perioder = perioder
                )
            }
            return BeregningRequest(resultat)
        }

        private fun vedtaksperioder(yrkesaktivitet: Yrkesaktivitet, inntekt: Inntekt?, inntektsjusteringer: Beløpstidslinje): List<VedtaksperiodeForBeregning> {
            return (vedtaksperioder[yrkesaktivitet]?.toList() ?: emptyList()).map {
                VedtaksperiodeForBeregning(
                    vedtaksperiodeId = it.first,
                    sykdomstidslinje = it.second,
                    dataForBeregning = it.third,
                    inntekt = inntekt,
                    inntektsjusteringer = inntektsjusteringer.subset(it.second.periode()!!)
                )
            }
        }

        private fun inntektsjusteringer(yrkesaktivitet: Yrkesaktivitet, beregningsperiode: Periode): Beløpstidslinje {
            return this.inntektsjusteringer[yrkesaktivitet]?.fold(Beløpstidslinje()) { resultat, (fom, tom, inntekt) ->
                // beløpstidslinje er en lukket tidslinje som ikke støtter åpen ende, så vi begrenser "null" til siste beregningsdato
                val reellPeriode = fom.til(tom ?: beregningsperiode.endInclusive)
                val kilde = Kilde(MeldingsreferanseId( UUID.randomUUID()), SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
                resultat.erstatt(Beløpstidslinje.fra(reellPeriode, inntekt, kilde))
            } ?: Beløpstidslinje()
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
        val sykdomstidslinje: Sykdomstidslinje,
        val dataForBeregning: DataForBeregning,
        val inntekt: Inntekt?,
        val inntektsjusteringer: Beløpstidslinje
    ) : Beregningsperiode {
        override val periode = checkNotNull(sykdomstidslinje.periode()) { "sykdomstidslinjen er tom" }

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
