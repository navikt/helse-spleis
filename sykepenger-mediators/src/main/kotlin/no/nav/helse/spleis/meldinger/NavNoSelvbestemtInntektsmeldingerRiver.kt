package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.NavNoSelvbestemtInntektsmeldingMessage

internal class NavNoSelvbestemtInntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "selvbestemte_arbeidsgiveropplysninger"
    override val riverName = "Selvbestemte arbeidsgiveropplysninger"

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
        message.interestedIn("beregnetInntekt", "harFlereArbeidsforhold")
        message.requireKey("vedtaksperiodeId")
    }

    override fun createMessage(packet: JsonMessage): NavNoSelvbestemtInntektsmeldingMessage {
        val meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["arbeidstakerFnr"].asText()
        )
        return NavNoSelvbestemtInntektsmeldingMessage(
            packet = packet,
            meldingsporing = meldingsporing
        )
    }
}
