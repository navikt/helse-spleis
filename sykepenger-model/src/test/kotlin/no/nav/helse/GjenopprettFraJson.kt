package no.nav.helse

import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson

internal fun gjenopprettFraJSON(fil: String, skjemaversjon: Int, regelverkslogg: Regelverkslogg = EmptyLog): Person {
    val json = fil.readResource()
    return gjenopprettFraJSONtekst(json, skjemaversjon, regelverkslogg)
}

// skjemaversjon er versjonen json-en ble serialisert med
internal fun gjenopprettFraJSONtekst(json: String, skjemaversjon: Int, regelverkslogg: Regelverkslogg = EmptyLog): Person {
    val serialisertPerson = SerialisertPerson(json, skjemaversjon)
    val dto = serialisertPerson.tilPersonDto()
    return Person.gjenopprett(regelverkslogg, dto, emptyList())
}

private fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: error("did not find resource <$this>")
