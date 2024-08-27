package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.til
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class AvbruttSøknadMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktorId"].asText()
    private val organisasjonsnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val periode = packet["fom"].asLocalDate() til packet["tom"].asLocalDate()
    override val fødselsnummer: String = packet["fnr"].asText()

    private val avbruttSøknad
        get() = AvbruttSøknad(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            periode = periode
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, avbruttSøknad, context)
    }
}
