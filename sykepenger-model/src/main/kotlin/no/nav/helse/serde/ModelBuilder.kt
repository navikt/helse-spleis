package no.nav.helse.serde

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import java.util.*

internal class ModelBuilder : StructureVisitor {

    private var personResult:Person? = null

    internal val result:Person get() = personResult!!

    private val stack: Stack<ModelState> = Stack()

    init {
        stack.push(Root())
    }

    private val currentState: ModelState
        get() = stack.peek()

    override fun toString() = currentState.toString()

    private fun pushState(state: ModelState) {
        currentState.leaving(this)
        stack.push(state)
        currentState.entering(this)
    }

    private fun popState() {
        currentState.leaving(this)
        stack.pop()
        currentState.entering(this)
    }

    override fun preVisitArrayField(name: String) { currentState.preVisitArrayField(this, name) }
    override fun postVisitArrayField() { currentState.postVisitArrayField(this) }
    override fun preVisitObjectField(name: String) { currentState.preVisitObjectField(this, name) }
    override fun postVisitObjectField() { currentState.postVisitObjectField(this) }
    override fun visitStringField(name: String, value: String) { currentState.visitStringField(this, name, value) }
    override fun visitBooleanField(name: String, value: Boolean) { currentState.visitBooleanField(this, name, value) }
    override fun visitNumberField(name: String, value: Number) { currentState.visitNumberField(this, name, value) }

    override fun preVisitArray() { currentState.preVisitArray(this) }
    override fun postVisitArray() { currentState.postVisitArray(this) }
    override fun preVisitObject() { currentState.preVisitObject(this) }
    override fun postVisitObject() { currentState.postVisitObject(this) }
    override fun visitString(value: String) { currentState.visitString(this, value) }
    override fun visitBoolean(value: Boolean) { currentState.visitBoolean(this, value) }
    override fun visitNumber(value: Number) { currentState.visitNumber(this, value) }

    private interface ModelState {
        fun entering(builder: ModelBuilder) {}
        fun leaving(builder: ModelBuilder) {}

        fun preVisitArrayField(builder: ModelBuilder, name: String) {}
        fun postVisitArrayField(builder: ModelBuilder) {}
        fun preVisitObjectField(builder: ModelBuilder, name: String) {}
        fun postVisitObjectField(builder: ModelBuilder) {}
        fun visitStringField(builder: ModelBuilder, name: String, value: String) {}
        fun visitBooleanField(builder: ModelBuilder, name: String, value: Boolean) {}
        fun visitNumberField(builder: ModelBuilder, name: String, value: Number) {}
        fun preVisitArray(builder: ModelBuilder) {}
        fun postVisitArray(builder: ModelBuilder) {}
        fun preVisitObject(builder: ModelBuilder) {}
        fun postVisitObject(builder: ModelBuilder) {}
        fun visitString(builder: ModelBuilder, value: String) {}
        fun visitBoolean(builder: ModelBuilder, value: Boolean) {}
        fun visitNumber(builder: ModelBuilder, value: Number) {}
    }

    private class Root : ModelState {
        override fun preVisitObject(builder: ModelBuilder) {
            builder.stack.push(PersonState {
                builder.personResult = it
            })
        }
    }

    private class PersonState(private val personSetter : (Person) -> Unit) : ModelState {
        val arbeidsgivere = mutableListOf<Arbeidsgiver>()
        var aktørId: String? = null
        var fødselsnummer: String? = null

        override fun visitStringField(builder: ModelBuilder, name: String, value: String) {
            when (name) {
                "aktørId" -> aktørId = value
                "fødselsnummer" -> fødselsnummer = value
            }
        }

        override fun postVisitObject(builder: ModelBuilder) {
            personSetter(Person(aktørId!!, fødselsnummer!!))
        }

        override fun preVisitArrayField(builder: ModelBuilder, name: String) {
            //if (name == "arbeidsgivere")
        }
    }



}
