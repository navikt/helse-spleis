package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Søknad that is sent to NAV
internal class SendtSøknadNavMessage(packet: JsonMessage, private val builder: SendtSøknadBuilder = SendtSøknadBuilder()) : SøknadMessage(packet, builder) {

    override fun _behandle(mediator: IHendelseMediator, packet: JsonMessage, context: MessageContext) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        byggSendtSøknad(builder, packet)
        mediator.behandle(this, builder.build(), context)
    }

    internal companion object {
        internal fun byggSendtSøknad(builder: SendtSøknadBuilder, packet: JsonMessage) {
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
            packet["korrigerer"].takeUnless(JsonNode::isMissingOrNull)?.let {
                builder.korrigerer(UUID.fromString(it.asText()))
            }
            builder.arbeidsgjennopptatt(packet["arbeidGjenopptatt"].asOptionalLocalDate())
        }
    }
}
