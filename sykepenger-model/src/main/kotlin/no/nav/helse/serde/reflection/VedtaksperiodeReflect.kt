package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    internal val id: UUID = vedtaksperiode["id"]
    private val gruppeId: UUID = vedtaksperiode["gruppeId"]
    private val aktørId: String = vedtaksperiode["aktørId"]
    private val fødselsnummer: String = vedtaksperiode["fødselsnummer"]
    private val organisasjonsnummer: String = vedtaksperiode["organisasjonsnummer"]
    private val maksdato: LocalDate? = vedtaksperiode["maksdato"]
    private val forbrukteSykedager: Int? = vedtaksperiode["forbrukteSykedager"]
    private val godkjentAv: String? = vedtaksperiode["godkjentAv"]
    private val godkjenttidspunkt: LocalDateTime? = vedtaksperiode["godkjenttidspunkt"]
    private val førsteFraværsdag:LocalDate? = vedtaksperiode["førsteFraværsdag"]
    private val dataForSimulering: Map<String, Any>? = vedtaksperiode.get<Simulering.SimuleringResultat?>("dataForSimulering")?.let {
        mapOf(
            "totalbeløp" to it.totalbeløp,
            "perioder" to it.perioder.map { periode ->
                mapOf(
                    "fom" to periode.periode.start,
                    "tom" to periode.periode.endInclusive,
                    "utbetalinger" to periode.utbetalinger.map { utbetaling ->
                        mapOf(
                            "forfallsdato" to utbetaling.forfallsdato,
                            "utbetalesTil" to mapOf(
                                "id" to utbetaling.utbetalesTil.id,
                                "navn" to utbetaling.utbetalesTil.navn
                            ),
                            "feilkonto" to utbetaling.feilkonto,
                            "detaljer" to utbetaling.detaljer.map { detalj ->
                                mapOf(
                                    "fom" to detalj.periode.start,
                                    "tom" to detalj.periode.endInclusive,
                                    "konto" to detalj.konto,
                                    "beløp" to detalj.beløp,
                                    "klassekode" to mapOf(
                                        "kode" to detalj.klassekode.kode,
                                        "beskrivelse" to detalj.klassekode.beskrivelse
                                    ),
                                    "uføregrad" to detalj.uføregrad,
                                    "utbetalingstype" to detalj.utbetalingstype,
                                    "tilbakeføring" to detalj.tilbakeføring,
                                    "sats" to mapOf(
                                        "sats" to detalj.sats.sats,
                                        "antall" to detalj.sats.antall,
                                        "type" to detalj.sats.type
                                    ),
                                    "refunderesOrgnummer" to detalj.refunderesOrgnummer
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private val dataForVilkårsvurdering: Map<String, Any>? = vedtaksperiode.get<Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")?.let {
        mapOf(
            "erEgenAnsatt" to it.erEgenAnsatt,
            "beregnetÅrsinntektFraInntektskomponenten" to it.beregnetÅrsinntektFraInntektskomponenten,
            "avviksprosent" to it.avviksprosent,
            "antallOpptjeningsdagerErMinst" to it.antallOpptjeningsdagerErMinst,
            "harOpptjening" to it.harOpptjening
        )
    }

    internal fun toMap() = mutableMapOf(
        "id" to id,
        "gruppeId" to gruppeId,
        "maksdato" to maksdato,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "førsteFraværsdag" to førsteFraværsdag,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering,
        "dataForSimulering" to dataForSimulering
    )

    internal fun toSpeilMap(arbeidsgiver: Arbeidsgiver) = mutableMapOf<String, Any?>(
        "id" to id,
        "gruppeId" to gruppeId,
        "maksdato" to maksdato,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "førsteFraværsdag" to førsteFraværsdag,
        "inntektFraInntektsmelding" to førsteFraværsdag?.let { arbeidsgiver.inntekt(it)?.toDouble() }
    )
}
