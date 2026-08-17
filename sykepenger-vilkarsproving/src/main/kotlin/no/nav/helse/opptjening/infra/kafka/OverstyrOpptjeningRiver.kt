package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.bootstrap.sikkerLogg

/**
 * Lytter på saksbehandlers overstyring av § 8-2 (opptjeningskravet) fra avslag til innvilgelse.
 *
 * Triggere: `{ "@event_name": "saksbehandler_opptjeningsoverstyring" }`
 *
 * Det foreligger ikke noe faktabasert grunnlag ved manuell overstyring; saksbehandleren dokumenterer
 * sin vurdering i fritekst.
 *
 * Ved vellykket overstyring publiseres `opptjeningsvurdering_manuelt_overstyrt` med den nye vurderingens id.
 * Spleis kan bruke denne til å hente resultatet via `OpptjeningsvurderingResultat`-behovet.
 */
internal class OverstyrOpptjeningRiver(
    rapidsConnection: RapidsConnection,
    private val opptjeningService: OpptjeningService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "saksbehandler_opptjeningsoverstyring") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("skjæringstidspunkt") }
            validate { it.requireKey("saksbehandlerIdent") }
            validate { it.requireKey("begrunnelse") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
        val saksbehandlerIdent = packet["saksbehandlerIdent"].asText()
        val begrunnelse = packet["begrunnelse"].asText()

        sikkerLogg.info(
            "Mottatt manuell opptjeningsoverstyring for fødselsnummer $fødselsnummer " +
            "med skjæringstidspunkt $skjæringstidspunkt fra saksbehandler $saksbehandlerIdent"
        )

        val vurderingId = opptjeningService.overstyrOpptjening(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            saksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = begrunnelse
        )

        val utgående = JsonMessage.newMessage(
            eventName = "opptjeningsvurdering_manuelt_overstyrt",
            map = mapOf(
                "fødselsnummer" to fødselsnummer,
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "opptjeningsvurderingId" to vurderingId.toString(),
                "ok" to true
            )
        )
        sikkerLogg.info(
            "Publiserer opptjeningsvurdering_manuelt_overstyrt for fødselsnummer $fødselsnummer. " +
            "VurderingId: $vurderingId. Melding:\n\t${utgående.toJson()}"
        )
        context.publish(fødselsnummer, utgående.toJson())
    }
}
