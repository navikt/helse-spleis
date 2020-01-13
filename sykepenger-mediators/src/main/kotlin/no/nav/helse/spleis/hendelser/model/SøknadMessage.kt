package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProblems

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(originalMessage: String, problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey("id", "type", "status",
            "fnr", "aktorId", "arbeidsgiver", "fom", "tom", "startSyketilfelle",
            "opprettet", "egenmeldinger",
            "fravar", "soknadsperioder")
    }
}
