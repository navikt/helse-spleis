package no.nav.helse.spleis.hendelser.model

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.JsonMessage

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(originalMessage: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) :
    JsonMessage(originalMessage, problems, aktivitetslogg) {
    init {
        requiredKey("fnr", "aktorId", "arbeidsgiver.orgnummer", "opprettet", "soknadsperioder")
    }
}
