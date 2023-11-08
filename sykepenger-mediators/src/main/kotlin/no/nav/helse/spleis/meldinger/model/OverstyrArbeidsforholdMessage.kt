package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class OverstyrArbeidsforholdMessage(val packet: JsonMessage): HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val overstyrteArbeidsforhold = packet["overstyrteArbeidsforhold"]
        .map {
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                orgnummer = it["orgnummer"].asText(),
                deaktivert = it["deaktivert"].asBoolean(),
                forklaring = it["forklaring"].asText()
            )
        }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            this,
            OverstyrArbeidsforhold(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                skjæringstidspunkt = skjæringstidspunkt,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                opprettet = opprettet
            ),
            context
        )
    }

}
