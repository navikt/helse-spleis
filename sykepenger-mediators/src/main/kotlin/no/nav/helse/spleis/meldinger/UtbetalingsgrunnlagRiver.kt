package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingsgrunnlagMessage

internal class UtbetalingsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(InntekterForSykepengegrunnlag)

    override val riverName = "Utbetalingsgrunnlag"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand")
        message.requireArray("@løsning.${InntekterForSykepengegrunnlag.name}") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "aktørId", "fordel", "beskrivelse")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingsgrunnlagMessage(packet)
}
