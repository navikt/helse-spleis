package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.spleis.meldinger.model.NavNoInntektsmeldingMessage

internal class NavNoInntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Nav.no-inntektsmelding (førstegangs)"
    override fun precondition(packet: JsonMessage) {
        packet.requireValue("avsenderSystem.navn", "NAV_NO")
        packet.requireKey("vedtaksperiodeId")
        // todo: endre fra interestedId til requireValue når Hag har rullet ut i prod
        packet.interestedIn("arsakTilInnsending") {
            check(it.asText() == "Ny")
        }
    }

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
    }

    override fun createMessage(packet: JsonMessage): NavNoInntektsmeldingMessage {
        val fødselsdato = packet["fødselsdato"].asLocalDate()
        val dødsdato = packet["dødsdato"].asOptionalLocalDate()
        val meldingsporing = Meldingsporing(
            id = packet["@id"].asText().toUUID(),
            fødselsnummer = packet["arbeidstakerFnr"].asText()
        )
        return NavNoInntektsmeldingMessage(
            packet = packet,
            personopplysninger = Personopplysninger(
                Personidentifikator(meldingsporing.fødselsnummer),
                fødselsdato, dødsdato
            ),
            meldingsporing = meldingsporing
        )
    }
}
