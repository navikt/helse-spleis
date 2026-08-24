package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import java.util.UUID
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.EndretVurderingPåSkjæringstidspunkt
import no.nav.helse.hendelser.Vurdering
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class EndretVurderingPåSkjæringstidspunktMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
    vurderingsformat: Vurderingsformat
) : HendelseMessage(packet) {

    private val vurderingId = UUID.fromString(packet[vurderingsformat.idPath].asText())
    internal val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    internal val vurdering = when (vurderingsformat) {
        Vurderingsformat.Forsikringsvudering -> Vurdering.Forsikringsvurdering(vurderingId)
        Vurderingsformat.Opptjeningsvurdering -> Vurdering.Opptjeningsvurdering(vurderingId)
    }
    internal val avsender = when (packet["manuellVurdering"].asBoolean(false)) {
        true -> Avsender.SAKSBEHANDLER
        false -> Avsender.SYSTEM
    }

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(
            message = this,
            endretVurderingPåSkjæringstidspunkt = EndretVurderingPåSkjæringstidspunkt(
                meldingsreferanseId = meldingsporing.id,
                skjæringstidspunkt = skjæringstidspunkt,
                endretVurdering = vurdering,
                avsender = avsender
            ),
            context = context
        )
    }

    sealed interface Vurderingsformat {
        val eventName: String
        val idPath: String
        data object Forsikringsvudering: Vurderingsformat {
            override val eventName = "endret_forsikringsvurdering"
            override val idPath = "forsikringsvurderingId"
        }
        data object Opptjeningsvurdering: Vurderingsformat {
            override val eventName = "endret_opptjeningsvurdering"
            override val idPath = "opptjeningsvurderingId"
        }
    }
}
