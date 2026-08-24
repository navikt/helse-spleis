package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage

internal class AvbruttSøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    private val søknadstyper = setOf(
        Søknadstype.Arbeidstaker,
        Søknadstype.Selvstendig,
        Søknadstype.Frilanser,
        Søknadstype.Jordbruker,
        Søknadstype.Fisker,
        Søknadstype.Arbeidsledig
    )

    override val eventNames = søknadstyper.map { it.eventName }.toSet()

    override val riverName = "Avbrutt søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey( "fnr")
        message.require("fom", JsonNode::asLocalDate)
        message.require("tom", JsonNode::asLocalDate)
        message.søknadstype.validate(message)
    }

    override fun createMessage(packet: JsonMessage): AvbruttSøknadMessage {
        return AvbruttSøknadMessage(
            packet = packet,
            meldingsporing = Meldingsporing(
                id = packet.meldingsreferanseId(),
                fødselsnummer = packet["fnr"].asText()
            ),
            behandlingsporing = packet.søknadstype.yrkesaktivitet(packet)
        )
    }

    private val JsonMessage.søknadstype get() = søknadstyper.singleOrNull {
        it.eventName == get("@event_name").asText()
    } ?: error("Ukjent søknadstype for eventName ${get("@event_name").asText()}")

    private sealed interface Søknadstype {
        val eventName: String
        fun validate(packet: JsonMessage) {}
        fun yrkesaktivitet(packet: JsonMessage): Behandlingsporing.Yrkesaktivitet

        data object Arbeidstaker: Søknadstype {
            override val eventName = "avbrutt_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = packet["arbeidsgiver.orgnummer"].asText()
            )
            override fun validate(packet: JsonMessage) {
                packet.requireKey("arbeidsgiver.orgnummer")
            }
        }

        data object Arbeidsledig: Søknadstype {
            override val eventName = "avbrutt_arbeidsledig_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = packet["tidligereArbeidsgiverOrgnummer"]
                .takeIf(JsonNode::isTextual)
                ?.asText()
                ?.let { Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = it) }
                ?: Behandlingsporing.Yrkesaktivitet.Arbeidsledig

            override fun validate(packet: JsonMessage) {
                packet.interestedIn("tidligereArbeidsgiverOrgnummer")
                packet.forbid("arbeidsgiver.orgnummer")
            }
        }

        data object Selvstendig: Søknadstype {
            override val eventName = "avbrutt_selvstendig_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = Behandlingsporing.Yrkesaktivitet.Selvstendig
        }

        data object Jordbruker: Søknadstype {
            override val eventName = "avbrutt_jordbruker_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = Behandlingsporing.Yrkesaktivitet.Selvstendig
        }

        data object Frilanser: Søknadstype {
            override val eventName = "avbrutt_frilanser_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = Behandlingsporing.Yrkesaktivitet.Frilans
        }

        data object Fisker: Søknadstype {
            override val eventName = "avbrutt_fisker_søknad"
            override fun yrkesaktivitet(packet: JsonMessage) = Behandlingsporing.Yrkesaktivitet.Selvstendig
        }
    }
}
