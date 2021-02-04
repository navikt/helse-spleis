package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    private val id: UUID = vedtaksperiode["id"]
    private val inntektsmeldingId:UUID? = vedtaksperiode["inntektsmeldingId"]
    private val skjæringstidspunktFraInfotrygd:LocalDate? = vedtaksperiode["skjæringstidspunktFraInfotrygd"]
    private val skjæringstidspunkt:LocalDate= vedtaksperiode["skjæringstidspunkt"]
    private val utbetalinger: List<UUID> = vedtaksperiode.get<List<Utbetaling>>("utbetalinger").map { UtbetalingReflect(it).toMap().getValue("id") as UUID }
    private val utbetalingstidslinje = UtbetalingstidslinjeReflect(vedtaksperiode.get<Utbetalingstidslinje>("utbetalingstidslinje")).toMap()
    private val tilstand = vedtaksperiode.get<Vedtaksperiode.Vedtaksperiodetilstand>("tilstand").type.name
    private val forlengelseFraInfotrygd: ForlengelseFraInfotrygd = vedtaksperiode["forlengelseFraInfotrygd"]
    private val opprettet = vedtaksperiode.get<LocalDateTime>("opprettet")
    private val oppdatert = vedtaksperiode.get<LocalDateTime>("oppdatert")
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
        "tilstand" to tilstand,
        "skjæringstidspunktFraInfotrygd" to skjæringstidspunktFraInfotrygd,
        "inntektsmeldingId" to inntektsmeldingId,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering,
        "dataForSimulering" to dataForSimulering,
        "utbetalinger" to utbetalinger,
        "utbetalingstidslinje" to utbetalingstidslinje,
        "forlengelseFraInfotrygd" to forlengelseFraInfotrygd,
        "opprettet" to opprettet,
        "oppdatert" to oppdatert
    )

    internal fun toSpeilMap(arbeidsgiver: Arbeidsgiver, periode: Periode) = mutableMapOf<String, Any?>(
        "id" to id,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "inntektsmeldingId" to inntektsmeldingId,
        "inntektFraInntektsmelding" to arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start)?.reflection{ _, månedlig, _, _ -> månedlig },
        "forlengelseFraInfotrygd" to forlengelseFraInfotrygd
    )
}
