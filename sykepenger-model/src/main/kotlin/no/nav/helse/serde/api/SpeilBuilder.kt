package no.nav.helse.serde.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.*
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.util.*


fun serializePersonForSpeil(person: Person): Pair<ObjectNode, Set<UUID>> {
    val jsonBuilder = SpeilBuilder()
    person.accept(jsonBuilder)
    return jsonBuilder.toJson() to jsonBuilder.hendelseReferanser
}

internal class SpeilBuilder : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()
    internal val hendelseReferanser = mutableSetOf<UUID>()

    init {
        stack.push(Root())
    }

    internal fun toJson() = currentState.toJson()

    private val currentState: JsonState
        get() = stack.peek()


    override fun toString() = currentState.toJson().toPrettyString()

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

    override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) =
        currentState.visitPersonAktivitetslogger(aktivitetslogger)

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

    override fun visitArbeidsgiverAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
        currentState.visitArbeidsgiverAktivitetslogger(aktivitetslogger)
    }

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()
    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun preVisitInntekter() = currentState.preVisitInntekter()
    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
        hendelseReferanser.add(inntekt.hendelseId)
        currentState.visitInntekt(inntekt)
    }

    override fun preVisitTidslinjer() = currentState.preVisitTidslinjer()
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


    override fun postVisitUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) =
        currentState.postVisitUtbetalingstidslinje(utbetalingstidslinje)

    override fun preVisitPerioder() = currentState.preVisitPerioder()
    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
        currentState.preVisitVedtaksperiode(vedtaksperiode, id)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
        hendelseReferanser.add(element.hendelseId)
        currentState.preVisitSykdomshistorikkElement(element)
    }

    override fun preVisitHendelseSykdomstidslinje() = currentState.preVisitHendelseSykdomstidslinje()
    override fun postVisitHendelseSykdomstidslinje() = currentState.postVisitHendelseSykdomstidslinje()
    override fun preVisitBeregnetSykdomstidslinje() = currentState.preVisitBeregnetSykdomstidslinje()
    override fun postVisitBeregnetSykdomstidslinje() = currentState.postVisitBeregnetSykdomstidslinje()
    override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.preVisitComposite(compositeSykdomstidslinje)

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.postVisitComposite(compositeSykdomstidslinje)

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) =
        currentState.postVisitSykdomshistorikkElement(element)

    override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) =
        currentState.postVisitVedtaksperiode(vedtaksperiode, id)

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = currentState.visitArbeidsdag(arbeidsdag)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = currentState.visitEgenmeldingsdag(egenmeldingsdag)
    override fun visitFeriedag(feriedag: Feriedag) = currentState.visitFeriedag(feriedag)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = currentState.visitImplisittDag(implisittDag)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = currentState.visitPermisjonsdag(permisjonsdag)
    override fun visitStudiedag(studiedag: Studiedag) = currentState.visitStudiedag(studiedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = currentState.visitSykHelgedag(sykHelgedag)
    override fun visitSykedag(sykedag: Sykedag) = currentState.visitSykedag(sykedag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = currentState.visitUbestemt(ubestemtdag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = currentState.visitUtenlandsdag(utenlandsdag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(tilstand)

    override fun preVisitVedtaksperiodeSykdomstidslinje() = currentState.preVisitVedtaksperiodeSykdomstidslinje()
    override fun postVisitVedtaksperiodeSykdomstidslinje() = currentState.postVisitVedtaksperiodeSykdomstidslinje()
    override fun preVisitUtbetalingslinjer() = currentState.preVisitUtbetalingslinjer()
    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        currentState.visitUtbetalingslinje(utbetalingslinje)

    override fun postVisitUtbetalingslinjer() = currentState.postVisitUtbetalingslinjer()

    private interface JsonState : PersonVisitor {
        fun entering() {}
        fun leaving() {}
        fun toJson(): ObjectNode =
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

        override fun toJson() = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
            .valueToTree<ObjectNode>(personMap)
    }

    private inner class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toSpeilMap())
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
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toSpeilMap())
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()
        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperioder.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))

        }

        override fun preVisitTidslinjer() {
            //arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            vedtaksperioder.forEach { periodeMap ->
                val førsteFraværsdag = periodeMap["førsteFraværsdag"] as LocalDate
                val sisteSykedag =
                    (periodeMap["sykdomstidslinje"] as List<Map<String, Any?>>).map { it["dagen"] as LocalDate }.max()!!
                periodeMap["utbetalingstidslinje"] =
                    (utbetalingstidslinjer.last()["dager"] as List<MutableMap<String, Any?>>).filterNot {
                        (it["dato"] as LocalDate)
                            .isBefore(førsteFraværsdag) || (it["dato"] as LocalDate).isAfter(sisteSykedag)
                    }
            }
            popState()
        }
    }

    private inner class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) : JsonState {

        private val dager = mutableListOf<MutableMap<String, Any?>>()

        init {
            utbetalingstidslinjeMap["dager"] = dager
        }

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            dager.add(UtbetalingsdagReflect(dag, "Arbeidsdag").toMap())
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            dager.add(UtbetalingsdagReflect(dag, "ArbeidsgiverperiodeDag").toMap())
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            dager.add(NavDagReflect(dag, "NavDag").toMap())
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            dager.add(UtbetalingsdagReflect(dag, "NavHelgDag").toMap())
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            dager.add(UtbetalingsdagReflect(dag, "Fridag").toMap())
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            dager.add(UtbetalingsdagReflect(dag, "UkjentDag").toMap())
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            val avvistDagMap = mutableMapOf<String, Any?>()
            dager.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun postVisitUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) {
            popState()
        }
    }


    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toSpeilMap())
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitVedtaksperiodeSykdomstidslinje() {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitUtbetalingslinjer() {
            val utbetalingstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["utbetalingslinjer"] = utbetalingstidslinjeListe
            pushState(UtbetalingslinjeState(utbetalingstidslinjeListe))
        }

        override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            popState()
        }
    }

    private inner class UtbetalingslinjeState(private val utbetalingstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {
        override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>(
                "fom" to utbetalingslinje.fom,
                "tom" to utbetalingslinje.tom,
                "dagsats" to utbetalingslinje.dagsats
            )
            utbetalingstidslinjeListe.add(utbetalingstidslinjeMap)
        }

        override fun postVisitUtbetalingslinjer() {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = leggTilDag(JsonDagType.ARBEIDSDAG, arbeidsdag)
        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = leggTilDag(JsonDagType.EGENMELDINGSDAG, egenmeldingsdag)
        override fun visitFeriedag(feriedag: Feriedag) = leggTilDag(JsonDagType.FERIEDAG, feriedag)
        override fun visitImplisittDag(implisittDag: ImplisittDag) = leggTilDag(JsonDagType.IMPLISITT_DAG, implisittDag)
        override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = leggTilDag(JsonDagType.PERMISJONSDAG, permisjonsdag)
        override fun visitStudiedag(studiedag: Studiedag) = leggTilDag(JsonDagType.STUDIEDAG, studiedag)
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = leggTilDag(JsonDagType.SYK_HELGEDAG, sykHelgedag)
        override fun visitSykedag(sykedag: Sykedag) = leggTilDag(JsonDagType.SYKEDAG, sykedag)
        override fun visitUbestemt(ubestemtdag: Ubestemtdag) = leggTilDag(JsonDagType.UBESTEMTDAG, ubestemtdag)
        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = leggTilDag(JsonDagType.UTENLANDSDAG, utenlandsdag)

        private fun leggTilDag(jsonDagType: JsonDagType, dag: Dag) {
            sykdomstidslinjeListe.add(
                mutableMapOf(
                    "dagen" to dag.dagen,
                    "hendelseType" to dag.hendelseType,
                    "type" to jsonDagType.name
                )
            )
        }

        override fun postVisitHendelseSykdomstidslinje() {
            popState()
        }

        override fun postVisitBeregnetSykdomstidslinje() {
            popState()
        }

        override fun postVisitVedtaksperiodeSykdomstidslinje() {
            popState()
        }
    }
}
