package no.nav.helse.serde.api

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.api.SpeilBuilder.Utbetalingshistorikkladd.Companion.tilDto
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.reflection.ArbeidsgiverReflect
import no.nav.helse.serde.reflection.PersonReflect
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.kronologisk
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.first
import kotlin.collections.getValue
import kotlin.collections.set
import kotlin.math.roundToInt


private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(person, hendelser)
    return jsonBuilder.toJson()
}

internal class SpeilBuilder(person: Person, private val hendelser: List<HendelseDTO>) {

    private val rootState = Root()
    private val stack = mutableListOf<PersonVisitor>(rootState)
    private val hendelsePerioder = mutableMapOf<UUID, Periode>()

    init {
        person.accept(DelegatedPersonVisitor(stack::first))
    }

    private fun finnHendelser(ider: List<UUID>) = hendelser.filter { UUID.fromString(it.id) in ider }

    internal fun toJson() = rootState.toJson()

    private fun pushState(state: PersonVisitor) {
        stack.add(0, state)
    }

    private fun popState() {
        stack.removeAt(0)
    }

    private inner class Root : PersonVisitor {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: String
        ) {
            pushState(PersonState(person, personMap, fødselsnummer))
        }

        override fun toString() = personMap.toString()

        fun toJson() = personMap.mapTilPersonDto()
    }

    private inner class PersonState(
        person: Person,
        private val personMap: MutableMap<String, Any?>,
        private val fødselsnummer: String
    ) : PersonVisitor {
        init {
            personMap.putAll(PersonReflect(person).toSpeilMap())
        }

        private val arbeidsgivere = mutableListOf<ArbeidsgiverDTO>()
        private val inntektshistorikk = mutableMapOf<String, InntektshistorikkVol2>()
        private val nøkkeldataOmInntekter = mutableListOf<NøkkeldataOmInntekt>()

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            if (!arbeidsgiver.harHistorikk()) return
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap, fødselsnummer, arbeidsgivere, inntektshistorikk, nøkkeldataOmInntekter))
        }

        override fun postVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: String
        ) {
            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                personMap["inntektsgrunnlag"] = inntektsgrunnlag(
                    person,
                    inntektshistorikk,
                    nøkkeldataOmInntekter
                        .groupBy { it.skjæringstidspunkt }
                        .mapNotNull { (_, value) -> value.maxByOrNull { it.sisteDagISammenhengendePeriode } }
                )
            }
            popState()
        }
    }

    private inner class ArbeidsgiverState(
        private val arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>,
        private val fødselsnummer: String,
        private val arbeidsgivere: MutableList<ArbeidsgiverDTO>,
        private val inntektshistorikk: MutableMap<String, InntektshistorikkVol2>,
        private val nøkkeldataOmInntekter: MutableList<NøkkeldataOmInntekt>
    ) : PersonVisitor {
        private val utbetalingshistorikkladder = mutableMapOf<UUID, Utbetalingshistorikkladd>()
        private val utbetalingberegning = mutableMapOf<UUID, Pair<UUID, LocalDateTime>>()

        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toSpeilMap())
        }

        private val vedtaksperioder = mutableListOf<VedtaksperiodeDTOBase>()
        private val gruppeIder = mutableMapOf<Vedtaksperiode, UUID>()
        private val utbetalinger = mutableMapOf<UUID, MutableList<Utbetaling>>()
        private val utbetalingerDTOs = mutableMapOf<UUID, MutableList<Utbetalingshistorikkladd.UtbetalingKladd>>()
        var inntekter = mutableListOf<Inntektshistorikk.Inntektsendring>()

        internal fun vedtaksperiode(vedtaksperiode: VedtaksperiodeDTOBase) {
            vedtaksperioder.add(vedtaksperiode)
        }

        override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                this.inntektshistorikk[arbeidsgiver.organisasjonsnummer()] = inntektshistorikk
            }
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
            utbetalinger.getOrPut(beregningId) { mutableListOf() }.add(utbetaling)
            val utbetalingstidslinjedager = mutableListOf<UtbetalingstidslinjedagDTO>()
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjedager, mutableListOf()))
            utbetalingerDTOs.getOrPut(beregningId) { mutableListOf() }
                .add(Utbetalingshistorikkladd.UtbetalingKladd(utbetalingstidslinje = utbetalingstidslinjedager))
        }

        override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
            val kladd = Utbetalingshistorikkladd()
            pushState(SykdomshistorikkElementState(kladd))
            utbetalingshistorikkladder[id] = kladd
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            this.vedtaksperioder.clear()
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] = this.vedtaksperioder.toList()
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            this.vedtaksperioder.clear()
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] =
                (arbeidsgiverMap["vedtaksperioder"] as List<Any?>) + this.vedtaksperioder.toList()
                    .filter { it.tilstand.visesNårForkastet() }
        }

        override fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, hendelseId: UUID) {
            inntekter.add(inntektsendring)
        }

        override fun preVisitVedtaksperiode(
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
            gruppeIder[vedtaksperiode] = arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
                ?.let(gruppeIder::getValue)
                ?: UUID.randomUUID()

            val sykepengegrunnlag = arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start)

            pushState(
                VedtaksperiodeState(
                    arbeidsgiverState = this,
                    vedtaksperiode = vedtaksperiode,
                    id = id,
                    periode = periode,
                    skjæringstidspunkt = skjæringstidspunkt,
                    periodetype = periodetype,
                    forlengelseFraInfotrygd = forlengelseFraInfotrygd,
                    tilstand = tilstand,
                    inntektskilde = inntektskilde,
                    sykepengegrunnlag = sykepengegrunnlag,
                    gruppeId = gruppeIder.getValue(vedtaksperiode),
                    fødselsnummer = fødselsnummer,
                    inntekter = inntekter,
                    utbetalinger = utbetalinger.flatMap { it.value },
                    hendelseIder = hendelseIder,
                    inntektsmeldingId = inntektsmeldingId,
                    nøkkeldataOmInntekter = nøkkeldataOmInntekter
                )
            )
        }

        override fun visitUtbetalingstidslinjeberegning(id: UUID, tidsstempel: LocalDateTime, sykdomshistorikkElementId: UUID) {
            utbetalingberegning[id] = sykdomshistorikkElementId to tidsstempel
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            utbetalingberegning.forEach { (beregningId, beregningInfo) ->
                val utbetalingerForBeregning = utbetalingerDTOs[beregningId] ?: emptyList()
                val (sykdomshistorikkelementId) = beregningInfo

                utbetalingshistorikkladder.getValue(sykdomshistorikkelementId).also {
                    it.utbetalinger.addAll(utbetalingerForBeregning)
                }
            }
            val tidslinjer = utbetalingshistorikkladder.values.filter { it.utbetalinger.isNotEmpty() }
            val arbeidsgiverDTO = arbeidsgiverMap.mapTilArbeidsgiverDto(tidslinjer.tilDto())
            arbeidsgivere.add(arbeidsgiverDTO)
            popState()
        }
    }

    private inner class SykdomshistorikkElementState(
        var resultat: Utbetalingshistorikkladd
    ) : PersonVisitor {

        override fun preVisitHendelseSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            hendelseId?.also { hendelsePerioder[it] = tidslinje.periode()!! }
            val dager = mutableListOf<SykdomstidslinjedagDTO>()
            resultat.hendelsetidslinje = dager
            pushState(SykdomstidslinjeState(dager))
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val dager = mutableListOf<SykdomstidslinjedagDTO>()
            resultat.beregnettidslinje = dager
            pushState(SykdomstidslinjeState(dager))
        }

        override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
            popState()
        }
    }

    private inner class VedtaksperiodeState(
        private val arbeidsgiverState: ArbeidsgiverState,
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
        private val inntekter: List<Inntektshistorikk.Inntektsendring>,
        private val utbetalinger: List<Utbetaling>,
        private val hendelseIder: List<UUID>,
        private val inntektsmeldingId: UUID?,
        private val nøkkeldataOmInntekter: MutableList<NøkkeldataOmInntekt>
    ) : PersonVisitor {

        private val fullstendig = tilstand.type in listOf(
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.UTBETALING_FEILET,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVVENTER_ARBEIDSGIVERE
        )
        private val hendelser = finnHendelser(hendelseIder)
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
        private val nøkkeldataOmInntekt = NøkkeldataOmInntekt(sisteDagISammenhengendePeriode = periode.endInclusive).also {
            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                it.skjæringstidspunkt = skjæringstidspunkt
            }
        }
        private val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
        private var forbrukteSykedager: Int? = null
        private var gjenståendeSykedager: Int? = null
        private var maksdato: LocalDate = LocalDate.MAX
        private var godkjentAv: String? = null
        private var godkjenttidspunkt: LocalDateTime? = null
        private var automatiskBehandling: Boolean = false

        private fun build(): VedtaksperiodeDTOBase {
            val utbetaling = arbeidsgiverFagsystemId?.let {
                utbetalinger.filter { utbetaling -> utbetaling.arbeidsgiverOppdrag().fagsystemId() == arbeidsgiverFagsystemId }
                    .kronologisk()
                    .reversed()
                    .firstOrNull()
            }
            val tilstandstypeDTO = buildTilstandtypeDto(utbetaling)

            if (!fullstendig) return buildUfullstendig(tilstandstypeDTO)

            val totalbeløpArbeidstaker = totalbeløpakkumulator.sum()
            val utbetalteUtbetalinger = byggUtbetalteUtbetalingerForPeriode()
            return buildFullstendig(tilstandstypeDTO, totalbeløpArbeidstaker, utbetalteUtbetalinger)
        }

        private fun buildFullstendig(tilstandstypeDTO: TilstandstypeDTO, totalbeløpArbeidstaker: Int, utbetalteUtbetalinger: UtbetalingerDTO): VedtaksperiodeDTO {
            nøkkeldataOmInntekter.add(nøkkeldataOmInntekt)
            val tom = beregnetSykdomstidslinje.last().dagen
            val vilkår = buildVilkår()
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
                hendelser = hendelser,
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

        private fun buildUfullstendig(tilstandstypeDTO: TilstandstypeDTO): UfullstendigVedtaksperiodeDTO {
            val ufullstendingUtbetalingstidslinje = buildUfullstendigUtbetalingstidslinje()
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

        private fun buildUfullstendigUtbetalingstidslinje() =
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

        private fun buildVilkår(): VilkårDTO {
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

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            pushState(SykdomshistorikkState())
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

            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                nøkkeldataOmInntekt.avviksprosent = dataForVilkårsvurdering.avviksprosent?.prosent()
            }
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
            arbeidsgiverState.vedtaksperiode(build())
            popState()
        }

        private fun byggUtbetalteUtbetalingerForPeriode(): UtbetalingerDTO =
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

    private inner class UtbetalingstidslinjeState(
        private val utbetalingstidslinjeMap: MutableList<UtbetalingstidslinjedagDTO>,
        private var utbetalinger: MutableList<Int>
    ) : PersonVisitor {

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            økonomi.reflectionRounded { _, aktuellDagsinntekt ->
                utbetalingstidslinjeMap.add(
                    UtbetalingsdagDTO(
                        type = TypeDataDTO.Arbeidsdag,
                        inntekt = aktuellDagsinntekt!!,
                        dato = dato
                    )
                )
            }
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            økonomi.reflectionRounded { _, aktuellDagsinntekt ->
                utbetalingstidslinjeMap.add(
                    UtbetalingsdagDTO(
                        type = TypeDataDTO.ArbeidsgiverperiodeDag,
                        inntekt = aktuellDagsinntekt!!,
                        dato = dato
                    )
                )
            }
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            økonomi.reflection { grad, _, _, _, totalGrad, aktuellDagsinntekt, arbeidsgiverbeløp, _, _ ->
                utbetalingstidslinjeMap.add(
                    NavDagDTO(
                        type = TypeDataDTO.NavDag,
                        inntekt = aktuellDagsinntekt!!.roundToInt(),
                        dato = dato,
                        utbetaling = arbeidsgiverbeløp!!,
                        grad = grad,
                        totalGrad = totalGrad
                    )
                )
                utbetalinger.add(arbeidsgiverbeløp)
            }
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            økonomi.reflection { grad, _ ->
                utbetalingstidslinjeMap.add(
                    UtbetalingsdagMedGradDTO(
                        type = TypeDataDTO.NavHelgDag,
                        inntekt = 0,   // Speil needs zero here
                        dato = dato,
                        grad = grad
                    )
                )
            }
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = if (dato.erHelg()) TypeDataDTO.Helgedag else TypeDataDTO.Feriedag,
                    inntekt = 0,    // Speil needs zero here
                    dato = dato
                )
            )
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.UkjentDag,
                    inntekt = 0,    // Speil needs zero here
                    dato = dato
                )
            )
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            utbetalingstidslinjeMap.add(
                AvvistDagDTO(
                    type = TypeDataDTO.AvvistDag,
                    inntekt = 0,    // Speil needs zero here
                    dato = dato,
                    begrunnelse = BegrunnelseDTO.valueOf(dag.begrunnelse.name),
                    grad = 0.0 // Speil wants zero here
                )
            )
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.ForeldetDag,
                    inntekt = 0,    // Speil needs zero here
                    dato = dato
                )
            )
        }

        override fun postVisit(tidslinje: Utbetalingstidslinje) {
            popState()
        }
    }

    private inner class SykdomshistorikkState : PersonVisitor {

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private open inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>) :
        PersonVisitor {

        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
            leggTilDag(dag, kilde)

        private fun leggTilDag(dag: Dag, kilde: Hendelseskilde) {
            sykdomstidslinjeListe.add(tilSpeilSykdomstidslinjedag(dag, kilde))
        }

        override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            popState()
        }
    }

    internal class NøkkeldataOmInntekt(
        val sisteDagISammenhengendePeriode: LocalDate
    ) {
        lateinit var skjæringstidspunkt: LocalDate
        var avviksprosent: Double? = null
    }

    private class Utbetalingshistorikkladd(
        var hendelsetidslinje: MutableList<SykdomstidslinjedagDTO> = mutableListOf(),
        var beregnettidslinje: MutableList<SykdomstidslinjedagDTO> = mutableListOf(),
        var utbetalinger: MutableList<UtbetalingKladd> = mutableListOf()
    ) {
        data class UtbetalingKladd(
            val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>
        )

        companion object {
            fun List<Utbetalingshistorikkladd>.tilDto() = this.map {
                UtbetalingshistorikkElementDTO(
                    hendelsetidslinje = it.hendelsetidslinje,
                    beregnettidslinje = it.beregnettidslinje,
                    utbetalinger = it.utbetalinger.map { utbetaling -> UtbetalingshistorikkElementDTO.UtbetalingDTO(utbetaling.utbetalingstidslinje) }
                )
            }
        }
    }

}

