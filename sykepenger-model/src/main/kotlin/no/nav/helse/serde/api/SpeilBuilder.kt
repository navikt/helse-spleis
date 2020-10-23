package no.nav.helse.serde.api

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.api.TilstandstypeDTO.*
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
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel
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
    private val hendelsePerioder = mutableMapOf<UUID, Periode>()

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

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) =
        currentState.preVisitInntekthistorikk(inntektshistorikk)

    override fun preVisitHendelseSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
        currentState.preVisitHendelseSykdomstidslinje(tidslinje, hendelseId, tidsstempel)
    }

    override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        currentState.postVisitInntekthistorikk(inntektshistorikk)
    }

    override fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, id: UUID) {
        currentState.visitInntekt(inntektsendring, id)
    }

    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) =
        currentState.preVisitTidslinjer(tidslinjer)

    override fun preVisit(tidslinje: Utbetalingstidslinje) =
        currentState.preVisit(tidslinje)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(
            dag,
            dato,
            økonomi
        )

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(dag, dato, økonomi)

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) =
        currentState.visit(
            dag,
            dato,
            økonomi
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

    override fun preVisitForkastedePerioder(vedtaksperioder: Map<Vedtaksperiode, ForkastetÅrsak>) =
        currentState.preVisitForkastedePerioder(vedtaksperioder)

    override fun postVisitForkastedePerioder(vedtaksperioder: Map<Vedtaksperiode, ForkastetÅrsak>) =
        currentState.postVisitForkastedePerioder(vedtaksperioder)

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode,
        opprinneligPeriode: Periode,
        hendelseIder: List<UUID>
    ) {
        currentState.preVisitVedtaksperiode(
            vedtaksperiode,
            id,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            periode,
            opprinneligPeriode,
            hendelseIder
        )
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        currentState.preVisitSykdomstidslinje(tidslinje, låstePerioder)
    }

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        currentState.postVisitSykdomstidslinje(tidslinje)
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
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode,
        opprinneligPeriode: Periode
    ) =
        currentState.postVisitVedtaksperiode(
            vedtaksperiode,
            id,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            periode,
            opprinneligPeriode
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
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, kilde)

    override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        currentState.visitDag(dag, dato, kilde)

    override fun visitDag(
        dag: ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, kilde)

    override fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, kilde)

    override fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, kilde)

    override fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        kilde: Hendelseskilde
    ) = currentState.visitDag(dag, dato, økonomi, grad, kilde)

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
            if (!arbeidsgiver.harHistorikk()) return
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
        private val gruppeIder = mutableMapOf<Vedtaksperiode, UUID>()
        lateinit var utbetalinger: List<Utbetaling>
        var vedtaksperiodeMap = mutableMapOf<String, Any?>()
        var inntekter = mutableListOf<Inntektshistorikk.Inntektsendring>()


        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            this.utbetalinger = utbetalinger
        }

        override fun preVisitHendelseSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            hendelseId?.also { hendelsePerioder[it] = tidslinje.periode()!! }
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            vedtaksperioder.forEach { periode ->
                gruppeIder[periode] = arbeidsgiver.finnSykeperiodeRettFør(periode)
                    ?.let { foregående -> gruppeIder.getValue(foregående) }
                    ?: UUID.randomUUID()
            }
            this.vedtaksperioder.clear()
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] = this.vedtaksperioder.toList()
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: Map<Vedtaksperiode, ForkastetÅrsak>) {
            vedtaksperioder.forEach { (periode, _) ->
                gruppeIder[periode] = arbeidsgiver.finnSykeperiodeRettFør(periode)
                    ?.let { foregående -> gruppeIder.getValue(foregående) }
                    ?: UUID.randomUUID()
            }
            this.vedtaksperioder.clear()
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: Map<Vedtaksperiode, ForkastetÅrsak>) {
            arbeidsgiverMap["vedtaksperioder"] =
                (arbeidsgiverMap["vedtaksperioder"] as List<Any?>) + this.vedtaksperioder.toList()
                    .filter { it.tilstand.visesNårForkastet() }
        }

        override fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, id: UUID) {
            inntekter.add(inntektsendring)
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode,
            opprinneligPeriode: Periode,
            hendelseIder: List<UUID>
        ) {
            pushState(
                VedtaksperiodeState(
                    vedtaksperiode = vedtaksperiode,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiodeMap = vedtaksperiodeMap,
                    vedtaksperioder = vedtaksperioder,
                    gruppeId = gruppeIder.getValue(vedtaksperiode),
                    fødselsnummer = fødselsnummer,
                    inntekter = inntekter,
                    utbetalinger = utbetalinger,
                    aktivitetslogg = aktivitetslogg,
                    hendelseIder = hendelseIder
                )
            )
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverDTO = arbeidsgiverMap.mapTilArbeidsgiverDto()
            arbeidsgivere.add(arbeidsgiverDTO)
            popState()
        }
    }

    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        arbeidsgiver: Arbeidsgiver,
        private val vedtaksperiodeMap: MutableMap<String, Any?>,
        private val vedtaksperioder: MutableList<VedtaksperiodeDTOBase>,
        private val gruppeId: UUID,
        private val fødselsnummer: String,
        private val inntekter: List<Inntektshistorikk.Inntektsendring>,
        private val utbetalinger: List<Utbetaling>,
        aktivitetslogg: List<AktivitetDTO>,
        private val hendelseIder: List<UUID>
    ) : JsonState {
        private var fullstendig = false
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
            vedtaksperiodeMap["hendelser"] = vedtaksperiodehendelser()
            vedtaksperiodeMap["dataForVilkårsvurdering"] = dataForVilkårsvurdering
            val tidligerePeriodeId = vedtaksperiode.foregåendeSomErBehandletUtenUtbetaling()
            vedtaksperiodeMap["aktivitetslogg"] =
                aktivitetslogg.filter { it.vedtaksperiodeId in listOf(vedtaksperiodeReflect.id, tidligerePeriodeId) }.distinctBy { it.melding }
            vedtaksperiodeMap["periodetype"] = vedtaksperiode.periodetype()
            arbeidsgiverFagsystemId = vedtaksperiodeReflect.arbeidsgiverFagsystemId
            personFagsystemId = vedtaksperiodeReflect.personFagsystemId
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
            val utbetalingstidslinje = mutableListOf<UtbetalingstidslinjedagDTO>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            førsteSykepengedag = tidslinje.førsteSykepengedag()
            sisteSykepengedag = tidslinje.sisteSykepengedag()
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje, totalbeløpakkumulator))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            val utbetaling = utbetalinger.findLast { arbeidsgiverFagsystemId != null && it.arbeidsgiverOppdrag().fagsystemId() ==arbeidsgiverFagsystemId }
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
                fullstendig = true
            } else {
                vedtaksperioder.add(vedtaksperiodeMap.mapTilUfullstendigVedtaksperiodeDto(gruppeId))
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
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode,
            opprinneligPeriode: Periode
        ) {
            vedtaksperiodeMap["totalbeløpArbeidstaker"] = totalbeløpakkumulator.sum()
            vedtaksperiodeMap["utbetalteUtbetalinger"] = byggUtbetalteUtbetalingerForPeriode()
            if (fullstendig) {
                vedtaksperioder.add(
                    vedtaksperiodeMap.mapTilVedtaksperiodeDto(
                        fødselsnummer,
                        inntekter,
                        førsteSykepengedag,
                        sisteSykepengedag,
                        gruppeId
                    )
                )
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
    ) : JsonState {

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
            økonomi.reflection { grad, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, _, _ ->
                utbetalingstidslinjeMap.add(
                    NavDagDTO(
                        type = TypeDataDTO.NavDag,
                        inntekt = aktuellDagsinntekt!!.roundToInt(),
                        dato = dato,
                        utbetaling = arbeidsgiverbeløp!!,
                        grad = grad
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

    private inner class SykdomshistorikkState : JsonState {

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private open inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<SykdomstidslinjedagDTO>) :
        JsonState {

        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
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
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) = leggTilDag(dag, kilde)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
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

}

