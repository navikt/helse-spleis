package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsledig
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class AvbruttArbeidsledigSøknadMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val organisasjonsnummer = packet["tidligereArbeidsgiverOrgnummer"].asText(Arbeidsledig)
    private val periode = packet["fom"].asLocalDate() til packet["tom"].asLocalDate()

    private val avbruttSøknad
        get() = AvbruttSøknad(
            meldingsreferanseId = meldingsporing.id,
            aktørId = meldingsporing.aktørId,
            fødselsnummer = meldingsporing.fødselsnummer,
            orgnummer = organisasjonsnummer,
            periode = periode
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, avbruttSøknad, context)
    }
}
