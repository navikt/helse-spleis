package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigTidligereArbeidstakerSøknadMessage
import no.nav.helse.spleis.meldinger.model.SøknadMessage

internal class NyeArbeidsledigSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad_arbeidsledig"
    override val riverName = "Ny arbeidsledig søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("sykmeldingId")
        message.requireValue("status", "NY")
        message.interestedIn("fremtidig_søknad", "tidligereArbeidsgiverOrgnummer")
        message.forbid("arbeidsgiver.orgnummer")
    }

    override fun createMessage(packet: JsonMessage): SøknadMessage {
        val meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fnr"].asText()
        )
        val tidligereArbeidsgiver = packet["tidligereArbeidsgiverOrgnummer"].takeIf(JsonNode::isTextual)?.asText() ?: return NyArbeidsledigSøknadMessage(
            packet = packet,
            meldingsporing = meldingsporing
        )
        return NyArbeidsledigTidligereArbeidstakerSøknadMessage(
            packet = packet,
            orgnummer = tidligereArbeidsgiver,
            meldingsporing = meldingsporing
        )
    }
}
