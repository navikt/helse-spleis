package no.nav.helse.serde

import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.reflection.ArbeidsdagReflect
import no.nav.helse.serde.reflection.ArbeidsgiverReflect
import no.nav.helse.serde.reflection.InntektReflect
import no.nav.helse.serde.reflection.VedtaksperiodeReflect
import no.nav.helse.serde.reflection.PersonReflect
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.*

internal class JsonBuilder : PersonVisitor {

    private val stack: Stack<JsonState> = Stack()

    init {
        stack.push(Root())
    }

    private val currentState: JsonState
        get() = stack.peek()

    override fun toString() = currentState.toString()

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
    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) = currentState.preVisitUtbetalingstidslinje(this, tidslinje)
    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) = currentState.visitArbeidsdag(this, dag)
    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) = currentState.postVisitUtbetalingstidslinje(this, tidslinje)
    override fun preVisitPerioder() = currentState.preVisitPerioder(this)
    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) = currentState.preVisitVedtaksperiode(this, vedtaksperiode)

    private interface JsonState {
        fun entering(jsonBuilder: JsonBuilder) {}
        fun leaving(jsonBuilder: JsonBuilder) {}

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
    }

    private class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()
        private val hendelseMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(jsonBuilder: JsonBuilder, person: Person) {
            jsonBuilder.pushState(PersonState(person, personMap))
        }

        override fun toString() = listOf(hendelseMap, personMap).toString()
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

        override fun preVisitVedtaksperiode(jsonBuilder: JsonBuilder, vedtaksperiode: Vedtaksperiode){
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

        override fun visitArbeidsdag(jsonBuilder: JsonBuilder, arbeidsdag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            val arbeidsdagMap = mutableMapOf<String, Any?>()
            dager.add(arbeidsdagMap)

            arbeidsdagMap.putAll(ArbeidsdagReflect(arbeidsdag).toMap())
        }

        override fun postVisitUtbetalingstidslinje(jsonBuilder: JsonBuilder, utbetalingstidslinje: Utbetalingstidslinje) {
            jsonBuilder.popState()
        }
    }

    private class VedtaksperiodeState(vedtaksperiode: Vedtaksperiode, private val vedtaksperiodeMap: MutableMap<String, Any?>): JsonState {
        init {
            vedtaksperiodeMap.putAll(VedtaksperiodeReflect(vedtaksperiode).toMap())
        }
    }

}
