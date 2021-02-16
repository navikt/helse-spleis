package no.nav.helse.serde.api.builders

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.api.*
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.kronologisk
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeState(
    vedtaksperiode: Vedtaksperiode,
    private val id: UUID,
    private val periode: Periode,
    private val skjæringstidspunkt: LocalDate,
    private val periodetype: Periodetype,
    private val forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
    private val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
    private val inntektskilde: Inntektskilde,
    private val sykepengegrunnlag: Inntekt?,
    private val gruppeId: UUID,
    private val fødselsnummer: String,
    private val hendelseIder: List<UUID>,
    private val inntektsmeldingId: UUID?,
    private val inntektshistorikkBuilder: InntektshistorikkBuilder
) : BuilderState() {

    private val fullstendig = tilstand.type in listOf(
        TilstandType.AVSLUTTET,
        TilstandType.AVVENTER_GODKJENNING,
        TilstandType.UTBETALING_FEILET,
        TilstandType.TIL_UTBETALING,
        TilstandType.AVVENTER_ARBEIDSGIVERE
    )
    private val warnings = hentWarnings(vedtaksperiode)
    private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()
    private val totalbeløpakkumulator = mutableListOf<Int>()
    private var dataForVilkårsvurdering: GrunnlagsdataDTO? = null
    private var opptjeningDTO: OpptjeningDTO? = null
    private var medlemskapstatusDTO: MedlemskapstatusDTO? = null
    private var dataForSimulering: SimuleringsdataDTO? = null
    private var arbeidsgiverFagsystemId: String? = null
    private var personFagsystemId: String? = null
    private var inUtbetaling = false
    private var utbetalingGodkjent = false
    private var avviksprosent: Double? = null
    private val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
    private var forbrukteSykedager: Int? = null
    private var gjenståendeSykedager: Int? = null
    private var maksdato: LocalDate = LocalDate.MAX
    private var godkjentAv: String? = null
    private var godkjenttidspunkt: LocalDateTime? = null
    private var automatiskBehandling: Boolean = false

    internal fun build(hendelser: List<HendelseDTO>, utbetalinger: List<Utbetaling>): VedtaksperiodeDTOBase {
        val relevanteHendelser = hendelser.filter { it.id in hendelseIder.map { it.toString() } }
        val utbetaling = arbeidsgiverFagsystemId?.let {
            utbetalinger.filter { utbetaling -> utbetaling.arbeidsgiverOppdrag().fagsystemId() == arbeidsgiverFagsystemId }
                .kronologisk()
                .reversed()
                .firstOrNull()
        }
        val tilstandstypeDTO = buildTilstandtypeDto(utbetaling)

        if (!fullstendig) return buildUfullstendig(utbetalinger, tilstandstypeDTO)

        val totalbeløpArbeidstaker = totalbeløpakkumulator.sum()
        val utbetalteUtbetalinger = byggUtbetalteUtbetalingerForPeriode(utbetalinger)
        return buildFullstendig(relevanteHendelser, tilstandstypeDTO, totalbeløpArbeidstaker, utbetalteUtbetalinger)
    }

    private fun buildFullstendig(relevanteHendelser: List<HendelseDTO>, tilstandstypeDTO: TilstandstypeDTO, totalbeløpArbeidstaker: Int, utbetalteUtbetalinger: UtbetalingerDTO): VedtaksperiodeDTO {
        inntektshistorikkBuilder.nøkkeldataOmInntekt(InntektshistorikkBuilder.NøkkeldataOmInntekt(periode.endInclusive, skjæringstidspunkt, avviksprosent))

        val tom = beregnetSykdomstidslinje.last().dagen
        val vilkår = buildVilkår(relevanteHendelser)
        return VedtaksperiodeDTO(
            id = id,
            gruppeId = gruppeId,
            fom = beregnetSykdomstidslinje.first().dagen,
            tom = tom,
            tilstand = tilstandstypeDTO,
            fullstendig = true,
            utbetalingsreferanse = null, // TODO: deprecated/never set in SpeilBuilder
            utbetalingstidslinje = utbetalingstidslinje,
            sykdomstidslinje = beregnetSykdomstidslinje,
            godkjentAv = godkjentAv,
            godkjenttidspunkt = godkjenttidspunkt,
            automatiskBehandlet = automatiskBehandling,
            vilkår = vilkår,
            inntektsmeldingId = inntektsmeldingId,
            inntektFraInntektsmelding = sykepengegrunnlag?.reflection { _, månedlig, _, _ -> månedlig },
            totalbeløpArbeidstaker = totalbeløpArbeidstaker,
            hendelser = relevanteHendelser,
            dataForVilkårsvurdering = dataForVilkårsvurdering,
            simuleringsdata = dataForSimulering,
            aktivitetslogg = warnings,
            utbetalinger = utbetalteUtbetalinger,
            utbetalteUtbetalinger = utbetalteUtbetalinger,
            forlengelseFraInfotrygd = forlengelseFraInfotrygd,
            periodetype = periodetype,
            inntektskilde = inntektskilde
        )
    }

    private fun buildUfullstendig(utbetalinger: List<Utbetaling>, tilstandstypeDTO: TilstandstypeDTO): UfullstendigVedtaksperiodeDTO {
        val ufullstendingUtbetalingstidslinje = buildUfullstendigUtbetalingstidslinje(utbetalinger)
        return UfullstendigVedtaksperiodeDTO(
            id = id,
            gruppeId = gruppeId,
            fom = beregnetSykdomstidslinje.first().dagen,
            tom = beregnetSykdomstidslinje.last().dagen,
            tilstand = tilstandstypeDTO,
            fullstendig = false,
            utbetalingstidslinje = ufullstendingUtbetalingstidslinje,
            inntektskilde = inntektskilde
        )
    }

    private fun buildUfullstendigUtbetalingstidslinje(utbetalinger: List<Utbetaling>) =
        utbetalinger
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
            .subset(periode)
            .map {
                val type = when (it) {
                    is Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag -> TypeDataDTO.ArbeidsgiverperiodeDag
                    is Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag -> TypeDataDTO.Arbeidsdag
                    is Utbetalingstidslinje.Utbetalingsdag.AvvistDag -> TypeDataDTO.AvvistDag
                    is Utbetalingstidslinje.Utbetalingsdag.Fridag -> TypeDataDTO.Feriedag
                    is Utbetalingstidslinje.Utbetalingsdag.ForeldetDag -> TypeDataDTO.ForeldetDag
                    is Utbetalingstidslinje.Utbetalingsdag.UkjentDag -> TypeDataDTO.UkjentDag
                    is Utbetalingstidslinje.Utbetalingsdag.NavDag -> TypeDataDTO.NavDag
                    is Utbetalingstidslinje.Utbetalingsdag.NavHelgDag -> TypeDataDTO.ArbeidsgiverperiodeDag
                }
                UfullstendigVedtaksperiodedagDTO(type = type, dato = it.dato)
            }

    private fun buildVilkår(hendelser: List<HendelseDTO>): VilkårDTO {
        val sisteSykepengedagEllerSisteDagIPerioden = sisteSykepengedag ?: beregnetSykdomstidslinje.last().dagen
        val personalder = Alder(fødselsnummer)
        val sykepengedager = SykepengedagerDTO(
            forbrukteSykedager = forbrukteSykedager,
            skjæringstidspunkt = skjæringstidspunkt,
            førsteSykepengedag = førsteSykepengedag,
            maksdato = maksdato,
            gjenståendeDager = gjenståendeSykedager,
            oppfylt = maksdato > sisteSykepengedagEllerSisteDagIPerioden
        )
        val alderSisteSykepengedag = personalder.alderPåDato(sisteSykepengedagEllerSisteDagIPerioden)
        val alder = AlderDTO(
            alderSisteSykedag = alderSisteSykepengedag,
            oppfylt = personalder.øvreAldersgrense > sisteSykepengedagEllerSisteDagIPerioden
        )
        val søknadNav = hendelser.find { it.type == "SENDT_SØKNAD_NAV" } as? SøknadNavDTO
        val søknadsfrist = søknadNav?.let {
            SøknadsfristDTO(
                sendtNav = it.sendtNav,
                søknadFom = it.fom,
                søknadTom = it.tom,
                oppfylt = søknadsfristOppfylt(it)
            )
        }

        return VilkårDTO(sykepengedager, alder, opptjeningDTO, søknadsfrist, medlemskapstatusDTO)
    }

    private fun søknadsfristOppfylt(søknadNav: SøknadNavDTO): Boolean {
        val søknadSendtMåned = søknadNav.sendtNav.toLocalDate().withDayOfMonth(1)
        val senesteMuligeSykedag = søknadNav.fom.plusMonths(3)
        return søknadSendtMåned < senesteMuligeSykedag.plusDays(1)
    }

    private fun buildTilstandtypeDto(utbetaling: Utbetaling?): TilstandstypeDTO {
        val utbetalt = totalbeløpakkumulator.sum() > 0
        val kunFerie = beregnetSykdomstidslinje.all { it.type == SpeilDagtype.FERIEDAG }
        return when (tilstand.type) {
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
            TilstandType.AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            TilstandType.AVVENTER_UFERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE,
            TilstandType.AVVENTER_HISTORIKK -> TilstandstypeDTO.Venter
            TilstandType.TIL_INFOTRYGD -> TilstandstypeDTO.TilInfotrygd
            TilstandType.UTBETALING_FEILET -> TilstandstypeDTO.Feilet
            TilstandType.TIL_UTBETALING -> TilstandstypeDTO.TilUtbetaling
            TilstandType.TIL_ANNULLERING -> TilstandstypeDTO.TilAnnullering
            TilstandType.AVVENTER_GODKJENNING -> TilstandstypeDTO.Oppgaver
            TilstandType.AVSLUTTET -> when {
                utbetaling == null -> TilstandstypeDTO.IngenUtbetaling
                utbetaling.erAnnullering() && utbetaling.erUtbetalt() -> TilstandstypeDTO.Annullert
                utbetaling.erAnnullering() && utbetaling.harFeilet() -> TilstandstypeDTO.AnnulleringFeilet
                utbetaling.erAnnullering() -> TilstandstypeDTO.TilAnnullering
                utbetalt -> TilstandstypeDTO.Utbetalt
                kunFerie -> TilstandstypeDTO.KunFerie
                else -> TilstandstypeDTO.IngenUtbetaling
            }
            TilstandType.AVSLUTTET_UTEN_UTBETALING,
            TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING,
            TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP,
            TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE ->
                if (kunFerie) TilstandstypeDTO.KunFerie else TilstandstypeDTO.IngenUtbetaling
        }
    }

    private fun hentWarnings(vedtaksperiode: Vedtaksperiode): List<AktivitetDTO> {
        val aktiviteter = mutableListOf<AktivitetDTO>()
        Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
            .accept(object : AktivitetsloggVisitor {
                override fun visitWarn(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitetslogg.Aktivitet.Warn,
                    melding: String,
                    tidsstempel: String
                ) {
                    kontekster.find { it.kontekstType == "Vedtaksperiode" }
                        ?.let { it.kontekstMap["vedtaksperiodeId"] }
                        ?.let(UUID::fromString)
                        ?.also { vedtaksperiodeId ->
                            aktiviteter.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
                        }
                }
            })
        return aktiviteter.distinctBy { it.melding }
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        pushState(SykdomstidslinjeState(beregnetSykdomstidslinje))
    }

    private var førsteSykepengedag: LocalDate? = null
    private var sisteSykepengedag: LocalDate? = null

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        if (inUtbetaling) return
        førsteSykepengedag = tidslinje.førsteSykepengedag()
        sisteSykepengedag = tidslinje.sisteSykepengedag()
        pushState(UtbetalingstidslinjeState(utbetalingstidslinje, totalbeløpakkumulator))
    }

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        if (dataForSimuleringResultat == null) return
        dataForSimulering = SimuleringsdataDTO(
            totalbeløp = dataForSimuleringResultat.totalbeløp,
            perioder = dataForSimuleringResultat.perioder.map { periode ->
                SimuleringsdataDTO.PeriodeDTO(
                    fom = periode.periode.start,
                    tom = periode.periode.endInclusive,
                    utbetalinger = periode.utbetalinger.map { utbetaling ->
                        SimuleringsdataDTO.UtbetalingDTO(
                            utbetalesTilId = utbetaling.utbetalesTil.id,
                            utbetalesTilNavn = utbetaling.utbetalesTil.navn,
                            forfall = utbetaling.forfallsdato,
                            detaljer = utbetaling.detaljer.map { detaljer ->
                                SimuleringsdataDTO.DetaljerDTO(
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

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        inUtbetaling = true
        utbetalingGodkjent = tilstand !in listOf(Utbetaling.IkkeGodkjent, Utbetaling.Ubetalt)
        this.maksdato = maksdato
        this.gjenståendeSykedager = gjenståendeSykedager
        this.forbrukteSykedager = forbrukteSykedager
    }

    override fun visitVurdering(vurdering: Utbetaling.Vurdering, ident: String, epost: String, tidspunkt: LocalDateTime, automatiskBehandling: Boolean) {
        if (!utbetalingGodkjent) return
        godkjentAv = ident
        godkjenttidspunkt = tidspunkt
        this.automatiskBehandling = automatiskBehandling
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverFagsystemId = oppdrag.fagsystemId()
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        personFagsystemId = oppdrag.fagsystemId()
    }

    override fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        inUtbetaling = false
    }

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {
        if (dataForVilkårsvurdering == null) return
        val medlemskapstatusDTO = when (dataForVilkårsvurdering.medlemskapstatus) {
            Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapstatusDTO.JA
            Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapstatusDTO.NEI
            else -> MedlemskapstatusDTO.VET_IKKE
        }.also { this.medlemskapstatusDTO = it }

        this.dataForVilkårsvurdering = GrunnlagsdataDTO(
            beregnetÅrsinntektFraInntektskomponenten = dataForVilkårsvurdering.beregnetÅrsinntektFraInntektskomponenten.reflection { årlig, _, _, _ -> årlig },
            avviksprosent = dataForVilkårsvurdering.avviksprosent?.ratio(),
            antallOpptjeningsdagerErMinst = dataForVilkårsvurdering.antallOpptjeningsdagerErMinst,
            harOpptjening = dataForVilkårsvurdering.harOpptjening,
            medlemskapstatus = medlemskapstatusDTO
        )
        opptjeningDTO = OpptjeningDTO(
            antallKjenteOpptjeningsdager = dataForVilkårsvurdering.antallOpptjeningsdagerErMinst,
            fom = skjæringstidspunkt.minusDays(dataForVilkårsvurdering.antallOpptjeningsdagerErMinst.toLong()),
            oppfylt = dataForVilkårsvurdering.harOpptjening
        )

        avviksprosent = dataForVilkårsvurdering.avviksprosent?.prosent()
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: List<UUID>,
        inntektsmeldingId: UUID?,
        inntektskilde: Inntektskilde
    ) {
        popState()
    }

    private fun byggUtbetalteUtbetalingerForPeriode(utbetalinger: List<Utbetaling>): UtbetalingerDTO =
        UtbetalingerDTO(
            utbetalinger.filter { it.erUtbetalt() }.byggUtbetaling(
                arbeidsgiverFagsystemId,
                Utbetaling::arbeidsgiverOppdrag
            ),
            utbetalinger.filter { it.erUtbetalt() }.byggUtbetaling(
                personFagsystemId,
                Utbetaling::personOppdrag
            )
        )

    private fun List<Utbetaling>.byggUtbetaling(fagsystemId: String?, oppdragStrategy: (Utbetaling) -> Oppdrag) =
        fagsystemId?.let {
            this.lastOrNull { utbetaling ->
                oppdragStrategy(utbetaling).fagsystemId() == fagsystemId
            }?.let { utbetaling ->
                val linjer = oppdragStrategy(utbetaling).linjerUtenOpphør().map { linje ->
                    UtbetalingerDTO.UtbetalingslinjeDTO(
                        fom = linje.fom,
                        tom = linje.tom,
                        dagsats = linje.beløp!!,
                        grad = linje.grad
                    )
                }
                UtbetalingerDTO.UtbetalingDTO(linjer, fagsystemId)
            }
        }
}
