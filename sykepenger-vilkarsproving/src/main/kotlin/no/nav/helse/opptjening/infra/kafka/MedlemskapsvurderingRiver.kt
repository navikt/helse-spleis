package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.MedlemskapService
import no.nav.helse.opptjening.application.VurderMedlemskapResultat
import no.nav.helse.opptjening.bootstrap.sikkerLogg

internal class MedlemskapsvurderingRiver(rapidsConnection: RapidsConnection, private val medlemskapService: MedlemskapService) : River.PacketListener {
    private val behovKey = "Medlemskapsvurdering"

    init {
        River(rapidsConnection).apply {
            precondition { it.requireAllOrAny("@behov", listOf(behovKey)) }
            validate { it.forbid("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("skjæringstidspunkt") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
        sikkerLogg.info("Mottatt behov for $behovKey for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt")

        val resultat = medlemskapService.vurderMedlemskap(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt
        )

        when (resultat) {
            is VurderMedlemskapResultat.HarVurdering -> {
                packet["@løsning"] =
                    mapOf(
                        "Medlemskapsvurdering" to mapOf(
                            "id" to resultat.vurderingId.toString()
                        ),
                    )
                sikkerLogg.info("Har vurdering for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${resultat.vurderingId}. Løsning:\n\t${packet.toJson()}")
                context.publish(packet.toJson())
            }

            is VurderMedlemskapResultat.TrengerMedlemskap -> {
                val utgåendeBehov = JsonMessage.newNeed(
                    behov = listOf("Medlemskap"),
                    map = mapOf(
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "fødselsnummer" to fødselsnummer,
                        "medlemskapPeriodeFom" to skjæringstidspunkt.toString(),
                        "medlemskapPeriodeTom" to skjæringstidspunkt.toString(),
                        "opprinneligBehov" to jacksonObjectMapper().readTree(packet.toJson())
                    )
                )
                sikkerLogg.info("Trenger medlemskap for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Publiserer nytt behov:\n\t${utgåendeBehov.toJson()}")
                context.publish(utgåendeBehov.toJson())
            }
        }
    }
}
