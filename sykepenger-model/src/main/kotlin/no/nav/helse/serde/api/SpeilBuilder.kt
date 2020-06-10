package no.nav.helse.serde.api

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.serde.api.SykdomstidslinjedagDTO.KildeDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
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
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.prosent
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt


private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

fun serializePersonForSpeil(person: Person, hendelser: List<HendelseDTO> = emptyList()): PersonDTO {
    val jsonBuilder = SpeilBuilder(hendelser)
    person.accept(jsonBuilder)
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

internal class SpeilBuilder(private val hendelser: List<HendelseDTO>) : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()
    private val rootState = Root()
    private val currentState: JsonState get() = stack.peek()

    init {
        stack.push(rootState)
    }

    internal fun toJson() = rootState.toJson()

    private fun pushState(state: JsonState) {
        stack.push(state)
    }

    private fun popState() {
        stack.pop()
    }

    override fun preVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.preVisitPerson(person, aktørId, fødselsnummer)

    override fun visitWarn(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Warn,
        melding: String,
        tidsstempel: String
    ) = currentState.visitWarn(kontekster, aktivitet, melding, tidsstempel)

    override fun postVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.postVisitPerson(person, aktørId, fødselsnummer)

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) = currentState.preVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) = currentState.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()

    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
        currentState.visitInntekt(inntekt, id)
    }

    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) =
        currentState.preVisitTidslinjer(tidslinjer)

    override fun preVisit(tidslinje: Utbetalingstidslinje) =
        currentState.preVisit(tidslinje)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi,
        aktuellDagsinntekt: Double
    ) =
        currentState.visit(dag, dato, økonomi, aktuellDagsinntekt)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi,
        aktuellDagsinntekt: Double
    ) =
        currentState.visit(dag, dato, økonomi, aktuellDagsinntekt)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ) =
        currentState.visit(
            dag,
            dato,
            økonomi,
            grad,
            aktuellDagsinntekt,
            dekningsgrunnlag,
            arbeidsgiverbeløp,
            personbeløp
        )

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel
    ) =
        currentState.visit(dag, dato, økonomi, grad)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ) =
        currentState.visit(
            dag,
            dato,
            økonomi,
            grad,
            aktuellDagsinntekt,
            dekningsgrunnlag,
            arbeidsgiverbeløp,
            personbeløp
        )

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun postVisit(tidslinje: Utbetalingstidslinje) =
        currentState.postVisit(tidslinje)

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) =
        currentState.preVisitPerioder(vedtaksperioder)

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) =
        currentState.postVisitPerioder(vedtaksperioder)

    override fun preVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) =
        currentState.preVisitForkastedePerioder(vedtaksperioder)

    override fun postVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) =
        currentState.postVisitForkastedePerioder(vedtaksperioder)

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode
    ) {
        currentState.preVisitVedtaksperiode(
            vedtaksperiode,
            id,
            gruppeId,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            periode
        )
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        currentState.postVisitUtbetalinger(utbetalinger)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        currentState.postVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) {
        currentState.preVisitSykdomshistorikkElement(element, id, tidsstempel)
    }

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitBeregnetSykdomstidslinje(tidslinje)

    override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitBeregnetSykdomstidslinje(tidslinje)

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) =
        currentState.postVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        currentState.visitDataForSimulering(dataForSimuleringResultat)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode
    ) =
        currentState.postVisitVedtaksperiode(
            vedtaksperiode,
            id,
            gruppeId,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            periode
        )

    override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(
        dag: Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)

    override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(
        dag: ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)

    override fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)

    override fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)

    override fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)

    override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
        currentState.visitDag(dag, dato, kilde, melding)

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) = currentState.visitTilstand(tilstand)

    private interface JsonState : PersonVisitor {}

    private inner class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(
            person: Person,
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
    ) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toSpeilMap())
        }

        private val arbeidsgivere = mutableListOf<ArbeidsgiverDTO>()
        private val aktivitetslogg = mutableListOf<AktivitetDTO>()

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

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
                    aktivitetslogg.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
                }
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap, fødselsnummer, arbeidsgivere, aktivitetslogg))
        }

        override fun postVisitPerson(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ) {
            popState()
        }
    }

    private inner class ArbeidsgiverState(
        private val arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>,
        private val fødselsnummer: String,
        private val arbeidsgivere: MutableList<ArbeidsgiverDTO>,
        private val aktivitetslogg: List<AktivitetDTO>
    ) : JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toSpeilMap())
        }

        private val vedtaksperioder = mutableListOf<VedtaksperiodeDTOBase>()
        lateinit var utbetalinger: List<Utbetaling>
        var vedtaksperiodeMap = mutableMapOf<String, Any?>()
        val fellesGrunnlagsdata = mutableMapOf<UUID, GrunnlagsdataDTO>()
        val fellesOpptjening = mutableMapOf<UUID, OpptjeningDTO>()
        var inntekter = mutableListOf<Inntekthistorikk.Inntekt>()


        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            this.utbetalinger = utbetalinger
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            this.vedtaksperioder.clear()
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] = this.vedtaksperioder.toList()
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {
            this.vedtaksperioder.clear()
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] =
                (arbeidsgiverMap["vedtaksperioder"] as List<Any?>) + this.vedtaksperioder.toList()
                    .filter { it.tilstand == TilstandstypeDTO.Utbetalt }
        }

        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
            inntekter.add(inntekt)
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode
        ) {
            pushState(
                VedtaksperiodeState(
                    vedtaksperiode = vedtaksperiode,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiodeMap = vedtaksperiodeMap,
                    fellesGrunnlagsdata = fellesGrunnlagsdata,
                    fellesOpptjening = fellesOpptjening,
                    vedtaksperioder = vedtaksperioder,
                    fødselsnummer = fødselsnummer,
                    inntekter = inntekter,
                    utbetalinger = utbetalinger,
                    aktivitetslogg = aktivitetslogg
                )
            )
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {

            val arbeidsgiverDTO = arbeidsgiverMap.mapTilArbeidsgiverDto()

            val mappedeArbeidsgivere = arbeidsgiverDTO.copy(
                vedtaksperioder = arbeidsgiverDTO.vedtaksperioder
                    .map { vedtaksperiode ->
                        if (vedtaksperiode.fullstendig) {
                            (vedtaksperiode as VedtaksperiodeDTO).copy(
                                dataForVilkårsvurdering = fellesGrunnlagsdata[vedtaksperiode.gruppeId],
                                vilkår = vedtaksperiode.vilkår.copy(opptjening = fellesOpptjening[vedtaksperiode.gruppeId])
                            )
                        } else {
                            vedtaksperiode
                        }
                    }
            )

            arbeidsgivere.add(mappedeArbeidsgivere)
            popState()
        }
    }

    private inner class VedtaksperiodeState(
        private val vedtaksperiode: Vedtaksperiode,
        arbeidsgiver: Arbeidsgiver,
        private val vedtaksperiodeMap: MutableMap<String, Any?>,
        private val fellesGrunnlagsdata: MutableMap<UUID, GrunnlagsdataDTO>,
        private val fellesOpptjening: MutableMap<UUID, OpptjeningDTO>,
        private val vedtaksperioder: MutableList<VedtaksperiodeDTOBase>,
        private val fødselsnummer: String,
        private val inntekter: List<Inntekthistorikk.Inntekt>,
        private val utbetalinger: List<Utbetaling>,
        aktivitetslogg: List<AktivitetDTO>
    ) : JsonState {
        private var fullstendig = false
        private val vedtaksperiodehendelser = mutableListOf<HendelseDTO>()
        private val beregnetSykdomstidslinje = mutableListOf<SykdomstidslinjedagDTO>()
        private val totalbeløpakkumulator = mutableListOf<Int>()
        private val dataForVilkårsvurdering = vedtaksperiode
            .get<Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")
            ?.let { mapDataForVilkårsvurdering(it) }

        private var arbeidsgiverFagsystemId: String?
        private var personFagsystemId: String?

        init {
            val vedtaksperiodeReflect = VedtaksperiodeReflect(vedtaksperiode)
            vedtaksperiodeMap.putAll(vedtaksperiodeReflect.toSpeilMap(arbeidsgiver))
            vedtaksperiodeMap["sykdomstidslinje"] = beregnetSykdomstidslinje
            vedtaksperiodeMap["hendelser"] = vedtaksperiodehendelser
            vedtaksperiodeMap["dataForVilkårsvurdering"] = dataForVilkårsvurdering
            vedtaksperiodeMap["aktivitetslogg"] =
                aktivitetslogg.filter { it.vedtaksperiodeId == vedtaksperiodeReflect.id }.distinctBy { it.melding }
            arbeidsgiverFagsystemId = vedtaksperiodeReflect.arbeidsgiverFagsystemId
            personFagsystemId = vedtaksperiodeReflect.personFagsystemId
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            pushState(SykdomshistorikkState(vedtaksperiodehendelser, beregnetSykdomstidslinje))
        }

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID?,
            tidsstempel: LocalDateTime
        ) {
            if (id == null) return
            hendelser.find { it.id == id.toString() }?.also { vedtaksperiodehendelser.add(it) }
        }

        private var førsteSykepengedag: LocalDate? = null
        private var sisteSykepengedag: LocalDate? = null

        override fun preVisit(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            førsteSykepengedag = tidslinje.førsteSykepengedag()
            sisteSykepengedag = tidslinje.sisteSykepengedag()
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje, totalbeløpakkumulator))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] =
                mapTilstander(tilstand = tilstand.type, utbetalt = (totalbeløpakkumulator.sum() > 0))
            if (tilstand.type in listOf(
                    TilstandType.AVSLUTTET,
                    TilstandType.AVVENTER_GODKJENNING,
                    TilstandType.UTBETALING_FEILET,
                    TilstandType.TIL_UTBETALING
                )
            ) {
                fullstendig = true
            } else {
                vedtaksperioder.add(vedtaksperiodeMap.mapTilUfullstendigVedtaksperiodeDto())
            }
        }

        override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
            dataForSimuleringResultat?.let {
                vedtaksperiodeMap["dataForSimulering"] = it.mapTilSimuleringsdataDto()
            }
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode
        ) {
            vedtaksperiodeMap["totalbeløpArbeidstaker"] = totalbeløpakkumulator.sum()
            vedtaksperiodeMap["utbetalinger"] = byggUtbetalingerForPeriode()
            if (fullstendig) {
                vedtaksperioder.add(
                    vedtaksperiodeMap.mapTilVedtaksperiodeDto(
                        fødselsnummer,
                        inntekter,
                        førsteSykepengedag,
                        sisteSykepengedag
                    )
                )
            }
            samleFellesdata(gruppeId)
            popState()
        }

        private fun samleFellesdata(gruppeId: UUID) {
            val førsteFraværsdag = vedtaksperiodeMap["førsteFraværsdag"] as? LocalDate
            val opptjening = førsteFraværsdag?.let {
                dataForVilkårsvurdering?.let {
                    mapOpptjening(førsteFraværsdag, it)
                }
            }
            dataForVilkårsvurdering?.let { fellesGrunnlagsdata.putIfAbsent(gruppeId, it) }
            opptjening?.let { fellesOpptjening.putIfAbsent(gruppeId, it) }
        }

        private fun byggUtbetalingerForPeriode(): UtbetalingerDTO =
            UtbetalingerDTO(
                utbetalinger.byggUtbetaling(
                    arbeidsgiverFagsystemId,
                    Utbetaling::arbeidsgiverOppdrag
                ),
                utbetalinger.byggUtbetaling(
                    personFagsystemId,
                    Utbetaling::personOppdrag
                )
            )

        private fun List<Utbetaling>.byggUtbetaling(fagsystemId: String?, oppdragStrategy: (Utbetaling) -> Oppdrag) =
            fagsystemId?.let { fagsystemId ->
                this.lastOrNull() { utbetaling ->
                    oppdragStrategy(utbetaling).fagsystemId() == fagsystemId
                }?.let { utbetaling ->
                    val linjer = oppdragStrategy(utbetaling).linjerUtenOpphør().map { linje ->
                        UtbetalingerDTO.UtbetalingslinjeDTO(
                            fom = linje.fom,
                            tom = linje.tom,
                            dagsats = linje.beløp,
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
    ) : JsonState {

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi,
            aktuellDagsinntekt: Double
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.Arbeidsdag,
                    inntekt = aktuellDagsinntekt.roundToInt(),
                    dato = dato
                )
            )
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi,
            aktuellDagsinntekt: Double
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagDTO(
                    type = TypeDataDTO.ArbeidsgiverperiodeDag,
                    inntekt = aktuellDagsinntekt.roundToInt(),
                    dato = dato
                )
            )
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            aktuellDagsinntekt: Double,
            dekningsgrunnlag: Double,
            arbeidsgiverbeløp: Int,
            personbeløp: Int
        ) {
            utbetalingstidslinjeMap.add(
                NavDagDTO(
                    type = TypeDataDTO.NavDag,
                    inntekt = aktuellDagsinntekt.roundToInt(),
                    dato = dato,
                    utbetaling = arbeidsgiverbeløp,
                    grad = grad.toDouble()
                )
            )
            utbetalinger.add(arbeidsgiverbeløp)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel
        ) {
            utbetalingstidslinjeMap.add(
                UtbetalingsdagMedGradDTO(
                    type = TypeDataDTO.NavHelgDag,
                    inntekt = 0,   // Speil needs zero here
                    dato = dato,
                    grad = grad.toDouble()
                )
            )
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
            økonomi: Økonomi,
            grad: Prosentdel,
            aktuellDagsinntekt: Double,
            dekningsgrunnlag: Double,
            arbeidsgiverbeløp: Int,
            personbeløp: Int
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

    private inner class SykdomshistorikkState(
        private val vedtaksperiodehendelser: MutableList<HendelseDTO>,
        private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>
    ) : JsonState {

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID?,
            tidsstempel: LocalDateTime
        ) {
            hendelser.find { it.id == id.toString() }?.also { vedtaksperiodehendelser.add(it) }
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID?,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>) :
        JsonState {

        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, dato, kilde, grad)

        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, dato, kilde, grad)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, dato, kilde, grad)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, dato, kilde, grad)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, dato, kilde, grad)

        override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, dato, kilde)

        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
            leggTilDag(dag, dato, kilde)

        private fun leggTilDag(
            dag: Dag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            grad: Prosentdel = 0.prosent
        ) {
            sykdomstidslinjeListe.add(
                SykdomstidslinjedagDTO(dato, dag.toSpeilDagtype(), KildeDTO(kilde.toSpeilKildetype()), grad.toDouble())
            )
        }

        private fun Dag.toSpeilDagtype() = when (this) {
            is Sykedag -> SpeilDagtype.SYKEDAG
            is Arbeidsdag, is UkjentDag -> SpeilDagtype.ARBEIDSDAG
            is Arbeidsgiverdag, is ArbeidsgiverHelgedag -> SpeilDagtype.ARBEIDSGIVERDAG
            is Feriedag -> SpeilDagtype.FERIEDAG
            is FriskHelgedag -> SpeilDagtype.FRISK_HELGEDAG
            is ForeldetSykedag -> SpeilDagtype.FORELDET_SYKEDAG
            is SykHelgedag -> SpeilDagtype.SYK_HELGEDAG
            is Permisjonsdag -> SpeilDagtype.PERMISJONSDAG
            is Studiedag -> SpeilDagtype.STUDIEDAG
            is Utenlandsdag -> SpeilDagtype.UTENLANDSDAG
            is ProblemDag -> SpeilDagtype.UBESTEMTDAG
        }

        private fun Hendelseskilde.toSpeilKildetype() = when {
            erAvType(Inntektsmelding::class) -> SpeilKildetype.Inntektsmelding
            erAvType(Søknad::class) -> SpeilKildetype.Søknad
            erAvType(Sykmelding::class) -> SpeilKildetype.Sykmelding
            else -> SpeilKildetype.Ukjent
        }

        override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            popState()
        }
    }
}
