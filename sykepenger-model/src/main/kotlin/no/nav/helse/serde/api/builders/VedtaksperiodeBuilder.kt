package no.nav.helse.serde.api.builders

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.*
import no.nav.helse.serde.api.*
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
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

internal class VedtaksperiodeBuilder(
    vedtaksperiode: Vedtaksperiode,
    dataForVilkårsvurdering: VilkårsgrunnlagHistorikk.Grunnlagsdata?,
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
    private val hendelseIder: Set<UUID>,
    private val inntektsmeldingId: UUID?,
    private val inntektshistorikkBuilder: InntektshistorikkBuilder,
    private val forkastet: Boolean
) : BuilderState() {

    private val beregningIder = mutableListOf<UUID>()
    private val fullstendig
        get() = tilstand.type in listOf(
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.UTBETALING_FEILET,
            TilstandType.REVURDERING_FEILET,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVVENTER_ARBEIDSGIVERE,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
        ) || (tilstand.type == TilstandType.AVSLUTTET_UTEN_UTBETALING && beregningIder.isNotEmpty())
    private val warnings =
        (hentWarnings(vedtaksperiode) + (dataForVilkårsvurdering?.meldingsreferanseId?.let { hentVilkårsgrunnlagWarnings(vedtaksperiode, id, it) }
            ?: emptyList())).distinctBy { it.melding }
    private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()

    private val medlemskapstatusDTO: MedlemskapstatusDTO? = dataForVilkårsvurdering?.let { grunnlagsdata ->
        when (grunnlagsdata.medlemskapstatus) {
            Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapstatusDTO.JA
            Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapstatusDTO.NEI
            else -> MedlemskapstatusDTO.VET_IKKE
        }
    }

    private val dataForVilkårsvurdering: GrunnlagsdataDTO? = dataForVilkårsvurdering?.let { grunnlagsdata ->
        GrunnlagsdataDTO(
            beregnetÅrsinntektFraInntektskomponenten = grunnlagsdata.sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
            avviksprosent = grunnlagsdata.avviksprosent?.ratio(),
            antallOpptjeningsdagerErMinst = grunnlagsdata.antallOpptjeningsdagerErMinst,
            harOpptjening = grunnlagsdata.harOpptjening,
            medlemskapstatus = medlemskapstatusDTO!!
        )
    }
    private val opptjeningDTO: OpptjeningDTO? = dataForVilkårsvurdering?.let { grunnlagsdata ->
        OpptjeningDTO(
            antallKjenteOpptjeningsdager = grunnlagsdata.antallOpptjeningsdagerErMinst,
            fom = skjæringstidspunkt.minusDays(grunnlagsdata.antallOpptjeningsdagerErMinst.toLong()),
            oppfylt = grunnlagsdata.harOpptjening
        )
    }
    private val avviksprosent: Double? = dataForVilkårsvurdering?.avviksprosent?.prosent()

    private var dataForSimulering: SimuleringsdataDTO? = null
    private var arbeidsgiverFagsystemId: String? = null
    private var personFagsystemId: String? = null
    private var inUtbetaling = false
    private var utbetalingGodkjent = false
    private val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
    private var forbrukteSykedager: Int? = null
    private var gjenståendeSykedager: Int? = null
    private var maksdato: LocalDate = LocalDate.MAX
    private var godkjentAv: String? = null
    private var godkjenttidspunkt: LocalDateTime? = null
    private var automatiskBehandling: Boolean = false
    private var totalbeløpArbeidstaker: Int = 0
    private var vedtaksperiodeUtbetaling: Utbetaling? = null

    internal fun build(hendelser: List<HendelseDTO>, utbetalinger: List<Utbetaling>): VedtaksperiodeDTOBase {
        val relevanteHendelser = hendelser.filter { it.id in hendelseIder.map { it.toString() } }

        val tilstandstypeDTO = buildTilstandtypeDto(finnRiktigUtbetaling(utbetalinger))

        if (!fullstendig) return buildUfullstendig(utbetalinger, tilstandstypeDTO, forkastet)

        val utbetalteUtbetalinger = byggUtbetalteUtbetalingerForPeriode(utbetalinger)
        return buildFullstendig(relevanteHendelser, tilstandstypeDTO, totalbeløpArbeidstaker, utbetalteUtbetalinger, forkastet)
    }

    private fun finnRiktigUtbetaling(utbetalinger: List<Utbetaling>) = arbeidsgiverFagsystemId?.let {
        utbetalinger.filter { utbetaling -> utbetaling.arbeidsgiverOppdrag().fagsystemId() == arbeidsgiverFagsystemId }
            .kronologisk()
            .reversed()
            .firstOrNull()
    }?.takeIf { it.erAnnullering() } ?: vedtaksperiodeUtbetaling

    private fun buildFullstendig(
        relevanteHendelser: List<HendelseDTO>,
        tilstandstypeDTO: TilstandstypeDTO,
        totalbeløpArbeidstaker: Int,
        utbetalteUtbetalinger: UtbetalingerDTO,
        forkastet: Boolean
    ): VedtaksperiodeDTO {
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
            inntektskilde = inntektskilde,
            beregningIder = beregningIder,
            erForkastet = forkastet
        )
    }

    private fun buildUfullstendig(utbetalinger: List<Utbetaling>, tilstandstypeDTO: TilstandstypeDTO, forkastet: Boolean): UfullstendigVedtaksperiodeDTO {
        val ufullstendingUtbetalingstidslinje = buildUfullstendigUtbetalingstidslinje(utbetalinger)
        return UfullstendigVedtaksperiodeDTO(
            id = id,
            gruppeId = gruppeId,
            fom = beregnetSykdomstidslinje.first().dagen,
            tom = beregnetSykdomstidslinje.last().dagen,
            tilstand = tilstandstypeDTO,
            fullstendig = false,
            utbetalingstidslinje = ufullstendingUtbetalingstidslinje,
            inntektskilde = inntektskilde,
            erForkastet = forkastet
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
        val sisteSykepengedagEllerSisteDagIPerioden = sykepengeperiode?.endInclusive ?: beregnetSykdomstidslinje.last().dagen
        val personalder = Alder(fødselsnummer)
        val sykepengedager = SykepengedagerDTO(
            forbrukteSykedager = forbrukteSykedager,
            skjæringstidspunkt = skjæringstidspunkt,
            førsteSykepengedag = sykepengeperiode?.start,
            maksdato = maksdato,
            gjenståendeDager = gjenståendeSykedager,
            oppfylt = maksdato > sisteSykepengedagEllerSisteDagIPerioden
        )
        val alderSisteSykepengedag = personalder.alderPåDato(sisteSykepengedagEllerSisteDagIPerioden)
        val alder = AlderDTO(
            alderSisteSykedag = alderSisteSykepengedag,
            oppfylt = personalder.datoForØvreAldersgrense > sisteSykepengedagEllerSisteDagIPerioden
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
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            TilstandType.AVVENTER_UFERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_REVURDERING,
            TilstandType.AVVENTER_UTBETALINGSGRUNNLAG,
            TilstandType.AVVENTER_HISTORIKK -> TilstandstypeDTO.Venter
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_ARBEIDSGIVERE -> TilstandstypeDTO.VenterPåKiling
            TilstandType.TIL_INFOTRYGD -> TilstandstypeDTO.TilInfotrygd
            TilstandType.UTBETALING_FEILET,
            TilstandType.REVURDERING_FEILET -> TilstandstypeDTO.Feilet
            TilstandType.TIL_UTBETALING -> TilstandstypeDTO.TilUtbetaling
            TilstandType.AVVENTER_GODKJENNING_REVURDERING,
            TilstandType.AVVENTER_GODKJENNING -> TilstandstypeDTO.Oppgaver
            TilstandType.AVSLUTTET -> when {
                utbetaling == null -> TilstandstypeDTO.IngenUtbetaling
                utbetaling.erAnnullering() && utbetaling.erUtbetalt() -> TilstandstypeDTO.Annullert
                utbetaling.erAnnullering() && utbetaling.harFeilet() -> TilstandstypeDTO.AnnulleringFeilet
                utbetaling.erAnnullering() -> TilstandstypeDTO.TilAnnullering
                utbetaling.revurdertUtenEndring() || utbetaling.harUtbetalinger() -> TilstandstypeDTO.Utbetalt
                utbetaling.utbetalingstidslinje(periode).kunFridager() -> TilstandstypeDTO.KunFerie
                else -> TilstandstypeDTO.IngenUtbetaling
            }
            TilstandType.AVSLUTTET_UTEN_UTBETALING,
            TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP,
            TilstandType.UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE ->
                if (utbetaling != null && utbetaling.utbetalingstidslinje(periode)
                        .kunFridager()
                ) TilstandstypeDTO.KunFerie else TilstandstypeDTO.IngenUtbetaling
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

    private fun hentVilkårsgrunnlagWarnings(vedtaksperiode: Vedtaksperiode, vedtaksperiodeId: UUID, vilkårsgrunnlagId: UUID): List<AktivitetDTO> {
        val aktiviteter = mutableListOf<AktivitetDTO>()
        Vedtaksperiode.hentVilkårsgrunnlagAktiviteter(vedtaksperiode).accept(object : AktivitetsloggVisitor {
            override fun visitWarn(
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitetslogg.Aktivitet.Warn,
                melding: String,
                tidsstempel: String
            ) {
                if (kontekster.filter { it.kontekstType == "Vilkårsgrunnlag" }
                        .mapNotNull { it.kontekstMap["meldingsreferanseId"] }
                        .map(UUID::fromString)
                        .any { it == vilkårsgrunnlagId }) {
                    aktiviteter.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
                }
            }
        })
        return aktiviteter.distinctBy { it.melding }
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        pushState(SykdomstidslinjeBuilder(beregnetSykdomstidslinje))
    }

    private var sykepengeperiode: Periode? = null

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        if (inUtbetaling) return
        sykepengeperiode = tidslinje.sykepengeperiode()
        pushState(UtbetalingstidslinjeBuilder(utbetalingstidslinje))
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
        if (tilstand is Utbetaling.Forkastet) return

        utbetalingGodkjent = tilstand !in listOf(Utbetaling.IkkeGodkjent, Utbetaling.Ubetalt)
        this.maksdato = maksdato
        this.gjenståendeSykedager = gjenståendeSykedager
        this.forbrukteSykedager = forbrukteSykedager
        this.beregningIder.add(beregningId)
        this.vedtaksperiodeUtbetaling = utbetaling
    }

    override fun visitVurdering(
        vurdering: Utbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
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
        if (tilstand is Utbetaling.Forkastet) return
        totalbeløpArbeidstaker = arbeidsgiverNettoBeløp + personNettoBeløp
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
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
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
                        grad = linje.grad!!
                    )
                }
                UtbetalingerDTO.UtbetalingDTO(linjer, fagsystemId)
            }
        }
}
