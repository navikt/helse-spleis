package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SykepengegrunnlagForArbeidsgiverMessage

internal class SykepengegrunnlagForArbeidsgiverRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(InntekterForSykepengegrunnlagForArbeidsgiver)

    override val riverName = "Sykepengegrunnlag for Arbeidsgiver"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand")
        message.require("${InntekterForSykepengegrunnlagForArbeidsgiver.name}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.requireArray("@løsning.${InntekterForSykepengegrunnlagForArbeidsgiver.name}") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "aktørId", "fordel", "beskrivelse")
            }
            requireArray("arbeidsforholdliste") {
                requireKey("orgnummer", "type")
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = SykepengegrunnlagForArbeidsgiverMessage(packet)
}
