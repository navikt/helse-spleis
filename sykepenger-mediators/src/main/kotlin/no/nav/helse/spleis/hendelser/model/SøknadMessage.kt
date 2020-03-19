package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(originalMessage: String, problems: MessageProblems) :
    HendelseMessage(originalMessage, problems) {

    init {
        requireKey("fnr", "aktorId", "arbeidsgiver.orgnummer", "soknadsperioder")
        require("opprettet", JsonNode::asLocalDateTime)
    }

    override val fødselsnummer: String get() = this["fnr"].asText()
}
