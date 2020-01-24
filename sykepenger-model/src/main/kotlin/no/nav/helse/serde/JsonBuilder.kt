package no.nav.helse.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.*

internal class JsonBuilder : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()

    init {
        stack.push(Root())
    }

    private val currentState: JsonState
        get() = stack.peek()

    override fun toString() = currentState.toJson()

    private fun pushState(state: JsonState) {
        currentState.leaving(this)
        stack.push(state)
        currentState.entering(this)
    }

    private fun popState() {
        currentState.leaving(this)
        stack.pop()
        currentState.entering(this)
    }

    override fun preVisitPerson(person: Person) = currentState.preVisitPerson(this, person)
    override fun postVisitPerson(person: Person) = currentState.postVisitPerson(this, person)
    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) =
        currentState.preVisitArbeidsgiver(this, arbeidsgiver)

    override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) =
        currentState.postVisitArbeidsgiver(this, arbeidsgiver)

    override fun preVisitArbeidsgivere() = currentState.preVisitArbeidsgivere(this)
    override fun postVisitArbeidsgivere() = currentState.postVisitArbeidsgivere(this)
    override fun preVisitInntektHistorie(inntektHistorie: InntektHistorie) =
        currentState.preVisitInntektHistorie(this, inntektHistorie)

    override fun postVisitInntektHistorie(inntektHistorie: InntektHistorie) {
        currentState.postVisitInntektHistorie(this, inntektHistorie)
    }

    override fun preVisitInntekter() = currentState.preVisitInntekter(this)
    override fun visitInntekt(inntekt: InntektHistorie.Inntekt) = currentState.visitInntekt(this, inntekt)
    override fun preVisitTidslinjer() = currentState.preVisitTidslinjer(this)
    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.preVisitUtbetalingstidslinje(this, tidslinje)

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) =
        currentState.visitArbeidsdag(this, dag)

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) =
        currentState.postVisitUtbetalingstidslinje(this, tidslinje)

    override fun preVisitPerioder() = currentState.preVisitPerioder(this)
    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) =
        currentState.preVisitVedtaksperiode(this, vedtaksperiode)

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) =
        currentState.preVisitSykdomshistorikk(this, sykdomshistorikk)

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) =
        currentState.preVisitSykdomshistorikkElement(this, element)

    override fun visitHendelse(hendelse: SykdomstidslinjeHendelse) = currentState.visitHendelse(this, hendelse)

    override fun preVisitHendelseSykdomstidslinje() = currentState.preVisitHendelseSykdomstidslinje(this)
    override fun postVisitHendelseSykdomstidslinje() = currentState.postVisitHendelseSykdomstidslinje(this)
    override fun preVisitBeregnetSykdomstidslinje() = currentState.preVisitBeregnetSykdomstidslinje(this)
    override fun postVisitBeregnetSykdomstidslinje() = currentState.postVisitBeregnetSykdomstidslinje(this)
    override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.preVisitComposite(this, compositeSykdomstidslinje)

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) =
        currentState.postVisitComposite(this, compositeSykdomstidslinje)

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) =
        currentState.postVisitSykdomshistorikkElement(this, element)

    override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) =
        currentState.postVisitVedtaksperiode(this, vedtaksperiode)

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = currentState.visitDag(this, arbeidsdag)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) = currentState.visitDag(this, egenmeldingsdag)
    override fun visitFeriedag(feriedag: Feriedag) = currentState.visitDag(this, feriedag)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = currentState.visitDag(this, implisittDag)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = currentState.visitDag(this, permisjonsdag)
    override fun visitStudiedag(studiedag: Studiedag) = currentState.visitDag(this, studiedag)
    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = currentState.visitDag(this, sykHelgedag)
    override fun visitSykedag(sykedag: Sykedag) = currentState.visitDag(this, sykedag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = currentState.visitDag(this, ubestemtdag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = currentState.visitDag(this, utenlandsdag)
    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        currentState.visitTilstand(this, tilstand)

    override fun preVisitVedtaksperiodeSykdomstidslinje() = currentState.preVisitVedtaksperiodeSykdomstidslinje(this)
    override fun postVisitVedtaksperiodeSykdomstidslinje() = currentState.postVisitVedtaksperiodeSykdomstidslinje(this)
    override fun preVisitUtbetalingslinjer() = currentState.preVisitUtbetalingslinjer(this)
    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        currentState.visitUtbetalingslinje(this, utbetalingslinje)

    override fun postVisitUtbetalingslinjer() = currentState.postVisitUtbetalingslinjer(this)

    private interface JsonState {
        fun entering(jsonBuilder: JsonBuilder) {}
        fun leaving(jsonBuilder: JsonBuilder) {}
        fun toJson(): String = throw RuntimeException("toJson() kan bare kalles på rotnode. Ble kalt på ${toString()}")

        fun preVisitPerson(jsonBuilder: JsonBuilder, person: Person) {}
        fun postVisitPerson(jsonBuilder: JsonBuilder, person: Person) {}
        fun preVisitArbeidsgiver(jsonBuilder: JsonBuilder, arbeidsgiver: Arbeidsgiver) {}
        fun postVisitArbeidsgiver(jsonBuilder: JsonBuilder, arbeidsgiver: Arbeidsgiver) {}
        fun preVisitArbeidsgivere(jsonBuilder: JsonBuilder) {}
        fun postVisitArbeidsgivere(jsonBuilder: JsonBuilder) {}
        fun preVisitInntekter(jsonBuilder: JsonBuilder) {}
        fun visitInntekt(jsonBuilder: JsonBuilder, inntekt: InntektHistorie.Inntekt) {}
        fun preVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {}
        fun postVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {}
        fun preVisitTidslinjer(jsonBuilder: JsonBuilder) {}
        fun preVisitUtbetalingstidslinje(jsonBuilder: JsonBuilder, tidslinje: Utbetalingstidslinje) {}
        fun visitArbeidsdag(jsonBuilder: JsonBuilder, arbeidsdag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {}
        fun postVisitUtbetalingstidslinje(jsonBuilder: JsonBuilder, utbetalingstidslinje: Utbetalingstidslinje) {}
        fun preVisitVedtaksperiode(jsonBuilder: JsonBuilder, vedtaksperiode: Vedtaksperiode) {}
        fun preVisitPerioder(jsonBuilder: JsonBuilder) {}
        fun preVisitSykdomshistorikk(jsonBuilder: JsonBuilder, sykdomshistorikk: Sykdomshistorikk) {}
        fun preVisitSykdomshistorikkElement(jsonBuilder: JsonBuilder, element: Sykdomshistorikk.Element) {}
        fun visitHendelse(jsonBuilder: JsonBuilder, hendelse: SykdomstidslinjeHendelse) {}
        fun preVisitHendelseSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun postVisitHendelseSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun preVisitBeregnetSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun postVisitBeregnetSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun preVisitComposite(jsonBuilder: JsonBuilder, compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
        fun postVisitComposite(jsonBuilder: JsonBuilder, compositeSykdomstidslinje: CompositeSykdomstidslinje) {}
        fun postVisitSykdomshistorikkElement(jsonBuilder: JsonBuilder, element: Sykdomshistorikk.Element) {}
        fun postVisitVedtaksperiode(jsonBuilder: JsonBuilder, vedtaksperiode: Vedtaksperiode) {}
        fun visitDag(jsonBuilder: JsonBuilder, dag: Dag) {}
        fun visitTilstand(jsonBuilder: JsonBuilder, tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {}
        fun postVisitVedtaksperiodeSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun preVisitVedtaksperiodeSykdomstidslinje(jsonBuilder: JsonBuilder) {}
        fun preVisitUtbetalingslinjer(jsonBuilder: JsonBuilder) {}
        fun visitUtbetalingslinje(jsonBuilder: JsonBuilder, utbetalingslinje: Utbetalingslinje) {}
        fun postVisitUtbetalingslinjer(jsonBuilder: JsonBuilder) {}
    }

    private class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()
        private val hendelseMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(jsonBuilder: JsonBuilder, person: Person) {
            jsonBuilder.pushState(PersonState(person, personMap))
        }

        override fun toString() = listOf(hendelseMap, personMap).toString()

        override fun toJson(): String = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
            .writeValueAsString(personMap)
    }

    private class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : JsonState {
        init {
            personMap.putAll(PersonReflect(person).toMap())
        }

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitArbeidsgivere(jsonBuilder: JsonBuilder) {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(jsonBuilder: JsonBuilder, arbeidsgiver: Arbeidsgiver) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            arbeidsgivere.add(arbeidsgiverMap)
            jsonBuilder.pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap))
        }

        override fun postVisitPerson(jsonBuilder: JsonBuilder, person: Person) {
            jsonBuilder.popState()
        }
    }

    private class ArbeidsgiverState(arbeidsgiver: Arbeidsgiver, private val arbeidsgiverMap: MutableMap<String, Any?>) :
        JsonState {
        init {
            arbeidsgiverMap.putAll(ArbeidsgiverReflect(arbeidsgiver).toMap())
        }

        override fun preVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {
            val inntekter = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["inntekter"] = inntekter
            jsonBuilder.pushState(InntektHistorieState(inntekter))
        }

        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitTidslinjer(jsonBuilder: JsonBuilder) {
            arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisitUtbetalingstidslinje(jsonBuilder: JsonBuilder, tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
            jsonBuilder.pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        private val vedtaksperioder = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder(jsonBuilder: JsonBuilder) {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperioder
        }

        override fun preVisitVedtaksperiode(jsonBuilder: JsonBuilder, vedtaksperiode: Vedtaksperiode) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperioder.add(vedtaksperiodeMap)
            jsonBuilder.pushState(VedtaksperiodeState(vedtaksperiode, arbeidsgiverMap))

        }

        override fun postVisitArbeidsgiver(jsonBuilder: JsonBuilder, arbeidsgiver: Arbeidsgiver) {
            jsonBuilder.popState()
        }
    }

    private class InntektHistorieState(private val inntekter: MutableList<MutableMap<String, Any?>>) : JsonState {
        override fun visitInntekt(jsonBuilder: JsonBuilder, inntekt: InntektHistorie.Inntekt) {
            val inntektMap = mutableMapOf<String, Any?>()
            inntekter.add(inntektMap)

            inntektMap.putAll(InntektReflect(inntekt).toMap())
        }

        override fun postVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {
            jsonBuilder.popState()
        }
    }

    private class UtbetalingstidslinjeState(utbetalingstidslinjeMap: MutableMap<String, Any?>) :
        JsonState {

        private val dager = mutableListOf<MutableMap<String, Any?>>()

        init {
            utbetalingstidslinjeMap["dager"] = dager
        }

        override fun visitArbeidsdag(
            jsonBuilder: JsonBuilder,
            arbeidsdag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
        ) {
            val arbeidsdagMap = mutableMapOf<String, Any?>()
            dager.add(arbeidsdagMap)

            arbeidsdagMap.putAll(ArbeidsdagReflect(arbeidsdag).toMap())
        }

        override fun postVisitUtbetalingstidslinje(
            jsonBuilder: JsonBuilder,
            utbetalingstidslinje: Utbetalingstidslinje
        ) {
            jsonBuilder.popState()
        }
    }

    private class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toMap())
        }

        private val sykdomshistorikkElementer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitSykdomshistorikk(jsonBuilder: JsonBuilder, sykdomshistorikk: Sykdomshistorikk) {
            vedtaksperiodeMap["sykdomshistorikk"] = sykdomshistorikkElementer
        }

        override fun preVisitSykdomshistorikkElement(jsonBuilder: JsonBuilder, element: Sykdomshistorikk.Element) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            jsonBuilder.pushState(SykdomshistorikkElementState(element, elementMap))
        }

        override fun visitTilstand(jsonBuilder: JsonBuilder, tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            vedtaksperiodeMap["tilstand"] = tilstand.type.name
        }

        override fun preVisitVedtaksperiodeSykdomstidslinje(jsonBuilder: JsonBuilder) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinjeListe
            jsonBuilder.pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitUtbetalingslinjer(jsonBuilder: JsonBuilder) {
            val utbetalingstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            vedtaksperiodeMap["utbetalingslinjer"] = utbetalingstidslinjeListe
            jsonBuilder.pushState(UtbetalingslinjeState(utbetalingstidslinjeListe))
        }

        override fun postVisitVedtaksperiode(jsonBuilder: JsonBuilder, vedtaksperiode: Vedtaksperiode) {
            jsonBuilder.popState()
        }
    }

    private class UtbetalingslinjeState(private val utbetalingstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {
        override fun visitUtbetalingslinje(jsonBuilder: JsonBuilder, utbetalingslinje: Utbetalingslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>(
                "fom" to utbetalingslinje.fom,
                "tom" to utbetalingslinje.tom,
                "dagsats" to utbetalingslinje.dagsats
            )
            utbetalingstidslinjeListe.add(utbetalingstidslinjeMap)
        }

        override fun postVisitUtbetalingslinjer(jsonBuilder: JsonBuilder) {
            jsonBuilder.popState()
        }
    }

    private class SykdomshistorikkElementState(
        element: Sykdomshistorikk.Element,
        private val elementMap: MutableMap<String, Any?>
    ) : JsonState {
        init {
            elementMap["tidsstempel"] = element.tidsstempel
        }

        override fun visitHendelse(jsonBuilder: JsonBuilder, hendelse: SykdomstidslinjeHendelse) {
            elementMap["hendelseId"] = hendelse.hendelseId()
        }

        override fun preVisitHendelseSykdomstidslinje(jsonBuilder: JsonBuilder) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinjeListe
            jsonBuilder.pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun preVisitBeregnetSykdomstidslinje(jsonBuilder: JsonBuilder) {
            val sykdomstidslinjeListe = mutableListOf<MutableMap<String, Any?>>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinjeListe
            jsonBuilder.pushState(SykdomstidslinjeState(sykdomstidslinjeListe))
        }

        override fun postVisitSykdomshistorikkElement(jsonBuilder: JsonBuilder, element: Sykdomshistorikk.Element) {
            jsonBuilder.popState()
        }
    }

    private class SykdomstidslinjeState(private val sykdomstidslinjeListe: MutableList<MutableMap<String, Any?>>) :
        JsonState {

        override fun visitDag(jsonBuilder: JsonBuilder, dag: Dag) {
            fun mapDag(dagen: Dag): MutableMap<String, Any?> = mutableMapOf(
                "dagen" to dagen.dagen,
                "hendelseId" to dagen.hendelse.hendelseId(),
                "type" to dagen.dagType().name,
                "erstatter" to dagen.erstatter.map { mapDag(it) }
            )

            sykdomstidslinjeListe.add(mapDag(dag))
        }

        override fun postVisitHendelseSykdomstidslinje(jsonBuilder: JsonBuilder) {
            jsonBuilder.popState()
        }

        override fun postVisitBeregnetSykdomstidslinje(jsonBuilder: JsonBuilder) {
            jsonBuilder.popState()
        }

        override fun postVisitVedtaksperiodeSykdomstidslinje(jsonBuilder: JsonBuilder) {
            jsonBuilder.popState()
        }
    }
}
