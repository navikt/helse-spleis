package no.nav.helse.serde.api

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.api.SpeilBuilder.Utbetalingshistorikkladd.Companion.tilDto
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.reflection.ArbeidsgiverReflect
import no.nav.helse.serde.reflection.PersonReflect
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.VedtaksperiodeReflect
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.kronologisk
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt


private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(person, hendelser)
    return jsonBuilder.toJson().also {
        it.arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>().forEach { vedtaksperiode ->
                vedtaksperiode.vilkår.sykepengegrunnlag ?: sikkerLogg.info(
                    "Sykepengegrunnlag er null, SpeilPerson-json: {}",
                    serdeObjectMapper.writeValueAsString(it)
                )
            }
        }
    }
}

internal class SpeilBuilder(person: Person, private val hendelser: List<HendelseDTO>) {

    private val rootState = Root()
    private val stack = mutableListOf<PersonVisitor>(rootState)
    private val hendelsePerioder = mutableMapOf<UUID, Periode>()

    init {
        person.accept(DelegatedPersonVisitor(stack::first))
    }

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
            hendelseIder: List<UUID>
        ) {
            gruppeIder[vedtaksperiode] = arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
                ?.let(gruppeIder::getValue)
                ?: UUID.randomUUID()
            pushState(
                VedtaksperiodeState(
                    vedtaksperiode = vedtaksperiode,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperioder = vedtaksperioder,
                    gruppeId = gruppeIder.getValue(vedtaksperiode),
                    fødselsnummer = fødselsnummer,
                    inntekter = inntekter,
                    utbetalinger = utbetalinger.flatMap { it.value },
                    hendelseIder = hendelseIder,
                    periode = periode,
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
        vedtaksperiode: Vedtaksperiode,
        arbeidsgiver: Arbeidsgiver,
        private val periode: Periode,
        private val vedtaksperioder: MutableList<VedtaksperiodeDTOBase>,
        private val gruppeId: UUID,
        private val fødselsnummer: String,
        private val inntekter: List<Inntektshistorikk.Inntektsendring>,
        private val utbetalinger: List<Utbetaling>,
        private val hendelseIder: List<UUID>,
        private val nøkkeldataOmInntekter: MutableList<NøkkeldataOmInntekt>
    ) : PersonVisitor {
        private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()
        private val totalbeløpakkumulator = mutableListOf<Int>()
        private val dataForVilkårsvurdering = vedtaksperiode
            .get<Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")
            ?.let { mapDataForVilkårsvurdering(it) }


        private val vedtaksperiodeMap = mutableMapOf<String, Any?>()
        private var arbeidsgiverFagsystemId: String? = null
        private var personFagsystemId: String? = null
        private var inUtbetaling = false
        private var utbetalingGodkjent = false
        private val nøkkeldataOmInntekt = NøkkeldataOmInntekt(sisteDagISammenhengendePeriode = periode.endInclusive)

        init {
            val vedtaksperiodeReflect = VedtaksperiodeReflect(vedtaksperiode)
            vedtaksperiodeMap.putAll(vedtaksperiodeReflect.toSpeilMap(arbeidsgiver, periode))
            vedtaksperiodeMap["sykdomstidslinje"] = beregnetSykdomstidslinje
            vedtaksperiodeMap["hendelser"] = vedtaksperiodehendelser()
            vedtaksperiodeMap["dataForVilkårsvurdering"] = dataForVilkårsvurdering
            vedtaksperiodeMap["aktivitetslogg"] = hentWarnings(vedtaksperiode)
            vedtaksperiodeMap["periodetype"] = vedtaksperiode.periodetype()
            vedtaksperiodeMap["maksdato"] = LocalDate.MAX
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
            val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            førsteSykepengedag = tidslinje.førsteSykepengedag()
            sisteSykepengedag = tidslinje.sisteSykepengedag()
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje, totalbeløpakkumulator))
        }

        override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
            dataForSimuleringResultat?.let {
                vedtaksperiodeMap["dataForSimulering"] = it.mapTilSimuleringsdataDto()
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
            inUtbetaling = true
            utbetalingGodkjent = tilstand !in listOf(Utbetaling.IkkeGodkjent, Utbetaling.Ubetalt)
            vedtaksperiodeMap["maksdato"] = maksdato
            vedtaksperiodeMap["gjenståendeSykedager"] = gjenståendeSykedager
            vedtaksperiodeMap["forbrukteSykedager"] = forbrukteSykedager
        }

        override fun visitVurdering(vurdering: Utbetaling.Vurdering, ident: String, epost: String, tidspunkt: LocalDateTime, automatiskBehandling: Boolean) {
            if (!utbetalingGodkjent) return
            vedtaksperiodeMap["godkjentAv"] = ident
            vedtaksperiodeMap["godkjenttidspunkt"] = tidspunkt
            vedtaksperiodeMap["automatiskBehandling"] = automatiskBehandling
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

        override fun visitSkjæringstidspunkt(skjæringstidspunkt: LocalDate) {
            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                nøkkeldataOmInntekt.skjæringstidspunkt = skjæringstidspunkt
            }
        }

        override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {
            if (Toggles.SpeilInntekterVol2Enabled.enabled) {
                nøkkeldataOmInntekt.avviksprosent = dataForVilkårsvurdering?.avviksprosent?.prosent()
            }
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode
        ) {
            vedtaksperiodeMap["totalbeløpArbeidstaker"] = totalbeløpakkumulator.sum()
            vedtaksperiodeMap["utbetalteUtbetalinger"] = byggUtbetalteUtbetalingerForPeriode()

            val utbetaling = arbeidsgiverFagsystemId?.let {
                utbetalinger.filter { utbetaling -> utbetaling.arbeidsgiverOppdrag().fagsystemId() == arbeidsgiverFagsystemId }
                    .kronologisk()
                    .reversed()
                    .firstOrNull()
            }
            vedtaksperiodeMap["tilstand"] =
                mapTilstander(
                    tilstand = tilstand.type,
                    utbetalt = totalbeløpakkumulator.sum() > 0,
                    kunFerie = beregnetSykdomstidslinje.all { it.type == SpeilDagtype.FERIEDAG },
                    utbetaling = utbetaling
                )
            if (tilstand.type in listOf(
                    TilstandType.AVSLUTTET,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.UTBETALING_FEILET,
                    TilstandType.TIL_UTBETALING
                )
            ) {
                nøkkeldataOmInntekter.add(nøkkeldataOmInntekt)
                vedtaksperioder.add(
                    vedtaksperiodeMap.mapTilVedtaksperiodeDto(
                        fødselsnummer,
                        inntekter,
                        førsteSykepengedag,
                        sisteSykepengedag,
                        gruppeId
                    )
                )
            } else {
                vedtaksperiodeMap["utbetalingstidslinje"] = utbetalinger.tilUfullstendigVedtaksperiodetidslinje(periode)
                vedtaksperioder.add(vedtaksperiodeMap.mapTilUfullstendigVedtaksperiodeDto(gruppeId))
            }
            popState()
        }

        private fun vedtaksperiodehendelser(): MutableList<HendelseDTO> {
            return hendelseIder.map { it.toString() }.let { ids ->
                hendelser.filter { it.id in ids }.toMutableList()
            }
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
                    begrunnelse = mapBegrunnelse(dag.begrunnelse),
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

