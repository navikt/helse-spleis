package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.hendelser.RettighetskategoriEndret
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class RettighetskategoriEndretMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : HendelseMessage(packet) {
    internal val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    internal val rettighetskategoriId = packet["rettighetskategoriId"].asText().toUUID()
    internal val hovedkategori = Hovedkategori.valueOf(packet["hovedkategori"].asText())

    override fun behandle(
        mediator: IHendelseMediator,
        context: BehandlingContext,
    ) {
        mediator.behandle(
            message = this,
            rettighetskategoriEndret =
                RettighetskategoriEndret(
                    meldingsreferanseId = meldingsporing.id,
                    skjæringstidspunkt = skjæringstidspunkt,
                    rettighetskategoriId = rettighetskategoriId,
                    hovedkategori = hovedkategori,
                ),
            context = context,
        )
    }
}
