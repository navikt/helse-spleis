package no.nav.helse.serde.api

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.TilstandType
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun mapTilstander(tilstand: TilstandType, utbetalt: Boolean) = when (tilstand) {
    TilstandType.START,
    TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
    TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
    TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
    TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
    TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
    TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP,
    TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
    TilstandType.AVVENTER_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
    TilstandType.AVVENTER_UFERDIG_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_HISTORIKK,
    TilstandType.AVVENTER_SIMULERING,
    TilstandType.TIL_INFOTRYGD -> TilstandstypeDTO.Venter
    TilstandType.UTBETALING_FEILET -> TilstandstypeDTO.Feilet
    TilstandType.TIL_UTBETALING -> TilstandstypeDTO.TilUtbetaling
    TilstandType.AVVENTER_GODKJENNING -> TilstandstypeDTO.Oppgaver
    TilstandType.AVSLUTTET -> if (utbetalt) TilstandstypeDTO.Utbetalt else TilstandstypeDTO.IngenUtbetaling
    TilstandType.AVSLUTTET_UTEN_UTBETALING -> TilstandstypeDTO.IngenUtbetaling
}

internal fun mapBegrunnelse(begrunnelse: Begrunnelse) = BegrunnelseDTO.valueOf(begrunnelse.name)

internal fun MutableMap<String, Any?>.mapTilVedtaksperiodeDto(): VedtaksperiodeDTO {
    val sykdomstidslinje = this["sykdomstidslinje"] as MutableList<SykdomstidslinjedagDTO>
    return VedtaksperiodeDTO(
        id = this["id"] as UUID,
        fom = sykdomstidslinje.firstOrNull()?.dagen,
        tom = sykdomstidslinje.lastOrNull()?.dagen,
        maksdato = this["maksdato"] as LocalDate?,
        forbrukteSykedager = this["forbrukteSykedager"] as Int?,
        godkjentAv = this["godkjentAv"] as String?,
        godkjenttidspunkt = this["godkjenttidspunkt"] as LocalDateTime?,
        sykdomstidslinje = sykdomstidslinje,
        utbetalingsreferanse = this["utbetalingsreferanse"] as String?,
        førsteFraværsdag = this["førsteFraværsdag"] as LocalDate?,
        inntektFraInntektsmelding = this["inntektFraInntektsmelding"] as Double?,
        totalbeløpArbeidstaker = this["totalbeløpArbeidstaker"] as Double,
        tilstand = this["tilstand"] as TilstandstypeDTO,
        hendelser = this["hendelser"] as MutableSet<UUID>,
        dataForVilkårsvurdering = this["dataForVilkårsvurdering"]?.let { it as GrunnlagsdataDTO },
        utbetalingslinjer = this["utbetalingslinjer"] as MutableList<UtbetalingslinjeDTO>,
        utbetalingstidslinje = this["utbetalingstidslinje"] as MutableList<UtbetalingstidslinjedagDTO>
    )
}

internal fun mapDataForVilkårsvurdering(grunnlagsdata: Vilkårsgrunnlag.Grunnlagsdata) = GrunnlagsdataDTO(
    erEgenAnsatt = grunnlagsdata.erEgenAnsatt,
    beregnetÅrsinntektFraInntektskomponenten = grunnlagsdata.beregnetÅrsinntektFraInntektskomponenten,
    avviksprosent = grunnlagsdata.avviksprosent,
    antallOpptjeningsdagerErMinst = grunnlagsdata.antallOpptjeningsdagerErMinst,
    harOpptjening = grunnlagsdata.harOpptjening
)
