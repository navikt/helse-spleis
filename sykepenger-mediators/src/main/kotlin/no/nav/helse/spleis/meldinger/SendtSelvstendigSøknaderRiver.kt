package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.SendtSøknadSelvstendigMessage

internal class SendtSelvstendigSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_selvstendig"
    override val riverName = "Sendt søknad Selvstendig"

    override fun precondition(packet: JsonMessage) {
        packet.requireAny("arbeidssituasjon", listOf("SELVSTENDIG_NARINGSDRIVENDE", "BARNEPASSER"))
        packet.require("selvstendigNaringsdrivende.ventetid") { ventetid ->
            check(!ventetid.isMissingOrNull())
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("id", "selvstendigNaringsdrivende", "arbeidssituasjon")
        message.forbid("arbeidsgiver.orgnummer")
        message.require("sendtNav", JsonNode::asLocalDateTime)
        message.requireArray("selvstendigNaringsdrivende.inntekt.inntektsAar")
        message.require("selvstendigNaringsdrivende.ventetid.fom", JsonNode::asLocalDate)
        message.require("selvstendigNaringsdrivende.ventetid.tom", JsonNode::asLocalDate)
        message.interestedIn("egenmeldingsdagerFraSykmelding") { egenmeldinger -> egenmeldinger.map { it.asLocalDate() } }
        message.interestedIn("sporsmal", "arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys", "fravar", "papirsykmeldinger", "inntektFraNyttArbeidsforhold")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadSelvstendigMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fnr"].asText()
    )
    )
}
