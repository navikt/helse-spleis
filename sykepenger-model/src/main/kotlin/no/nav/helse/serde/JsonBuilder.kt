package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.*
import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDateTime
import java.util.*

fun Person.serialize(): SerialisertPerson {
    val jsonBuilder = JsonBuilder()
    this.accept(jsonBuilder)
    return SerialisertPerson(jsonBuilder.toString())
}

internal class JsonBuilder : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()

    init {
        stack.push(Root())
    }

    internal fun toJson() = SerialisertPerson.medSkjemaversjon(currentState.toJson())

    private val currentState: JsonState
        get() = stack.peek()

    override fun toString() = toJson().toString()

    private fun pushState(state: JsonState) {
        currentState.leaving()
        stack.push(state)
        currentState.entering()
    }

    private fun popState() {
        currentState.leaving()
        stack.pop()
        currentState.entering()
    }

    override fun preVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.preVisitPerson(person, aktørId, fødselsnummer)

    override fun postVisitPerson(
        person: Person,
        aktørId: String,
        fødselsnummer: String
    ) = currentState.postVisitPerson(person, aktørId, fødselsnummer)

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) =
        currentState.visitPersonAktivitetslogg(aktivitetslogg)

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) =
        currentState.preVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) =
        currentState.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()
    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) = currentState.visitInntekt(inntekt)
    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) = currentState.preVisitTidslinjer(tidslinjer)
    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.preVisitUtbetalingstidslinje(tidslinje)

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) =
        currentState.visitArbeidsdag(dag)

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) =
        currentState.visitArbeidsgiverperiodeDag(dag)

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) =
        currentState.visitNavDag(dag)

    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) =
        currentState.visitNavHelgDag(dag)

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) =
        currentState.visitFridag(dag)

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) =
        currentState.visitAvvistDag(dag)

    override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) =
        currentState.visitUkjentDag(dag)

    override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) =
        currentState.visitForeldetDag(dag)

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.postVisitUtbetalingstidslinje(tidslinje)

    override fun preVisitPerioder() = currentState.preVisitPerioder()
    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID
    ) =
        currentState.preVisitVedtaksperiode(vedtaksperiode, id, gruppeId)

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.preVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitHendelseSykdomstidslinje(tidslinje)

    override fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitHendelseSykdomstidslinje(tidslinje)

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitBeregnetSykdomstidslinje(tidslinje)

    override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitBeregnetSykdomstidslinje(tidslinje)

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.preVisitSykdomstidslinje(tidslinje)

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) =
        currentState.postVisitSykdomstidslinje(tidslinje)

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.postVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID
    ) =
        currentState.postVisitVedtaksperiode(vedtaksperiode, id, gruppeId)

    override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) = currentState.visitArbeidsdag(dag)
    override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) = currentState.visitArbeidsdag(dag)
    override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) =
        currentState.visitEgenmeldingsdag(dag)

    override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) =
        currentState.visitEgenmeldingsdag(dag)

    override fun visitFeriedag(dag: Feriedag.Inntektsmelding) = currentState.visitFeriedag(dag)
    override fun visitFeriedag(dag: Feriedag.Søknad) = currentState.visitFeriedag(dag)
    override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) = currentState.visitFriskHelgedag(dag)
    override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) = currentState.visitFriskHelgedag(dag)

    override fun visitImplisittDag(dag: ImplisittDag) = currentState.visitImplisittDag(dag)
    override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) =
        currentState.visitPermisjonsdag(dag)

    override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) = currentState.visitPermisjonsdag(dag)
    override fun visitStudiedag(dag: Studiedag) = currentState.visitStudiedag(dag)
    override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) = currentState.visitSykHelgedag(dag)
    override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = currentState.visitSykHelgedag(dag)
    override fun visitSykedag(dag: Sykedag.Sykmelding) = currentState.visitSykedag(dag)
    override fun visitSykedag(dag: Sykedag.Søknad) = currentState.visitSykedag(dag)
    override fun visitForeldetSykedag(dag: ForeldetSykedag) = currentState.visitForeldetSykedag(dag)
    override fun visitUbestemt(dag: Ubestemtdag) = currentState.visitUbestemt(dag)
    override fun visitUtenlandsdag(dag: Utenlandsdag) = currentState.visitUtenlandsdag(dag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(tilstand)

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) =
        currentState.preVisitUtbetalinger(utbetalinger)

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) =
        currentState.postVisitUtbetalinger(utbetalinger)

    override fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) =
        currentState.preVisitUtbetaling(utbetaling, tidsstempel)

    private interface JsonState : PersonVisitor {
        fun entering() {}
        fun leaving() {}
        fun toJson(): JsonNode =
            throw RuntimeException("toJson() kan bare kalles på rotnode. Ble kalt på ${toString()}")
    }

    private inner class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(
            person: Person,
            aktørId: String,
            fødselsnummer: String
        ) {
            pushState(PersonState(person, personMap))
        }

        override fun toString() = personMap.toString()

        override fun toJson(): JsonNode = serdeObjectMapper.valueToTree<JsonNode>(personMap)
    }

    private inner class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toMap())
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personMap["aktivitetslogg"] = AktivitetsloggReflect(aktivitetslogg).toMap()
        }

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            arbeidsgivere.add(arbeidsgiverMap)
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap))
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
        arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>
    ) :
        JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toMap())
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            val inntekter = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["inntekter"] = inntekter
            pushState(InntektHistorieState(inntekter))
        }

        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
            arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            arbeidsgiverMap["utbetalinger"] = mutableListOf<MutableMap<String, Any?>>().also {
                pushState(UtbetalingerState(it))
            }
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID
        ) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperioder.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))

        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            popState()
        }
    }

    private inner class UtbetalingerState(private val utbetalinger: MutableList<MutableMap<String, Any?>>) : JsonState {

        override fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {
            utbetalinger.add(UtbetalingReflect(utbetaling).toMap())
        }

        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            popState()
        }
    }

    private inner class InntektHistorieState(private val inntekter: MutableList<MutableMap<String, Any?>>) : JsonState {
        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
            val inntektMap = mutableMapOf<String, Any?>()
            inntekter.add(inntektMap)

            inntektMap.putAll(InntektReflect(inntekt).toMap())
        }

        override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            popState()
        }
    }

    private inner class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) :
        JsonState {

        private val dager = mutableListOf<MutableMap<String, Any?>>()

        init {
            utbetalingstidslinjeMap["dager"] = dager
        }

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Arbeidsdag).toMap())
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.ArbeidsgiverperiodeDag).toMap())
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            dager.add(NavDagReflect(dag, TypeData.NavDag).toMap())
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            dager.add(UtbetalingsdagMedGradReflect(dag, TypeData.NavHelgDag).toMap())
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Fridag).toMap())
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.UkjentDag).toMap())
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            val avvistDagMap = mutableMapOf<String, Any?>()
            dager.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            val foreldetDagMap = mutableMapOf<String, Any?>()
            dager.add(foreldetDagMap)

            foreldetDagMap.putAll(UtbetalingsdagReflect(dag, TypeData.ForeldetDag).toMap())
        }

        override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) = popState()
    }

    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toMap())
        }

        private val sykdomshistorikkElementer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            vedtaksperiodeMap["sykdomshistorikk"] = sykdomshistorikkElementer
        }

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            pushState(SykdomshistorikkElementState(id, tidsstempel, elementMap))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinje = mutableMapOf<String, Any?>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje))
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID
        ) {
            popState()
        }
    }

    private inner class SykdomshistorikkElementState(
        id: UUID,
        tidsstempel: LocalDateTime,
        private val elementMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            elementMap["hendelseId"] = id
            elementMap["tidsstempel"] = tidsstempel
        }

        override fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {

        override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) =
            leggTilDag(JsonDagType.ARBEIDSDAG_INNTEKTSMELDING, dag)

        override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) =
            leggTilDag(JsonDagType.ARBEIDSDAG_SØKNAD, dag)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) =
            leggTilDag(JsonDagType.EGENMELDINGSDAG_INNTEKTSMELDING, dag)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) =
            leggTilDag(JsonDagType.EGENMELDINGSDAG_SØKNAD, dag)

        override fun visitFeriedag(dag: Feriedag.Inntektsmelding) =
            leggTilDag(JsonDagType.FERIEDAG_INNTEKTSMELDING, dag)

        override fun visitFeriedag(dag: Feriedag.Søknad) = leggTilDag(JsonDagType.FERIEDAG_SØKNAD, dag)

        override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) = leggTilDag(JsonDagType.FRISK_HELGEDAG_INNTEKTSMELDING, dag)
        override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) = leggTilDag(JsonDagType.FRISK_HELGEDAG_SØKNAD, dag)

        override fun visitImplisittDag(dag: ImplisittDag) = leggTilDag(JsonDagType.IMPLISITT_DAG, dag)
        override fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) =
            leggTilDag(JsonDagType.PERMISJONSDAG_SØKNAD, dag)

        override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) =
            leggTilDag(JsonDagType.PERMISJONSDAG_AAREG, dag)

        override fun visitStudiedag(dag: Studiedag) = leggTilDag(JsonDagType.STUDIEDAG, dag)
        override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) = leggTilSykedag(JsonDagType.SYK_HELGEDAG_SYKMELDING, dag)
        override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = leggTilSykedag(JsonDagType.SYK_HELGEDAG_SØKNAD, dag)
        override fun visitSykedag(dag: Sykedag.Sykmelding) = leggTilSykedag(JsonDagType.SYKEDAG_SYKMELDING, dag)
        override fun visitSykedag(dag: Sykedag.Søknad) = leggTilSykedag(JsonDagType.SYKEDAG_SØKNAD, dag)
        override fun visitForeldetSykedag(dag: ForeldetSykedag) = leggTilSykedag(JsonDagType.KUN_ARBEIDSGIVER_SYKEDAG, dag)
        override fun visitUbestemt(dag: Ubestemtdag) = leggTilDag(JsonDagType.UBESTEMTDAG, dag)
        override fun visitUtenlandsdag(dag: Utenlandsdag) = leggTilDag(JsonDagType.UTENLANDSDAG, dag)

        private fun leggTilDag(jsonDagType: JsonDagType, dag: Dag) {
            sykdomstidslinjeListe.add(
                mutableMapOf(
                    "dagen" to dag.dagen,
                    "type" to jsonDagType.name
                )
            )
        }

        private fun leggTilSykedag(jsonDagType: JsonDagType, dag: GradertDag) {
            sykdomstidslinjeListe.add(
                mutableMapOf(
                    "dagen" to dag.dagen,
                    "type" to jsonDagType.name,
                    "grad" to dag.grad
                )
            )
        }

        override fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            popState()
        }

        override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            popState()
        }
    }
}
