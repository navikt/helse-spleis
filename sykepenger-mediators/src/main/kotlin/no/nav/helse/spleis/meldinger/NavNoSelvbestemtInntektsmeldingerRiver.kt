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
    override val eventName = "inntektsmelding"
    override val riverName = "Selvbestemt Nav.no-inntektsmeldinger"
    override fun precondition(packet: JsonMessage) {
        packet.requireValue("avsenderSystem.navn", "NAV_NO_SELVBESTEMT")
        packet.requireKey("vedtaksperiodeId")
    }

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
        message.interestedIn("beregnetInntekt")
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
