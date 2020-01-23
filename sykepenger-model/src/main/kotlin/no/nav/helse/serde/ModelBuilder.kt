package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import java.util.*

internal class ModelBuilder(private val jsonString: String) : StructureVisitor {
    private var personResult:Person? = null
    private val hendelser = mutableMapOf<String,ArbeidstakerHendelse>()

    private val stack: Stack<ModelState> = Stack()

    internal fun result() : Person {
        if (personResult == null) parse()
        return personResult ?: throw RuntimeException("Kunne ikke gjenopprette personen")
    }

    private fun parse() {
        val json: JsonNode = jacksonObjectMapper().readTree(jsonString)
        stack.push(HendelserState())
        JsonVisitable(json["hendelser"]).accept(this)
        stack.push((PersonState()))
        JsonVisitable(json["person"]).accept(this)
    }

    private val currentState: ModelState
        get() = stack.peek()

    override fun toString() = currentState.toString()

    override fun preVisitArrayField(name: String) { currentState.preVisitArrayField(name) }
    override fun postVisitArrayField() { currentState.postVisitArrayField() }
    override fun preVisitObjectField(name: String) { currentState.preVisitObjectField(name) }
    override fun postVisitObjectField() { currentState.postVisitObjectField() }
    override fun visitStringField(name: String, value: String) { currentState.visitStringField(name, value) }
    override fun visitBooleanField(name: String, value: Boolean) { currentState.visitBooleanField(name, value) }
    override fun visitNumberField(name: String, value: Number) { currentState.visitNumberField(name, value) }

    override fun preVisitArray() { currentState.preVisitArray() }
    override fun postVisitArray() { currentState.postVisitArray() }
    override fun preVisitObject() { currentState.preVisitObject() }
    override fun postVisitObject() { currentState.postVisitObject() }
    override fun visitString(value: String) { currentState.visitString(value) }
    override fun visitBoolean(value: Boolean) { currentState.visitBoolean(value) }
    override fun visitNumber(value: Number) { currentState.visitNumber(value) }

    private interface ModelState : StructureVisitor

    private inner class HendelserState : ModelState

    private inner class PersonState : ModelState {
        val arbeidsgivere = mutableListOf<Arbeidsgiver>()
        var aktørId: String? = null
        var fødselsnummer: String? = null

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "aktørId" -> aktørId = value
                "fødselsnummer" -> fødselsnummer = value
            }
        }

        override fun postVisitObject() {
            personResult = Person(aktørId!!, fødselsnummer!!)
        }

        override fun preVisitArrayField(name: String) {
            //if (name == "arbeidsgivere")
        }
    }



}
