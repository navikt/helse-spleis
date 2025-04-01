package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.NavNoInntektsmeldingMessage

internal class NavNoInntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val alternativEventName = "arbeidsgiveropplysninger"
    override val riverName = "Arbeidsgiveropplysninger"

    override fun precondition(packet: JsonMessage) {
        packet.requireValue("avsenderSystem.navn", "NAV_NO")
        packet.requireKey("vedtaksperiodeId")
        packet.requireValue("arsakTilInnsending", "Ny")
    }

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
        message.interestedIn("beregnetInntekt")
    }

    override fun createMessage(packet: JsonMessage): NavNoInntektsmeldingMessage {
        val meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["arbeidstakerFnr"].asText()
        )
        return NavNoInntektsmeldingMessage(
            packet = packet,
            meldingsporing = meldingsporing
        )
    }
}
