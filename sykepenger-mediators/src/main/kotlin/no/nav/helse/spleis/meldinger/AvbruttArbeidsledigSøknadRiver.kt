package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal class AvbruttArbeidsledigSøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "avbrutt_arbeidsledig_søknad"
    override val riverName = "Avbrutt arbeidsledig søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("@id", "fnr")
        message.require("fom", JsonNode::asLocalDate)
        message.require("tom", JsonNode::asLocalDate)
        message.interestedIn("tidligereArbeidsgiverOrgnummer")
        message.forbid("arbeidsgiver.orgnummer")
    }

    override fun createMessage(packet: JsonMessage): HendelseMessage {
        val meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fnr"].asText()
        )
        val behandlingsporing = packet["tidligereArbeidsgiverOrgnummer"]
            .takeIf(JsonNode::isTextual)
            ?.asText()
            ?.let {
                Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = it)
            } ?: Behandlingsporing.Yrkesaktivitet.Arbeidsledig
        return AvbruttSøknadMessage(packet, meldingsporing, behandlingsporing)
    }
}
