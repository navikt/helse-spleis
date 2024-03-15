package no.nav.helse

import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson

internal fun gjenopprettFraJSON(fil: String, jurist: MaskinellJurist = MaskinellJurist()): Person {
    val json = fil.readResource()
    return gjenopprettFraJSONtekst(json, jurist)
}
internal fun gjenopprettFraJSONtekst(json: String, jurist: MaskinellJurist = MaskinellJurist()): Person {
    val serialisertPerson = SerialisertPerson(json)
    val dto = serialisertPerson.tilPersonDto()
    return Person.gjenopprett(jurist, dto, emptyList())
}

private fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: error("did not find resource <$this>")