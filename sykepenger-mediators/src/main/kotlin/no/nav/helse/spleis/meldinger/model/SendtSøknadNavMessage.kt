package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Søknad that is sent to NAV
internal class SendtSøknadNavMessage(private val packet: JsonMessage, private val builder: SendtSøknadBuilder = SendtSøknadBuilder()) : SøknadMessage(packet, builder) {

    override fun _behandle(mediator: IHendelseMediator, packet: JsonMessage) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        byggSendtSøknad(builder, packet)
        mediator.behandle(this, builder.build())
    }

    internal companion object {
        internal fun byggSendtSøknad(builder: SøknadBuilder, packet: JsonMessage) {
            builder.permittert(packet["permitteringer"].takeIf(JsonNode::isArray)?.takeUnless { it.isEmpty }?.let { true } ?: false)
            packet["merknaderFraSykmelding"].takeIf(JsonNode::isArray)?.forEach {
                builder.merknader(it.path("type").asText(), it.path("beskrivelse").takeUnless { it.isMissingOrNull() }?.asText())
            }
            packet["papirsykmeldinger"].forEach {
                builder.papirsykmelding(fom = it.path("fom").asLocalDate(), tom = it.path("tom").asLocalDate())
            }
            packet["andreInntektskilder"].forEach {
                builder.inntektskilde(sykmeldt = it["sykmeldt"].asBoolean(), type = it["type"].asText())
            }
            packet["egenmeldinger"].forEach {
                builder.egenmelding(fom = it.path("fom").asLocalDate(), tom = it.path("tom").asLocalDate())
            }
            packet["fravar"].forEach {
                val fraværstype = it["type"].asText()
                val fom = it.path("fom").asLocalDate()
                val tom = it.path("tom").takeUnless { it.isMissingOrNull() }?.asLocalDate()
                builder.fravær(fraværstype, fom, tom)
            }
            builder.arbeidsgjennopptatt(packet["arbeidGjenopptatt"].asOptionalLocalDate())
        }
    }
}
