package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import java.util.*

internal abstract class HendelseMessage(private val packet: MessageDelegate) {
    internal val id: UUID = UUID.fromString(packet["@id"].asText())
    internal val navn = packet["@event_name"].asText()
    internal val opprettet = packet["@opprettet"].asLocalDateTime()

    internal abstract val fødselsnummer: String

    internal abstract fun behandle(mediator: IHendelseMediator)

    fun toJson() = packet.toJson()
}

internal fun asPeriode(jsonNode: JsonNode) =
    Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate())

