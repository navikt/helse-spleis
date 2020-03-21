package no.nav.helse.serde.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor
import java.util.*


fun serializePersonForSpeil(person: Person): Pair<ObjectNode, Set<UUID>> {
    val hendelseReferanser = mutableSetOf<UUID>()
    val jsonBuilder = SpeilBuilder(hendelseReferanser)
    person.accept(jsonBuilder)
    return jsonBuilder.toJson() to hendelseReferanser
}

internal class SpeilBuilder(private val hendelser: MutableSet<UUID> = mutableSetOf()) : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()
    private val rootState = Root()
    private val currentState: JsonState get() = stack.peek()

    init {
        stack.push(rootState)
    }

    internal fun toJson() = rootState.toJson()

    override fun toString() = rootState.toJson().toPrettyString()

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
    ) = currentState.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere()
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere()
    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) =
        currentState.preVisitInntekthistorikk(inntekthistorikk)

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        currentState.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
        hendelser.add(inntekt.hendelseId)
        currentState.visitInntekt(inntekt)
    }

    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) =
        currentState.preVisitTidslinjer(tidslinjer)

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

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        currentState.postVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
        hendelser.add(element.hendelseId)
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

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) = currentState.visitArbeidsdag(arbeidsdag)
    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) = currentState.visitArbeidsdag(arbeidsdag)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) =
        currentState.visitEgenmeldingsdag(egenmeldingsdag)

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) =
        currentState.visitEgenmeldingsdag(egenmeldingsdag)

    override fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) = currentState.visitFeriedag(feriedag)
    override fun visitFeriedag(feriedag: Feriedag.Søknad) = currentState.visitFeriedag(feriedag)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = currentState.visitImplisittDag(implisittDag)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) =
        currentState.visitPermisjonsdag(permisjonsdag)

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) = currentState.visitPermisjonsdag(permisjonsdag)
    override fun visitStudiedag(studiedag: Studiedag) = currentState.visitStudiedag(studiedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) = currentState.visitSykHelgedag(sykHelgedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) = currentState.visitSykHelgedag(sykHelgedag)
    override fun visitSykedag(sykedag: Sykedag.Sykmelding) = currentState.visitSykedag(sykedag)
    override fun visitSykedag(sykedag: Sykedag.Søknad) = currentState.visitSykedag(sykedag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = currentState.visitUbestemt(ubestemtdag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = currentState.visitUtenlandsdag(utenlandsdag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(tilstand)

    override fun preVisitUtbetalingslinjer() = currentState.preVisitUtbetalingslinjer()
    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        currentState.visitUtbetalingslinje(utbetalingslinje)

    override fun postVisitUtbetalingslinjer() = currentState.postVisitUtbetalingslinjer()

    private interface JsonState : PersonVisitor {
        fun entering() {}
        fun leaving() {}
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

        fun toJson() = jacksonObjectMapper()
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
        private val arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>
    ) :
        JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toSpeilMap())
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()
        private var utbetalingstidslinje: Utbetalingstidslinje? = null

        override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
            utbetalingstidslinje = tidslinjer.lastOrNull()
        }

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            val avgrensetUtbetalingstidslinje = mutableListOf<MutableMap<String, Any?>>()

            vedtaksperiodeMap["utbetalingstidslinje"] = avgrensetUtbetalingstidslinje
            pushState(VedtaksperiodeState(vedtaksperiode, arbeidsgiver, vedtaksperiodeMap))
            vedtaksperioder.add(vedtaksperiodeMap)

            utbetalingstidslinje
                ?.subset(vedtaksperiode.periode().start, vedtaksperiode.periode().endInclusive)
                ?.accept(UtbetalingstidslinjeVisitor(avgrensetUtbetalingstidslinje))
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            popState()
        }
    }

    private class UtbetalingstidslinjeVisitor(private val utbetalingstidslinjeMap: MutableList<MutableMap<String, Any?>>) :
        UtbetalingsdagVisitor {

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            utbetalingstidslinjeMap.add(UtbetalingsdagReflect(dag, TypeData.Arbeidsdag).toMap())
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            utbetalingstidslinjeMap.add(UtbetalingsdagReflect(dag, TypeData.ArbeidsgiverperiodeDag).toMap())
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            utbetalingstidslinjeMap.add(NavDagReflect(dag, TypeData.NavDag).toMap())
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            utbetalingstidslinjeMap.add(UtbetalingsdagReflect(dag, TypeData.NavHelgDag).toMap())
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            utbetalingstidslinjeMap.add(UtbetalingsdagReflect(dag, TypeData.Fridag).toMap())
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            utbetalingstidslinjeMap.add(UtbetalingsdagReflect(dag, TypeData.UkjentDag).toMap())
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            val avvistDagMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjeMap.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            val foreldetDagMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjeMap.add(foreldetDagMap)

            foreldetDagMap.putAll(UtbetalingsdagReflect(dag, TypeData.ForeldetDag).toMap())
        }
    }


    private inner class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        arbeidsgiver: Arbeidsgiver,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toSpeilMap(arbeidsgiver))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            val hendelserForVedtaksperiode = mutableSetOf<UUID>()
            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinjeListe
            vedtaksperiodeMap["hendelser"] = hendelserForVedtaksperiode
            sykdomshistorikk.sykdomstidslinje().accept(SykdomstidslinjeState(sykdomstidslinjeListe))
            pushState(FinnHendelserState(hendelserForVedtaksperiode))
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
        private val linjerForSpeil = mutableListOf<Utbetalingslinje>()
        override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {
            linjerForSpeil.add(utbetalingslinje)
        }

        override fun postVisitUtbetalingslinjer() {
            linjerForSpeil.forEach { utbetalingslinje ->
                val utbetalingstidslinjeMap = mutableMapOf<String, Any?>(
                    "fom" to utbetalingslinje.fom,
                    "tom" to utbetalingslinje.tom,
                    "dagsats" to utbetalingslinje.dagsats,
                    "grad" to utbetalingslinje.grad
                )
                utbetalingstidslinjeListe.add(utbetalingstidslinjeMap)
            }
            popState()
        }
    }

    private inner class FinnHendelserState(
        private val hendelser: MutableSet<UUID>
    ) :
        JsonState {

        override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
            hendelser.add(element.hendelseId)
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }

        override fun preVisitHendelseSykdomstidslinje() {}
        override fun postVisitHendelseSykdomstidslinje() {}

        override fun postVisitBeregnetSykdomstidslinje() {}
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) =
            leggTilDag(JsonDagType.ARBEIDSDAG_INNTEKTSMELDING, arbeidsdag)

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) =
            leggTilDag(JsonDagType.ARBEIDSDAG_SØKNAD, arbeidsdag)

        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) =
            leggTilDag(JsonDagType.EGENMELDINGSDAG_INNTEKTSMELDING, egenmeldingsdag)

        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) =
            leggTilDag(JsonDagType.EGENMELDINGSDAG_SØKNAD, egenmeldingsdag)

        override fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) =
            leggTilDag(JsonDagType.FERIEDAG_INNTEKTSMELDING, feriedag)

        override fun visitFeriedag(feriedag: Feriedag.Søknad) = leggTilDag(JsonDagType.FERIEDAG_SØKNAD, feriedag)
        override fun visitImplisittDag(implisittDag: ImplisittDag) = leggTilDag(JsonDagType.IMPLISITT_DAG, implisittDag)
        override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) =
            leggTilDag(JsonDagType.PERMISJONSDAG_SØKNAD, permisjonsdag)

        override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) =
            leggTilDag(JsonDagType.PERMISJONSDAG_AAREG, permisjonsdag)

        override fun visitStudiedag(studiedag: Studiedag) = leggTilDag(JsonDagType.STUDIEDAG, studiedag)
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) =
            leggTilSykedag(JsonDagType.SYK_HELGEDAG_SYKMELDING, sykHelgedag)

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) =
            leggTilSykedag(JsonDagType.SYK_HELGEDAG_SØKNAD, sykHelgedag)

        override fun visitSykedag(sykedag: Sykedag.Sykmelding) = leggTilSykedag(JsonDagType.SYKEDAG_SYKMELDING, sykedag)
        override fun visitSykedag(sykedag: Sykedag.Søknad) = leggTilSykedag(JsonDagType.SYKEDAG_SØKNAD, sykedag)
        override fun visitUbestemt(ubestemtdag: Ubestemtdag) = leggTilDag(JsonDagType.UBESTEMTDAG, ubestemtdag)
        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = leggTilDag(JsonDagType.UTENLANDSDAG, utenlandsdag)

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

        override fun postVisitBeregnetSykdomstidslinje() {
            popState()
        }
    }
}
