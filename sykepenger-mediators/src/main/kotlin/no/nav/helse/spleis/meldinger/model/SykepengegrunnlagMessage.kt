package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.helse.hendelser.Sykepengegrunnlag
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.Behov.Behovstype
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

internal class SykepengegrunnlagMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : BehovMessage(packet) {
    private val sykepengegrunnlag =
        Sykepengegrunnlag(
            meldingsreferanseId = meldingsporing.id,
            vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID(),
            skjæringstidspunkt = packet["${Behovstype.Sykepengegrunnlag.utgåendeNavn}.skjæringstidspunkt"].asLocalDate(),
            behandlingsporing = packet.yrkesaktivitetssporing,
        )

    override fun behandle(
        mediator: IHendelseMediator,
        context: BehandlingContext,
    ) {
        mediator.behandle(this, sykepengegrunnlag, context)
    }
}
