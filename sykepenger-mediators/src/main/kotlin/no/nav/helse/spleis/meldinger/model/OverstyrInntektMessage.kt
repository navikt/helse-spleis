package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class OverstyrInntektMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val månedligInntekt = packet["månedligInntekt"].asDouble()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val forklaring = packet["forklaring"].asText()
    private val subsumsjon = packet["subsumsjon"].takeUnless(JsonNode::isMissingOrNull)?.let {
        Subsumsjon(
            paragraf = it["paragraf"].asText(),
            ledd = it.path("ledd").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
            bokstav = it.path("bokstav").takeUnless(JsonNode::isMissingOrNull)?.asText()
        )
    }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, OverstyrInntekt(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                inntekt = månedligInntekt.månedlig,
                skjæringstidspunkt = skjæringstidspunkt,
                subsumsjon = subsumsjon,
                forklaring = forklaring
            ),
            context
        )
}
