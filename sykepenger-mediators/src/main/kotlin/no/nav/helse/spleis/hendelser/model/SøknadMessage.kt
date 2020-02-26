package no.nav.helse.spleis.hendelser.model

import no.nav.helse.rapids_rivers.MessageProblems

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(originalMessage: String, problems: MessageProblems) :
    HendelseMessage(originalMessage, problems) {

    init {
        requireKey("fnr", "aktorId", "arbeidsgiver.orgnummer", "opprettet", "soknadsperioder")
    }
}
