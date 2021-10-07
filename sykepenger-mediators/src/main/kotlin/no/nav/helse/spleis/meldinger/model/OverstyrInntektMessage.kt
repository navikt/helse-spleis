package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class OverstyrInntektMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val månedligInntekt = packet["månedligInntekt"].asDouble()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator) =
        mediator.behandle(
            this, OverstyrInntekt(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                inntekt = månedligInntekt.månedlig,
                skjæringstidspunkt = skjæringstidspunkt
            )

        )
}
