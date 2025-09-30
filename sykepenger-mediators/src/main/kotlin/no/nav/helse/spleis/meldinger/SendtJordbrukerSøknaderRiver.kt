package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.SendtSøknadJordbrukerMessage

internal class SendtJordbrukerSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_selvstendig"
    override val riverName = "Sendt søknad Jordbruker"

    override fun precondition(packet: JsonMessage) {
        packet.requireValue("arbeidssituasjon", "JORDBRUKER")
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("id")
        message.forbid("arbeidsgiver.orgnummer")
        message.require("sendtNav", JsonNode::asLocalDateTime)
        message.interestedIn("egenmeldingsdagerFraSykmelding") { egenmeldinger -> egenmeldinger.map { it.asLocalDate() } }
        message.interestedIn("sporsmal", "arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys", "fravar", "papirsykmeldinger", "inntektFraNyttArbeidsforhold")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadJordbrukerMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fnr"].asText()
    )
    )
}
