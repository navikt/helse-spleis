package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageProcessor
import java.util.*

internal abstract class HendelseMessage(originalMessage: String, problems: MessageProblems) : JsonMessage(originalMessage, problems) {

    internal open val id: UUID get() = UUID.fromString(this["@id"].asText())
    internal abstract val fødselsnummer: String

    open fun accept(processor: MessageProcessor) {}
}

internal fun asPeriode(jsonNode: JsonNode) =
    Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate())

