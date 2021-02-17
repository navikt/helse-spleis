package no.nav.helse.serde.api.builders

import no.nav.helse.person.PersonVisitor
import no.nav.helse.serde.AbstractBuilder

internal abstract class BuilderState(
    private var builder: AbstractBuilder? = null
) : PersonVisitor {

    internal fun builder(builder: AbstractBuilder) {
        this.builder = builder
    }

    protected fun pushState(newState: BuilderState) {
        builder?.pushState(newState)
    }

    protected fun popState() {
        builder?.popState()
    }
}
