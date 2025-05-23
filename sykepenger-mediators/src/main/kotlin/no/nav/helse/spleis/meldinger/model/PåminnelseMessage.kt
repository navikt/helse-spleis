package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val yrkesaktivitetssporing = packet.yrkesaktivitetssporing
    private val antallGangerPåminnet = packet["antallGangerPåminnet"].asInt()
    private val tilstand = TilstandType.valueOf(packet["tilstand"].asText())
    private val tilstandsendringstidspunkt = packet["tilstandsendringstidspunkt"].asLocalDateTime()
    private val påminnelsestidspunkt = packet["påminnelsestidspunkt"].asLocalDateTime()
    private val nestePåminnelsestidspunkt = packet["nestePåminnelsestidspunkt"].asLocalDateTime()
    private val flagg = packet["flagg"].map { it.asText() }.toSet()

    private val påminnelse get() = Påminnelse(
        meldingsreferanseId = meldingsporing.id,
        behandlingsporing = yrkesaktivitetssporing,
        vedtaksperiodeId = vedtaksperiodeId,
        antallGangerPåminnet = antallGangerPåminnet,
        tilstand = tilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        påminnelsestidspunkt = påminnelsestidspunkt,
        nestePåminnelsestidspunkt = nestePåminnelsestidspunkt,
        opprettet = opprettet,
        flagg = flagg
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, påminnelse, context)
    }
}
