package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.VilkårsvurderingRepository
import no.nav.helse.opptjening.bootstrap.sikkerLogg
import no.nav.helse.opptjening.domain.Opptjening
import no.nav.helse.opptjening.domain.Utfall

internal class OpptjeningsvurderingResultatRiver(rapidsConnection: RapidsConnection, private val vilkårsvurderingRepository: VilkårsvurderingRepository): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAllOrAny("@behov", listOf("OpptjeningsvurderingResultat"))
                it.requireKey("opptjeningsvurderingId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val opptjeningsvurderingId = packet["opptjeningsvurderingId"].asUUID()
        sikkerLogg.info("Mottatt behov for OpptjeningsvurderingResultat for opptjeningsvurderingId $opptjeningsvurderingId")
        val vurdering = vilkårsvurderingRepository.finn<Opptjening>(opptjeningsvurderingId) ?: error("Finner ikke opptjening")
        val kodeverkkode = vurdering.kodeverkkode ?: error("Det skal ikke være mulig å spørre om en vurdering der kodeverkkode ikke er satt")

        val opptjeningOk = when (kodeverkkode.utfall) {
            Utfall.Oppfylt -> true
            Utfall.IkkeOppfylt -> false
        }

        packet["@løsning"] =
            mapOf(
                "OpptjeningsvurderingResultat" to mapOf(
                    "ok" to opptjeningOk,
                ),
            )
        sikkerLogg.info("Publiserer løsning for OpptjeningsvurderingResultat for opptjeningsvurderingId $opptjeningsvurderingId. Løsning:\n\t${packet.toJson()}")
        context.publish(packet.toJson())
    }
}
