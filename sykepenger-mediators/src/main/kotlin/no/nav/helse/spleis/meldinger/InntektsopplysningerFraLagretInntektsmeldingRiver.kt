package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InntektsopplysningerFraLagretInntektsmeldingMessage

internal class InntektsopplysningerFraLagretInntektsmeldingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsopplysninger_fra_lagret_inntektsmelding"

    override val riverName = "InntektsopplysningerFraLagretInntektsmelding"


    init {
        river.precondition { packet -> packet.requireValue("yrkesaktivitetstype", "ARBEIDSTAKER") }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey( "vedtaksperiodeId", "organisasjonsnummer", "fødselsnummer", "inntektsmeldingMeldingsreferanseId")
        message.interestedIn("inntektsmeldingOrganisasjonsnummer")
    }

    override fun createMessage(packet: JsonMessage) = InntektsopplysningerFraLagretInntektsmeldingMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
    )
}
