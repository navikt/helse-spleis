package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.til
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class ForkastSykmeldingsperioderMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val periode = packet["fom"].asLocalDate() til packet["tom"].asLocalDate()

    private val forkastSykmeldingsperioder
        get() = ForkastSykmeldingsperioder(
            meldingsreferanseId = meldingsporing.id,
            organisasjonsnummer = organisasjonsnummer,
            periode = periode
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, forkastSykmeldingsperioder, context)
    }
}
