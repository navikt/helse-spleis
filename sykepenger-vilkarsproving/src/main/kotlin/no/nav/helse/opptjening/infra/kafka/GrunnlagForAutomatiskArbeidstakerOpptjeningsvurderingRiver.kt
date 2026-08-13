package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.domain.Arbeidsforhold

class GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver(
    rapidsConnection: RapidsConnection,
    private val opptjeningService: OpptjeningService
) : River.PacketListener {
    private val behovKey = "ArbeidsforholdV2"

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAllOrAny("@behov", listOf(behovKey))
                it.requireValue("@final", true)
                it.requireKey("fødselsnummer")
                it.requireKey("skjæringstidspunkt")
                it.requireKey("opprinneligBehov")
                it.requireKey("@løsning")
            }

            validate {
                it.requireArray("@løsning.$behovKey") {
                    requireKey("orgnummer")
                    requireAny("type", listOf("FORENKLET_OPPGJØRSORDNING", "FRILANSER", "MARITIMT", "ORDINÆRT"))
                    require("ansattSiden", JsonNode::asLocalDate)
                    interestedIn("ansattTil", JsonNode::asLocalDate)
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val arbeidsforhold = packet.mapArbeidsforhold()

        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val resultat = opptjeningService.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsforhold = arbeidsforhold,
        )
        when(resultat){
            OpptjeningService.BehandleGrunnlagResultat.AlleredeVurdert -> {
                // No-op. finn ut av lognivå
            }
            is OpptjeningService.BehandleGrunnlagResultat.NyVurderingForetatt -> {
                val opprinneligBehov = packet["opprinneligBehov"] as ObjectNode
                val løsning = opprinneligBehov.putObject("@løsning")
                løsning.putObject("Opptjeningsvurdering")
                    .put("id", resultat.vurderingId.toString())
                context.publish(opprinneligBehov.toString())
            }

            OpptjeningService.BehandleGrunnlagResultat.IngenVurderingFunnet -> {
                // No op med warning logging om vi ikke logger i servicen
            }
        }

    }

    private fun JsonMessage.mapArbeidsforhold() =
        mapArbeidsforhold(this["@løsning.$behovKey"])

    private fun mapArbeidsforhold(arbeidsforhold: JsonNode) = arbeidsforhold
        .filterNot { it["orgnummer"].asText().isBlank() }
        .filter {
            val til = it["ansattTil"].asOptionalLocalDate()
            til == null || it["ansattSiden"].asLocalDate() <= til
        }
        .map {
            Arbeidsforhold(
                orgnummer = it["orgnummer"].asText(),
                ansattFom = it["ansattSiden"].asLocalDate(),
                ansattTom = it["ansattTil"].asOptionalLocalDate(),
                type = when (it["type"].asText()) {
                    "FORENKLET_OPPGJØRSORDNING" -> Arbeidsforhold.Arbeidsforholdtype.FORENKLET_OPPGJØRSORDNING
                    "FRILANSER" -> Arbeidsforhold.Arbeidsforholdtype.FRILANSER
                    "MARITIMT" -> Arbeidsforhold.Arbeidsforholdtype.MARITIMT
                    "ORDINÆRT" -> Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
                    else -> error("har ikke mappingregel for arbeidsforholdtype: ${it["type"].asText()}")
                }
            )
        }
}
