package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsledigMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadFrilansMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadSelvstendigMessage

internal class SendtArbeidsledigSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_arbeidsledig"
    override val riverName = "Sendt søknad Arbeidsledig"

    override fun validate(message: JsonMessage) {
        message.requireKey("id")
        message.forbid("arbeidsgiver.orgnummer")
        message.forbid("papirsykmeldinger")
        message.forbid("fravar")
        message.require("sendtNav", JsonNode::asLocalDateTime)
        message.interestedIn("egenmeldingsdagerFraSykmelding") { egenmeldinger -> egenmeldinger.map { it.asLocalDate() } }
        message.interestedIn("tidligereArbeidsgiverOrgnummer", "sporsmal", "arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadArbeidsledigMessage(packet)
}
