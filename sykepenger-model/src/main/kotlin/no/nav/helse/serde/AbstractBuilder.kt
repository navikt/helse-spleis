package no.nav.helse.serde

import no.nav.helse.person.DelegatedPersonVisitor
import no.nav.helse.person.PersonVisitor

internal abstract class AbstractBuilder private constructor(
    private val stack: MutableList<PersonVisitor>
) : PersonVisitor by DelegatedPersonVisitor(stack::first) {

    internal constructor() : this(mutableListOf())

    internal fun pushState(state: PersonVisitor) {
        stack.add(0, state)
    }

    internal fun popState() {
        stack.removeAt(0)
    }
}
