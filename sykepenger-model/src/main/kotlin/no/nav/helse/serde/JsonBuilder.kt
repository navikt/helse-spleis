package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.*
import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
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
    override fun preVisitUtbetalingstidslinje() =
        currentState.preVisitUtbetalingstidslinje()

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

    override fun postVisitUtbetalingstidslinje() =
        currentState.postVisitUtbetalingstidslinje()

    override fun preVisitPerioder() = currentState.preVisitPerioder()
    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) =
        currentState.preVisitVedtaksperiode(vedtaksperiode, id)

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(sykdomshistorikk)

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.preVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun preVisitHendelseSykdomstidslinje() = currentState.preVisitHendelseSykdomstidslinje()
    override fun postVisitHendelseSykdomstidslinje() = currentState.postVisitHendelseSykdomstidslinje()
    override fun preVisitBeregnetSykdomstidslinje() = currentState.preVisitBeregnetSykdomstidslinje()
    override fun postVisitBeregnetSykdomstidslinje() = currentState.postVisitBeregnetSykdomstidslinje()
    override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.preVisitComposite(compositeSykdomstidslinje)

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.postVisitComposite(compositeSykdomstidslinje)

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.postVisitSykdomshistorikkElement(element, id, tidsstempel)

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
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) = currentState.visitSykHelgedag(sykHelgedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) = currentState.visitSykHelgedag(sykHelgedag)
    override fun visitSykedag(sykedag: Sykedag.Sykmelding) = currentState.visitSykedag(sykedag)
    override fun visitSykedag(sykedag: Sykedag.Søknad) = currentState.visitSykedag(sykedag)
    override fun visitKunArbeidsgiverSykedag(sykedag: KunArbeidsgiverSykedag) = currentState.visitKunArbeidsgiverSykedag(sykedag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = currentState.visitUbestemt(ubestemtdag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = currentState.visitUtenlandsdag(utenlandsdag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(tilstand)

    override fun preVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) = currentState.preVisitUtbetalingslinjer(linjer)
    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        currentState.visitUtbetalingslinje(utbetalingslinje)

    override fun postVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) = currentState.postVisitUtbetalingslinjer(linjer)

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

        override fun preVisitUtbetalingstidslinje() {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder() {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
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

        override fun postVisitUtbetalingstidslinje() = popState()
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

            pushState(SykdomshistorikkElementState(element, id, tidsstempel, elementMap))
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {
            val utbetalingstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["utbetalingslinjer"] = utbetalingstidslinjeListe
            pushState(UtbetalingslinjeState(utbetalingstidslinjeListe))
        }

        override fun preVisitUtbetalingstidslinje() {
            val utbetalingstidslinje = mutableMapOf<String, Any?>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje))
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
                "dagsats" to utbetalingslinje.dagsats,
                "grad" to utbetalingslinje.grad
            )
            utbetalingstidslinjeListe.add(utbetalingstidslinjeMap)
        }

        override fun postVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {
            popState()
        }
    }

    private inner class SykdomshistorikkElementState(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime,
        private val elementMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            elementMap["hendelseId"] = id
            elementMap["tidsstempel"] = tidsstempel
        }

        override fun preVisitHendelseSykdomstidslinje() {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinjeListe
            pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitBeregnetSykdomstidslinje() {
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
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) = leggTilSykedag(JsonDagType.SYK_HELGEDAG_SYKMELDING, sykHelgedag)
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) = leggTilSykedag(JsonDagType.SYK_HELGEDAG_SØKNAD, sykHelgedag)
        override fun visitSykedag(sykedag: Sykedag.Sykmelding) = leggTilSykedag(JsonDagType.SYKEDAG_SYKMELDING, sykedag)
        override fun visitSykedag(sykedag: Sykedag.Søknad) = leggTilSykedag(JsonDagType.SYKEDAG_SØKNAD, sykedag)
        override fun visitKunArbeidsgiverSykedag(sykedag: KunArbeidsgiverSykedag) = leggTilSykedag(JsonDagType.KUN_ARBEIDSGIVER_SYKEDAG, sykedag)
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

        override fun postVisitHendelseSykdomstidslinje() {
            popState()
        }

        override fun postVisitBeregnetSykdomstidslinje() {
            popState()
        }
    }
}
