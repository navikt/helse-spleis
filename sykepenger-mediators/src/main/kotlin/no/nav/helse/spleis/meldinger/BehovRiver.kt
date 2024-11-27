package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.spleis.IMessageMediator

internal abstract class BehovRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "behov"
    protected abstract val behov: List<Aktivitet.Behov.Behovtype>

    init {
        river.precondition { packet ->
            packet.requireValue("@final", true)
            packet.requireAll("@behov", behov.map(Enum<*>::name))
        }
        river.validate { packet ->
            packet.requireKey("@løsning", "fødselsnummer")
            packet.require("@besvart", JsonNode::asLocalDateTime)
        }
    }
}
