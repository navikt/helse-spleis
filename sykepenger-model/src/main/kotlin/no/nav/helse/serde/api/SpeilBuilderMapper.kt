package no.nav.helse.serde.api

import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.api.SimuleringsdataDTO.*
import no.nav.helse.sykdomstidslinje.erHelg
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
    inntekter: List<Inntekthistorikk.Inntekt>,
    førsteSykepengedag: LocalDate?,
    sisteSykepengedag: LocalDate?
): VedtaksperiodeDTO {
    val sykdomstidslinje = this["sykdomstidslinje"] as List<SykdomstidslinjedagDTO>
    val dataForVilkårsvurdering = this["dataForVilkårsvurdering"]?.let { it as GrunnlagsdataDTO }
    val hendelser = this["hendelser"] as List<HendelseDTO>
    val søknad = hendelser.find { it.type == "SENDT_SØKNAD_NAV" } as? SøknadNavDTO
    val vilkår = mapVilkår(
        this,
        sykdomstidslinje,
        fødselsnummer,
        dataForVilkårsvurdering,
        søknad,
        inntekter,
        førsteSykepengedag,
        sisteSykepengedag
    )
    return VedtaksperiodeDTO(
        id = this["id"] as UUID,
        gruppeId = this["gruppeId"] as UUID,
        fom = sykdomstidslinje.first().dagen,
        tom = sykdomstidslinje.last().dagen,
        tilstand = this["tilstand"] as TilstandstypeDTO,
        fullstendig = true,
        utbetalingsreferanse = this["utbetalingsreferanse"] as String?,
        utbetalingstidslinje = this["utbetalingstidslinje"] as List<UtbetalingstidslinjedagDTO>,
        sykdomstidslinje = sykdomstidslinje,
        godkjentAv = this["godkjentAv"] as String?,
        godkjenttidspunkt = this["godkjenttidspunkt"] as LocalDateTime?,
        vilkår = vilkår,
        førsteFraværsdag = this["førsteFraværsdag"] as? LocalDate,
        inntektFraInntektsmelding = this["inntektFraInntektsmelding"] as? Double,
        totalbeløpArbeidstaker = this["totalbeløpArbeidstaker"] as Int,
        hendelser = hendelser,
        dataForVilkårsvurdering = dataForVilkårsvurdering,
        simuleringsdata = this["dataForSimulering"] as? SimuleringsdataDTO,
        aktivitetslogg = this["aktivitetslogg"] as List<AktivitetDTO>,
        utbetalinger = this["utbetalinger"] as UtbetalingerDTO,
        forlengelseFraInfotrygd = this["forlengelseFraInfotrygd"] as ForlengelseFraInfotrygd
    )
}

internal fun Simulering.SimuleringResultat.mapTilSimuleringsdataDto(): SimuleringsdataDTO {
    return SimuleringsdataDTO(
        totalbeløp = totalbeløp,
        perioder = perioder.map { periode ->
            PeriodeDTO(
                fom = periode.periode.start,
                tom = periode.periode.endInclusive,
                utbetalinger = periode.utbetalinger.map { utbetaling ->
                    UtbetalingDTO(
                        utbetalesTilId = utbetaling.utbetalesTil.id,
                        utbetalesTilNavn = utbetaling.utbetalesTil.navn,
                        forfall = utbetaling.forfallsdato,
                        detaljer = utbetaling.detaljer.map { detaljer ->
                            DetaljerDTO(
                                faktiskFom = detaljer.periode.start,
                                faktiskTom = detaljer.periode.endInclusive,
                                konto = detaljer.konto,
                                beløp = detaljer.beløp,
                                tilbakeføring = detaljer.tilbakeføring,
                                sats = detaljer.sats.sats,
                                typeSats = detaljer.sats.type,
                                antallSats = detaljer.sats.antall,
                                uføregrad = detaljer.uføregrad,
                                klassekode = detaljer.klassekode.kode,
                                klassekodeBeskrivelse = detaljer.klassekode.beskrivelse,
                                utbetalingstype = detaljer.utbetalingstype,
                                refunderesOrgNr = detaljer.refunderesOrgnummer
                            )
                        },
                        feilkonto = utbetaling.feilkonto
                    )
                }
            )
        }
    )
}

internal fun MutableMap<String, Any?>.mapTilUfullstendigVedtaksperiodeDto(): UfullstendigVedtaksperiodeDTO {
    val sykdomstidslinje = this["sykdomstidslinje"] as List<SykdomstidslinjedagDTO>
    return UfullstendigVedtaksperiodeDTO(
        id = this["id"] as UUID,
        gruppeId = this["gruppeId"] as UUID,
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
    harOpptjening = grunnlagsdata.harOpptjening,
    medlemskapstatus = when (grunnlagsdata.medlemskapstatus) {
        Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapstatusDTO.JA
        Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapstatusDTO.NEI
        else -> MedlemskapstatusDTO.VET_IKKE
    }
)

internal fun mapOpptjening(
    førsteFraværsdag: LocalDate,
    dataForVilkårsvurdering: GrunnlagsdataDTO
) = OpptjeningDTO(
    antallKjenteOpptjeningsdager = dataForVilkårsvurdering.antallOpptjeningsdagerErMinst,
    fom = førsteFraværsdag.minusDays(dataForVilkårsvurdering.antallOpptjeningsdagerErMinst.toLong()),
    oppfylt = dataForVilkårsvurdering.harOpptjening
)

internal fun mapVilkår(
    vedtaksperiodeMap: Map<String, Any?>,
    sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    fødselsnummer: String,
    dataForVilkårsvurdering: GrunnlagsdataDTO?,
    søknadNav: SøknadNavDTO?,
    inntekter: List<Inntekthistorikk.Inntekt>,
    førsteSykepengedag: LocalDate?,
    sisteSykepengedag: LocalDate?
): VilkårDTO {
    val førsteFraværsdag = vedtaksperiodeMap["førsteFraværsdag"] as? LocalDate
    val sykepengegrunnlag = Inntekthistorikk.Inntekt.sykepengegrunnlag(inntekter, førsteFraværsdag ?: LocalDate.MAX)
    val beregnetMånedsinntekt = Inntekthistorikk.Inntekt.inntekt(inntekter, førsteFraværsdag ?: LocalDate.MAX)?.toDouble()
    val sisteSykepengedagEllerSisteDagIPerioden = sisteSykepengedag ?: sykdomstidslinje.last().dagen
    val personalder = SpleisAlder(fødselsnummer)
    val forbrukteSykedager = vedtaksperiodeMap["forbrukteSykedager"] as Int?
    val over67 = personalder.redusertYtelseAlder.isBefore(sisteSykepengedagEllerSisteDagIPerioden)
    val maksdato = vedtaksperiodeMap["maksdato"] as LocalDate?
    val gjenståendeDager = maksdato?.let {
        if (sisteSykepengedagEllerSisteDagIPerioden.isBefore(it)) {
            sisteSykepengedagEllerSisteDagIPerioden.datesUntil(it).filter { d -> !d.erHelg() }.count().toInt()
        } else {
            0
        }
    }
    val sykepengedager = SykepengedagerDTO(
        forbrukteSykedager = forbrukteSykedager,
        førsteFraværsdag = førsteFraværsdag,
        førsteSykepengedag = førsteSykepengedag,
        maksdato = maksdato,
        gjenståendeDager = gjenståendeDager,
        oppfylt = ikkeOppbruktSykepengedager(maksdato, sisteSykepengedagEllerSisteDagIPerioden)
    )
    val alderSisteSykepengedag = personalder.alderPåDato(sisteSykepengedagEllerSisteDagIPerioden)
    val alder = AlderDTO(
        alderSisteSykedag = alderSisteSykepengedag,
        oppfylt = personalder.øvreAldersgrense.isAfter(sisteSykepengedagEllerSisteDagIPerioden)
    )
    val opptjening = førsteFraværsdag?.let {
        dataForVilkårsvurdering?.let {
            mapOpptjening(førsteFraværsdag, it)
        }
    }
    val søknadsfrist = søknadNav?.let {
        SøknadsfristDTO(
            sendtNav = it.sendtNav,
            søknadFom = it.fom,
            søknadTom = it.tom,
            oppfylt = søknadsfristOppfylt(it)
        )
    }
    val sykepengegrunnlagDTO = førsteFraværsdag?.let {
        SykepengegrunnlagDTO(
            sykepengegrunnlag = sykepengegrunnlag,
            grunnbeløp = `1G`.beløp(førsteFraværsdag).toInt(),
            oppfylt = sykepengegrunnlagOppfylt(
                over67 = over67,
                beregnetMånedsinntekt = beregnetMånedsinntekt,
                førsteFraværsdag = førsteFraværsdag
            )
        )
    }
    return VilkårDTO(sykepengedager, alder, opptjening, søknadsfrist, sykepengegrunnlagDTO)
}

private fun sykepengegrunnlagOppfylt(
    over67: Boolean,
    beregnetMånedsinntekt: Double?,
    førsteFraværsdag: LocalDate
) = beregnetMånedsinntekt?.times(12)?.let {
    if (over67) it > `2G`.beløp(førsteFraværsdag) else it > halvG.beløp(førsteFraværsdag)
}

private fun søknadsfristOppfylt(søknadNav: SøknadNavDTO): Boolean {
    val søknadSendtMåned = søknadNav.sendtNav.toLocalDate().withDayOfMonth(1)
    val senesteMuligeSykedag = søknadNav.fom.plusMonths(3)
    return søknadSendtMåned.isBefore(senesteMuligeSykedag.plusDays(1))
}

private fun ikkeOppbruktSykepengedager(
    maksdato: LocalDate?,
    sisteSykepengedag: LocalDate
) = maksdato?.isAfter(sisteSykepengedag)
