package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.mapping.JsonDagType.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
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

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) = currentState.visitInntekt(inntekt, id)
    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) = currentState.preVisitTidslinjer(tidslinjer)
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
        currentState.visit(dag, dato, økonomi, grad, aktuellDagsinntekt, dekningsgrunnlag, arbeidsgiverbeløp, personbeløp)

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
        currentState.visit(dag, dato, økonomi, grad, aktuellDagsinntekt, dekningsgrunnlag, arbeidsgiverbeløp, personbeløp)

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

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) = currentState.preVisitPerioder(vedtaksperioder)
    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) = currentState.postVisitPerioder(vedtaksperioder)
    override fun preVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) = currentState.preVisitForkastedePerioder(vedtaksperioder)
    override fun postVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) = currentState.postVisitForkastedePerioder(vedtaksperioder)
    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int
    ) =
        currentState.preVisitVedtaksperiode(vedtaksperiode, id, gruppeId, arbeidsgiverNettoBeløp, personNettoBeløp)

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

    override fun preVisitSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        låstePerioder: List<Periode>,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.preVisitSykdomstidslinje(tidslinje, låstePerioder, id, tidsstempel)

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) =
        currentState.postVisitSykdomstidslinje(tidslinje, id, tidsstempel)

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) =
        currentState.postVisitSykdomshistorikkElement(element, id, tidsstempel)

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int
    ) =
        currentState.postVisitVedtaksperiode(vedtaksperiode, id, gruppeId, arbeidsgiverNettoBeløp, personNettoBeløp)

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
    ) =
        currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)
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
    ) =
        currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)
    override fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) =
        currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)
    override fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) =
        currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)
    override fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) =
        currentState.visitDag(dag, dato, økonomi, grad, arbeidsgiverBetalingProsent, kilde)
    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
        currentState.visitDag(dag, dato, kilde, melding)

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

        override fun preVisit(tidslinje: Utbetalingstidslinje) {
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
            arbeidsgiverMap["forkastede"] = this.vedtaksperioder.toList()
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int
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
        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
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

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi,
            aktuellDagsinntekt: Double
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Arbeidsdag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi,
            aktuellDagsinntekt: Double
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.ArbeidsgiverperiodeDag).toMap())
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
            dager.add(UtbetalingsdagReflect(dag, TypeData.NavDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.NavHelgDag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.Fridag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            dager.add(UtbetalingsdagReflect(dag, TypeData.UkjentDag).toMap())
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
            val avvistDagMap = mutableMapOf<String, Any?>()
            dager.add(avvistDagMap)

            avvistDagMap.putAll(AvvistdagReflect(dag).toMap())
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            val foreldetDagMap = mutableMapOf<String, Any?>()
            dager.add(foreldetDagMap)

            foreldetDagMap.putAll(UtbetalingsdagReflect(dag, TypeData.ForeldetDag).toMap())
        }

        override fun postVisit(tidslinje: Utbetalingstidslinje) = popState()
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

        override fun preVisit(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinje = mutableMapOf<String, Any?>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinje
            pushState(UtbetalingstidslinjeState(utbetalingstidslinje))
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            gruppeId: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int
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
            val sykdomstidslinje = mutableMapOf<String, Any>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val sykdomstidslinje = mutableMapOf<String, Any>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }
    }

    private inner class SykdomstidslinjeState(private val sykdomstidslinje: MutableMap<String, Any>) : JsonState {

        private val dager: MutableList<MutableMap<String, Any>> = mutableListOf()

        override fun preVisitSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            låstePerioder: List<Periode>,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            sykdomstidslinje["id"] = id
            sykdomstidslinje["tidsstempel"] = tidsstempel
            sykdomstidslinje["låstePerioder"] = låstePerioder.map {
                mapOf("fom" to it.start, "tom" to it.endInclusive)
            }
        }

        override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) {
            sykdomstidslinje["dager"] = dager
        }

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
        ) =
            leggTilDag(dag, dato, kilde, økonomi)

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
        ) =
            leggTilDag(dag, dato, kilde, økonomi)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) =
            leggTilDag(dag, dato, kilde, økonomi)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) =
            leggTilDag(dag, dato, kilde, økonomi)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) =
            leggTilDag(dag, dato, kilde, økonomi)

        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
            leggTilDag(dag, dato, kilde, melding = melding)

        private fun leggTilDag(
            dag: Dag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            økonomi: Økonomi? = null,
            melding: String? = null
        ) {
            dager.add(
                mutableMapOf(
                    "dato" to dato,
                    "type" to dag.toJsonType(),
                    "kilde" to kilde.toJson()
                ).also { data ->
                    økonomi?.let { data.putAll(økonomi.toMap())}
                    melding?.let { data["melding"] = it }
                }
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

private fun Hendelseskilde.toJson() = mapOf(
    "type" to toString(), "id" to meldingsreferanseId()
)

private fun Dag.toJsonType() = when (this) {
    is Sykedag -> SYKEDAG
    is UkjentDag -> UKJENT_DAG
    is Arbeidsdag -> ARBEIDSDAG
    is Arbeidsgiverdag -> ARBEIDSGIVERDAG
    is Feriedag -> FERIEDAG
    is FriskHelgedag -> FRISK_HELGEDAG
    is ArbeidsgiverHelgedag -> ARBEIDSGIVER_HELGEDAG
    is ForeldetSykedag -> FORELDET_SYKEDAG
    is SykHelgedag -> SYK_HELGEDAG
    is Permisjonsdag -> PERMISJONSDAG
    is Studiedag -> STUDIEDAG
    is Utenlandsdag -> UTENLANDSDAG
    is ProblemDag -> PROBLEMDAG
}
