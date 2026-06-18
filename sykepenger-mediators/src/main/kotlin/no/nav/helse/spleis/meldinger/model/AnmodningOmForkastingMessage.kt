package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

// Understands a JSON message representing a Påminnelse
internal class AnmodningOmForkastingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().let { UUID.fromString(it) }
    private val force = packet["force"].takeIf { it.isBoolean }?.asBoolean() ?: false

    private val anmodning = AnmodningOmForkasting(
        meldingsreferanseId = meldingsporing.id,
        behandlingsporing = packet.yrkesaktivitetssporing,
        vedtaksperiodeId = vedtaksperiodeId,
        force = force
    )

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(this, anmodning, context)
    }
}
