package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    internal val id: UUID = vedtaksperiode["id"]
    private val maksdato: LocalDate? = vedtaksperiode["maksdato"]
    private val gjenståendeSykedager: Int? = vedtaksperiode["gjenståendeSykedager"]
    private val forbrukteSykedager: Int? = vedtaksperiode["forbrukteSykedager"]
    private val godkjentAv: String? = vedtaksperiode["godkjentAv"]
    private val godkjenttidspunkt: LocalDateTime? = vedtaksperiode["godkjenttidspunkt"]
    private val automatiskBehandling: Boolean? = vedtaksperiode["automatiskBehandling"]
    private val beregningsdatoFraInfotrygd:LocalDate? = vedtaksperiode["beregningsdatoFraInfotrygd"]
    private val beregningsdato:LocalDate= vedtaksperiode["beregningsdato"]
    internal val personFagsystemId: String? = vedtaksperiode["personFagsystemId"]
    private val personNettoBeløp: Int = vedtaksperiode["personNettoBeløp"]
    internal val arbeidsgiverFagsystemId: String? = vedtaksperiode["arbeidsgiverFagsystemId"]
    private val arbeidsgiverNettoBeløp: Int = vedtaksperiode["arbeidsgiverNettoBeløp"]
    private val forlengelseFraInfotrygd: ForlengelseFraInfotrygd = vedtaksperiode["forlengelseFraInfotrygd"]
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

    private val dataForVilkårsvurdering: Map<String, Any?>? = vedtaksperiode.get<Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")?.let {
        mapOf(
            "erEgenAnsatt" to it.erEgenAnsatt,
            "beregnetÅrsinntektFraInntektskomponenten" to
                it.beregnetÅrsinntektFraInntektskomponenten.reflection { årlig, _, _, _ -> årlig },
            "avviksprosent" to it.avviksprosent?.ratio(),
            "antallOpptjeningsdagerErMinst" to it.antallOpptjeningsdagerErMinst,
            "harOpptjening" to it.harOpptjening,
            "medlemskapstatus" to when (it.medlemskapstatus) {
                no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Ja -> no.nav.helse.serde.mapping.JsonMedlemskapstatus.JA
                no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Nei -> no.nav.helse.serde.mapping.JsonMedlemskapstatus.NEI
                else -> no.nav.helse.serde.mapping.JsonMedlemskapstatus.VET_IKKE
            }
        )
    }

    internal fun toMap() = mutableMapOf(
        "id" to id,
        "maksdato" to maksdato,
        "gjenståendeSykedager" to gjenståendeSykedager,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "automatiskBehandling" to automatiskBehandling,
        "beregningsdatoFraInfotrygd" to beregningsdatoFraInfotrygd,
        "beregningsdato" to beregningsdato,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering,
        "dataForSimulering" to dataForSimulering,
        "personFagsystemId" to personFagsystemId,
        "personNettoBeløp" to personNettoBeløp,
        "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
        "arbeidsgiverNettoBeløp" to arbeidsgiverNettoBeløp,
        "forlengelseFraInfotrygd" to forlengelseFraInfotrygd
    )

    internal fun toSpeilMap(arbeidsgiver: Arbeidsgiver) = mutableMapOf<String, Any?>(
        "id" to id,
        "maksdato" to maksdato,
        "gjenståendeSykedager" to gjenståendeSykedager,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "automatiskBehandling" to automatiskBehandling,
        "beregningsdatoFraInfotrygd" to beregningsdatoFraInfotrygd,
        "beregningsdato" to beregningsdato,
        "inntektFraInntektsmelding" to arbeidsgiver.inntekt(beregningsdato)?.get<Double>("årlig")?.div(12.0),
        "forlengelseFraInfotrygd" to forlengelseFraInfotrygd
    )
}
