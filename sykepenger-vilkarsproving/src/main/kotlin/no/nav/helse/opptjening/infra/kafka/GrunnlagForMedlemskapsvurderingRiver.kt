package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.MedlemskapService
import no.nav.helse.opptjening.bootstrap.sikkerLogg
import no.nav.helse.opptjening.domain.Medlemskapssvar

internal class GrunnlagForMedlemskapsvurderingRiver(
    rapidsConnection: RapidsConnection,
    private val medlemskapService: MedlemskapService
) : River.PacketListener {
    private val behovKey = "Medlemskap"

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
                it.requireAny("@løsning.$behovKey.resultat.svar", listOf("JA", "NEI"))
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val svar = when (val svar = packet["@løsning.$behovKey.resultat.svar"].asText()) {
            "JA" -> Medlemskapssvar.Ja
            "NEI" -> Medlemskapssvar.Nei
            else -> error("har ikke mappingregel for medlemskapssvar: $svar")
        }
        sikkerLogg.info("Mottatt løsning på behov for $behovKey for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Svar: $svar")

        val resultat = medlemskapService.behandleGrunnlagForMedlemskapsvurdering(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            svar = svar
        )
        when (resultat) {
            MedlemskapService.BehandleGrunnlagResultat.AlleredeVurdert -> {
                sikkerLogg.warn("Allerede vurdert for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Ingen ny vurdering foretatt.")
            }

            is MedlemskapService.BehandleGrunnlagResultat.NyVurderingForetatt -> {
                sikkerLogg.info("Ny vurdering foretatt for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${resultat.vurderingId}")
                val opprinneligBehov = packet["opprinneligBehov"] as ObjectNode
                val løsning = opprinneligBehov.putObject("@løsning")
                løsning.putObject("Medlemskapsvurdering")
                    .put("id", resultat.vurderingId.toString())
                val løsningString = opprinneligBehov.toString()
                sikkerLogg.info("Publiserer løsning på behov for medlemskapsvurdering for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${resultat.vurderingId}. Løsning:\n\t$løsningString")
                context.publish(løsningString)
            }

            MedlemskapService.BehandleGrunnlagResultat.IngenPrøvingFunnet -> {
                sikkerLogg.warn("Ingen prøving funnet for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt.")
            }
        }
    }
}
