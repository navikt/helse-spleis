package no.nav.helse.serde.api

import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.TilstandType
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.utbetalingstidslinje.Alder as SpleisAlder

internal fun mapTilstander(tilstand: TilstandType, utbetalt: Boolean) = when (tilstand) {
    TilstandType.START,
    TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
    TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
    TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
    TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
    TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
    TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP,
    TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
    TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
    TilstandType.AVVENTER_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
    TilstandType.AVVENTER_UFERDIG_GAP,
    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
    TilstandType.AVVENTER_SIMULERING,
    TilstandType.AVVENTER_HISTORIKK -> TilstandstypeDTO.Venter
    TilstandType.TIL_INFOTRYGD -> TilstandstypeDTO.TilInfotrygd
    TilstandType.UTBETALING_FEILET -> TilstandstypeDTO.Feilet
    TilstandType.TIL_UTBETALING -> TilstandstypeDTO.TilUtbetaling
    TilstandType.AVVENTER_GODKJENNING -> TilstandstypeDTO.Oppgaver
    TilstandType.AVSLUTTET -> if (utbetalt) TilstandstypeDTO.Utbetalt else TilstandstypeDTO.IngenUtbetaling
    TilstandType.AVSLUTTET_UTEN_UTBETALING -> TilstandstypeDTO.IngenUtbetaling
    TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING -> TilstandstypeDTO.IngenUtbetaling
}

internal fun mapBegrunnelse(begrunnelse: Begrunnelse) = BegrunnelseDTO.valueOf(begrunnelse.name)

internal fun MutableMap<String, Any?>.mapTilPersonDto() = PersonDTO(
    fødselsnummer = this["fødselsnummer"] as String,
    aktørId = this["aktørId"] as String,
    arbeidsgivere = this["arbeidsgivere"] as List<ArbeidsgiverDTO>
)

internal fun MutableMap<String, Any?>.mapTilArbeidsgiverDto() = ArbeidsgiverDTO(
    organisasjonsnummer = this["organisasjonsnummer"] as String,
    id = this["id"] as UUID,
    vedtaksperioder = this["vedtaksperioder"] as List<VedtaksperiodeDTO>
)

internal fun MutableMap<String, Any?>.mapTilVedtaksperiodeDto(
    fødselsnummer: String,
    inntekter: List<Inntekthistorikk.Inntekt>
): VedtaksperiodeDTO {
    val sykdomstidslinje = this["sykdomstidslinje"] as List<SykdomstidslinjedagDTO>
    val utbetalingslinjer = this["utbetalingslinjer"] as List<UtbetalingslinjeDTO>
    val dataForVilkårsvurdering = this["dataForVilkårsvurdering"]?.let { it as GrunnlagsdataDTO }
    val hendelser = this["hendelser"] as List<HendelseDTO>
    val søknad = hendelser.find { it.type == "SENDT_SØKNAD_NAV" }!! as SøknadDTO
    val vilkår = mapVilkår(
        this,
        utbetalingslinjer,
        sykdomstidslinje,
        fødselsnummer,
        dataForVilkårsvurdering,
        søknad,
        inntekter
    )
    return VedtaksperiodeDTO(
        id = this["id"] as UUID,
        fom = sykdomstidslinje.first().dagen,
        tom = sykdomstidslinje.last().dagen,
        tilstand = this["tilstand"] as TilstandstypeDTO,
        fullstendig = true,
        utbetalingsreferanse = this["utbetalingsreferanse"] as String?,
        utbetalingstidslinje = this["utbetalingstidslinje"] as List<UtbetalingstidslinjedagDTO>,
        godkjentAv = this["godkjentAv"] as String?,
        vilkår = vilkår,
        godkjenttidspunkt = this["godkjenttidspunkt"] as LocalDateTime?,
        sykdomstidslinje = sykdomstidslinje,
        førsteFraværsdag = this["førsteFraværsdag"] as LocalDate,
        inntektFraInntektsmelding = this["inntektFraInntektsmelding"] as Double,
        totalbeløpArbeidstaker = this["totalbeløpArbeidstaker"] as Int,
        hendelser = hendelser,
        dataForVilkårsvurdering = dataForVilkårsvurdering,
        utbetalingslinjer = utbetalingslinjer
    )
}

internal fun MutableMap<String, Any?>.mapTilUfullstendigVedtaksperiodeDto(): UfullstendigVedtaksperiodeDTO {
    val sykdomstidslinje = this["sykdomstidslinje"] as List<SykdomstidslinjedagDTO>
    return UfullstendigVedtaksperiodeDTO(
        id = this["id"] as UUID,
        fom = sykdomstidslinje.first().dagen,
        tom = sykdomstidslinje.last().dagen,
        tilstand = this["tilstand"] as TilstandstypeDTO,
        fullstendig = false
    )
}

internal fun mapDataForVilkårsvurdering(grunnlagsdata: Vilkårsgrunnlag.Grunnlagsdata) = GrunnlagsdataDTO(
    erEgenAnsatt = grunnlagsdata.erEgenAnsatt,
    beregnetÅrsinntektFraInntektskomponenten = grunnlagsdata.beregnetÅrsinntektFraInntektskomponenten,
    avviksprosent = grunnlagsdata.avviksprosent,
    antallOpptjeningsdagerErMinst = grunnlagsdata.antallOpptjeningsdagerErMinst,
    harOpptjening = grunnlagsdata.harOpptjening
)

internal fun mapVilkår(
    vedtaksperiodeMap: Map<String, Any?>,
    utbetalingslinjer: List<UtbetalingslinjeDTO>,
    sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    fødselsnummer: String,
    dataForVilkårsvurdering: GrunnlagsdataDTO?,
    søknad: SøknadDTO,
    inntekter: List<Inntekthistorikk.Inntekt>
): Vilkår {
    val førsteFraværsdag = vedtaksperiodeMap["førsteFraværsdag"] as LocalDate
    val beregnetMånedsinntekt =
        (Inntekthistorikk.Inntekt.inntekt(inntekter, førsteFraværsdag) ?: inntekter.firstOrNull()?.beløp)?.toDouble()

    val førsteSykepengedag = utbetalingslinjer.map { it.fom }.min()
    val sisteSykepengedagEllerSisteDagIPerioden =
        utbetalingslinjer.map { it.tom }.max() ?: sykdomstidslinje.last().dagen
    val personalder = SpleisAlder(fødselsnummer)
    val forbrukteSykedager = vedtaksperiodeMap["forbrukteSykedager"] as Int?
    val over67 = personalder.redusertYtelseAlder.isBefore(sisteSykepengedagEllerSisteDagIPerioden)
    val maksdato = vedtaksperiodeMap["maksdato"] as LocalDate?
    val sykepengedager = Sykepengedager(
        forbrukteSykedager = forbrukteSykedager,
        førsteFraværsdag = førsteFraværsdag,
        førsteSykepengedag = førsteSykepengedag,
        maksdato = maksdato,
        oppfylt = ikkeOppbruktSykepengedager(maksdato, sisteSykepengedagEllerSisteDagIPerioden)
    )
    val alderSisteSykepengedag = personalder.alderPåDato(sisteSykepengedagEllerSisteDagIPerioden)
    val alder = Alder(
        alderSisteSykedag = alderSisteSykepengedag,
        oppfylt = personalder.øvreAldersgrense.isAfter(sisteSykepengedagEllerSisteDagIPerioden)
    )
    val opptjening = dataForVilkårsvurdering?.let {
        Opptjening(
            antallKjenteOpptjeningsdager = it.antallOpptjeningsdagerErMinst,
            fom = førsteFraværsdag.minusDays(it.antallOpptjeningsdagerErMinst.toLong()),
            oppfylt = it.harOpptjening
        )
    }
    val søknadsfrist = Søknadsfrist(
        sendtNav = søknad.sendtNav,
        søknadFom = søknad.fom,
        søknadTom = søknad.tom,
        oppfylt = søknadsfristOppfylt(søknad)
    )
    val sykepengegrunnlag = Sykepengegrunnlag(
        sykepengegrunnlag = beregnetMånedsinntekt?.times(12),
        grunnbeløp = `1G`.beløp(førsteFraværsdag).toInt(),
        oppfylt = sykepengegrunnlagOppfylt(
            over67 = over67,
            beregnetMånedsinntekt = beregnetMånedsinntekt,
            førsteFraværsdag = førsteFraværsdag
        )
    )
    return Vilkår(sykepengedager, alder, opptjening, søknadsfrist, sykepengegrunnlag)
}

private fun sykepengegrunnlagOppfylt(
    over67: Boolean,
    beregnetMånedsinntekt: Double?,
    førsteFraværsdag: LocalDate
) = beregnetMånedsinntekt?.times(12)?.let {
    if (over67) it > `2G`.beløp(førsteFraværsdag) else it > halvG.beløp(førsteFraværsdag)
}

private fun søknadsfristOppfylt(søknad: SøknadDTO): Boolean {
    val søknadSendtMåned = søknad.sendtNav.toLocalDate().withDayOfMonth(1)
    val senesteMuligeSykedag = søknad.fom.plusMonths(3)
    return søknadSendtMåned.isBefore(senesteMuligeSykedag.plusDays(1))
}

private fun ikkeOppbruktSykepengedager(
    maksdato: LocalDate?,
    sisteSykepengedag: LocalDate
) = maksdato?.isBefore(sisteSykepengedag)
