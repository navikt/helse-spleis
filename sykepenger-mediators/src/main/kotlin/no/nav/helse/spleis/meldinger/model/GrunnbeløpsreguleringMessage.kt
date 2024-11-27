package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class GrunnbeløpsreguleringMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : HendelseMessage(packet) {
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()

    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext,
    ) {
        mediator.behandle(
            this,
            Grunnbeløpsregulering(
                meldingsporing.id,
                skjæringstidspunkt,
                opprettet,
            ),
            context,
        )
    }
}
