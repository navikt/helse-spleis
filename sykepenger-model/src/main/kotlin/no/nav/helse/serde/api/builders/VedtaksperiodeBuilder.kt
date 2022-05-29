package no.nav.helse.serde.api.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Periodetype
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.serde.api.AlderDTO
import no.nav.helse.serde.api.DagtypeDTO
import no.nav.helse.serde.api.GrunnlagsdataDTO
import no.nav.helse.serde.api.MedlemskapstatusDTO
import no.nav.helse.serde.api.OpptjeningDTO
import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.SykepengedagerDTO
import no.nav.helse.serde.api.SøknadsfristDTO
import no.nav.helse.serde.api.TilstandstypeDTO
import no.nav.helse.serde.api.UfullstendigVedtaksperiodeDTO
import no.nav.helse.serde.api.UfullstendigVedtaksperiodedagDTO
import no.nav.helse.serde.api.VedtaksperiodeDTO
import no.nav.helse.serde.api.VedtaksperiodeDTOBase
import no.nav.helse.serde.api.VilkårDTO
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent

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
    private val hendelseIder: Set<Dokumentsporing>,
    private val vilkårsgrunnlagInntektBuilder: VilkårsgrunnlagInntektBuilder,
    private val forkastet: Boolean
) : BuilderState() {

    private val beregningIder = mutableListOf<UUID>()
    private val fullstendig
        get() = tilstand.type in listOf(
            AVSLUTTET,
            AVVENTER_GODKJENNING,
            AVVENTER_GODKJENNING_REVURDERING,
            UTBETALING_FEILET,
            REVURDERING_FEILET,
            TIL_UTBETALING,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        ) || (tilstand.type in listOf(
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE
        ) && beregningIder.isNotEmpty())
    private val grunnlagsdataBuilder = dataForVilkårsvurdering?.let { GrunnlagsdataBuilder(skjæringstidspunkt, it) }

    private val warnings = hentWarningsMedForegåendeUtenUtbetaling(vedtaksperiode)

    private var vedtaksperiodeUtbetalingstidslinjeperiode = LocalDate.MIN til LocalDate.MIN
    private var inUtbetaling = false
    private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()

    private var inntektsmeldingId: UUID? = null
    private var utbetalingId: UUID? = null

    internal fun build(hendelser: List<HendelseDTO>, utbetalinger: List<UtbetalingshistorikkElementDTO>): VedtaksperiodeDTOBase {
        val relevanteHendelser = hendelser.filter { it.id in hendelseIder.ider().map(UUID::toString) }

        val utbetaling = utbetalingId?.let { UtbetalingshistorikkElementDTO.UtbetalingDTO.utbetalingFor(utbetalinger, it) }

        val tilstandstypeDTO = UtbetalingshistorikkElementDTO.UtbetalingDTO.tilstandFor(
            periode = periode,
            tilstandstype = tilstand.type,
            utbetaling = utbetaling,
            utbetalinger = utbetalinger
        )

        if (!fullstendig) return buildUfullstendig(utbetaling, tilstandstypeDTO, forkastet)

        val sisteUtbetalingFor = utbetaling?.let { UtbetalingshistorikkElementDTO.UtbetalingDTO.sisteUtbetalingFor(utbetalinger, utbetaling) }
        return buildFullstendig(relevanteHendelser, tilstandstypeDTO, utbetaling, sisteUtbetalingFor, forkastet)
    }

    private fun buildFullstendig(
        relevanteHendelser: List<HendelseDTO>,
        tilstandstypeDTO: TilstandstypeDTO,
        utbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?,
        sisteUtbetalingFor: UtbetalingshistorikkElementDTO.UtbetalingDTO?,
        forkastet: Boolean
    ): VedtaksperiodeDTO {
        vilkårsgrunnlagInntektBuilder.nøkkeldataOmInntekt(VilkårsgrunnlagInntektBuilder.NøkkeldataOmInntekt(periode.endInclusive, skjæringstidspunkt, grunnlagsdataBuilder?.avviksprosent))

        val tom = beregnetSykdomstidslinje.last().dagen
        val vilkår = buildVilkår(utbetaling, relevanteHendelser)
        return VedtaksperiodeDTO(
            id = id,
            gruppeId = gruppeId,
            fom = beregnetSykdomstidslinje.first().dagen,
            tom = tom,
            tilstand = tilstandstypeDTO,
            fullstendig = true,
            utbetalingsreferanse = null, // TODO: deprecated/never set in SpeilBuilder
            utbetalingstidslinje = utbetaling?.utbetalingstidslinje?.filter { it.dato in vedtaksperiodeUtbetalingstidslinjeperiode } ?: emptyList(),
            sykdomstidslinje = beregnetSykdomstidslinje,
            vilkår = vilkår,
            inntektsmeldingId = inntektsmeldingId,
            inntektFraInntektsmelding = sykepengegrunnlag?.reflection { _, månedlig, _, _ -> månedlig },
            hendelser = relevanteHendelser,
            dataForVilkårsvurdering = grunnlagsdataBuilder?.grunnlagsdata,
            aktivitetslogg = warnings,
            utbetaling = utbetaling,
            sisteUtbetaling = sisteUtbetalingFor,
            forlengelseFraInfotrygd = forlengelseFraInfotrygd,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
            beregningIder = beregningIder,
            erForkastet = forkastet
        )
    }

    private fun buildUfullstendig(utbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?, tilstandstypeDTO: TilstandstypeDTO, forkastet: Boolean): UfullstendigVedtaksperiodeDTO {
        val ufullstendingUtbetalingstidslinje = buildUfullstendigUtbetalingstidslinje(utbetaling)
        return UfullstendigVedtaksperiodeDTO(
            id = id,
            gruppeId = gruppeId,
            fom = beregnetSykdomstidslinje.first().dagen,
            tom = beregnetSykdomstidslinje.last().dagen,
            tilstand = tilstandstypeDTO,
            fullstendig = false,
            utbetalingstidslinje = ufullstendingUtbetalingstidslinje,
            sykdomstidslinje = beregnetSykdomstidslinje,
            inntektskilde = inntektskilde,
            erForkastet = forkastet
        )
    }

    private fun buildUfullstendigUtbetalingstidslinje(utbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?) =
        utbetaling?.utbetalingstidslinje?.filter { it.dato in vedtaksperiodeUtbetalingstidslinjeperiode }?.map { UfullstendigVedtaksperiodedagDTO(
            type = it.type,
            dato = it.dato
        )} ?: emptyList()

    private fun buildVilkår(utbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?, hendelser: List<HendelseDTO>): VilkårDTO {
        val sykepengeperiode = utbetaling?.utbetalingstidslinje?.filter { it.dato in periode }?.let { dager ->
            val første = dager.firstOrNull { it.type == DagtypeDTO.NavDag } ?: return@let null
            val siste = dager.last { it.type == DagtypeDTO.NavDag }
            første.dato til siste.dato
        }
        val sisteSykepengedagEllerSisteDagIPerioden = sykepengeperiode?.endInclusive ?: beregnetSykdomstidslinje.last().dagen
        val personalder = fødselsnummer.somFødselsnummer().alder()
        val maksdato = utbetaling?.maksdato ?: LocalDate.MAX
        val sykepengedager = SykepengedagerDTO(
            forbrukteSykedager = utbetaling?.forbrukteSykedager,
            skjæringstidspunkt = skjæringstidspunkt,
            førsteSykepengedag = sykepengeperiode?.start,
            maksdato = maksdato,
            gjenståendeDager = utbetaling?.gjenståendeSykedager,
            oppfylt = maksdato > sisteSykepengedagEllerSisteDagIPerioden
        )
        val alderSisteSykepengedag = personalder.alderPåDato(sisteSykepengedagEllerSisteDagIPerioden)
        val alder = AlderDTO(
            alderSisteSykedag = alderSisteSykepengedag,
            oppfylt = personalder.innenfor70årsgrense(sisteSykepengedagEllerSisteDagIPerioden)
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

        return VilkårDTO(sykepengedager, alder, grunnlagsdataBuilder?.opptjening, søknadsfrist, grunnlagsdataBuilder?.medlemskapstatus)
    }

    private fun søknadsfristOppfylt(søknadNav: SøknadNavDTO): Boolean {
        val søknadSendtMåned = søknadNav.sendtNav.toLocalDate().withDayOfMonth(1)
        val senesteMuligeSykedag = søknadNav.fom.plusMonths(3)
        return søknadSendtMåned < senesteMuligeSykedag.plusDays(1)
    }

    private fun hentWarningsMedForegåendeUtenUtbetaling(vedtaksperiode: Vedtaksperiode): List<AktivitetDTO> {
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

    override fun visitInntektsmeldinginfo(id: UUID, arbeidsforholdId: String?) {
        this.inntektsmeldingId = id
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        pushState(SykdomstidslinjeBuilder(beregnetSykdomstidslinje))
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?
    ) {
        this.inUtbetaling = true
        if (tilstand is Utbetaling.Forkastet) return
        this.utbetalingId = id
        this.beregningIder.add(beregningId)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        if (this.inUtbetaling || tidslinje.isEmpty()) return
        this.vedtaksperiodeUtbetalingstidslinjeperiode = tidslinje.periode()
    }

    override fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?
    ) {
        this.inUtbetaling = false
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        popState()
    }
}

private class GrunnlagsdataBuilder(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) : VilkårsgrunnlagHistorikkVisitor {
    init {
        grunnlagsdata.accept(this)
    }

    lateinit var medlemskapstatus: MedlemskapstatusDTO
        private set
    lateinit var grunnlagsdata: GrunnlagsdataDTO
        private set
    lateinit var opptjening: OpptjeningDTO
        private set
    var avviksprosent: Double? = null
        private set
    var meldingsreferanseId: UUID? = null
        private set

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        this.medlemskapstatus = when (medlemskapstatus) {
            Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapstatusDTO.JA
            Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapstatusDTO.NEI
            else -> MedlemskapstatusDTO.VET_IKKE
        }
        this.grunnlagsdata = GrunnlagsdataDTO(
            beregnetÅrsinntektFraInntektskomponenten = sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
            avviksprosent = avviksprosent?.ratio(),
            antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager(),
            harOpptjening = opptjening.erOppfylt(),
            medlemskapstatus = this.medlemskapstatus
        )
        this.opptjening = OpptjeningDTO(
            antallKjenteOpptjeningsdager = opptjening.opptjeningsdager(),
            fom = opptjening.opptjeningFom(),
            oppfylt = opptjening.erOppfylt()
        )
        this.avviksprosent = avviksprosent?.prosent()
        this.meldingsreferanseId = meldingsreferanseId
    }
}

