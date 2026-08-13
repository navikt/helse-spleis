package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.application.VurderOpptjeningResultat
import no.nav.helse.opptjening.domain.Arbeidssituasjon

class OpptjeningsvurderingRiver(rapidsConnection: RapidsConnection, private val opptjeningService: OpptjeningService) : River.PacketListener {
    init {

        River(rapidsConnection).apply {
            precondition { it.requireAllOrAny("@behov", listOf("Opptjeningsvurdering")) }
            validate { it.forbid("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("skjæringstidspunkt") }
            validate { it.requireKey("arbeidssituasjon") } //TODO strengere validering
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
        val arbeidssituasjon = Arbeidssituasjon.valueOf(packet["arbeidssituasjon"].asText())

        val vurderOpptjeningResultat = opptjeningService.vurderOpptjening(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidssituasjon = arbeidssituasjon
        )

        when (vurderOpptjeningResultat) {
            is VurderOpptjeningResultat.HarVurdering -> {
                packet["@løsning"] =
                    mapOf(
                        "Opptjeningsvurdering" to mapOf(
                            "id" to vurderOpptjeningResultat.vurderingId.toString()
                        ),
                    )
                context.publish(packet.toJson())
            }

            is VurderOpptjeningResultat.TrengerArbeidsforhold -> {
                val utgåendeBehov = JsonMessage.newNeed(
                    behov = listOf("ArbeidsforholdV2"),

                    map = mapOf(
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "fødselsnummer" to fødselsnummer,
                        "opprinneligBehov" to jacksonObjectMapper().readTree(packet.toJson()) //TODO vi må være sikker på json eller string her?
                    )
                )
                context.publish(utgåendeBehov.toJson())
            }
        }
    }
}
