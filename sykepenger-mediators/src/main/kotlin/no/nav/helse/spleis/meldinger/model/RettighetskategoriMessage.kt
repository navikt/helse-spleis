package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.helse.hendelser.Rettighetskategori
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.Behov.Behovstype
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

internal class RettighetskategoriMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : BehovMessage(packet) {
    private val rettighetskategori =
        Rettighetskategori(
            meldingsreferanseId = meldingsporing.id,
            vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID(),
            skjæringstidspunkt = packet["${Behovstype.Rettighetskategori.utgåendeNavn}.skjæringstidspunkt"].asLocalDate(),
            behandlingsporing = packet.yrkesaktivitetssporing,
            rettighetskategoriId = packet["@løsning.${Behovstype.Rettighetskategori.utgåendeNavn}.id"].asText().toUUID(),
            hovedkategori = Hovedkategori.valueOf(packet["@løsning.${Behovstype.Rettighetskategori.utgåendeNavn}.hovedkategori"].asText()),
        )

    override fun behandle(
        mediator: IHendelseMediator,
        context: BehandlingContext,
    ) {
        mediator.behandle(this, rettighetskategori, context)
    }
}
