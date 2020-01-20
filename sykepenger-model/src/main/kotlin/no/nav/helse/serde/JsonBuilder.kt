package no.nav.helse.serde

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
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
    }

    private class Root : JsonState {
        private val personMap = mutableMapOf<String, Any?>()

        override fun preVisitPerson(jsonBuilder: JsonBuilder, person: Person) {
            jsonBuilder.pushState(PersonState(person, personMap))
        }

        override fun toString() = personMap.toString()
    }

    private class PersonState(person: Person, private val personMap: MutableMap<String, Any?>) : JsonState {
        init {
            //TODO: Hente felter fra Person med reflection og legg i personMap
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
            //TODO: Hente felter fra Arbeidsgiver med reflection og legg i arbeidsgiverMap
        }

        override fun preVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {
            val inntekter = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["inntekter"] = inntekter
            jsonBuilder.pushState(InntektHistorieState(inntekter))
        }

        override fun postVisitArbeidsgiver(jsonBuilder: JsonBuilder, arbeidsgiver: Arbeidsgiver) {
            jsonBuilder.popState()
        }
    }

    private class InntektHistorieState(private val inntekter: MutableList<MutableMap<String, Any?>>) : JsonState {
        override fun visitInntekt(jsonBuilder: JsonBuilder, inntekt: InntektHistorie.Inntekt) {
            val inntektMap = mutableMapOf<String, Any?>()
            inntekter.add(inntektMap)
            //TODO: Hente felter fra Inntekt med reflection og legg i inntektMap
        }

        override fun postVisitInntektHistorie(jsonBuilder: JsonBuilder, inntektHistorie: InntektHistorie) {
            jsonBuilder.popState()
        }
    }
}
