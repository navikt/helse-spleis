package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class OverstyrInntektMessage(private val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val månedligInntekt = packet["månedligInntekt"].asDouble().månedlig
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val forklaring = packet["forklaring"].asText()
    private val subsumsjon = packet["subsumsjon"].takeUnless(JsonNode::isMissingOrNull)?.let {
        Subsumsjon(
            paragraf = it["paragraf"].asText(),
            ledd = it.path("ledd").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
            bokstav = it.path("bokstav").takeUnless(JsonNode::isMissingOrNull)?.asText()
        )
    }

    private val refusjonsopplysninger = RefusjonsopplysningerBuilder().apply {
        packet["refusjonsopplysninger"].map { refusjonsopplysning ->
            leggTil(
                Refusjonsopplysning(
                meldingsreferanseId = id,
                fom = refusjonsopplysning.path("fom").asLocalDate(),
                tom = refusjonsopplysning.path("tom").asOptionalLocalDate(),
                beløp = refusjonsopplysning.path("beløp").asDouble().månedlig
            ), opprettet)
        }
    }.build()

    private val arbeidsgiverInntektsopplysning = ArbeidsgiverInntektsopplysning(organisasjonsnummer, Saksbehandler(skjæringstidspunkt, id, månedligInntekt, forklaring, subsumsjon, opprettet), refusjonsopplysninger)

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, OverstyrArbeidsgiveropplysninger(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiveropplysninger = listOf(arbeidsgiverInntektsopplysning)
            ),
            context
        )
}
