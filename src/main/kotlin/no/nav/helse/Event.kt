package no.nav.helse

interface Event {

    fun name() =
        this.javaClass::getName.name
}
