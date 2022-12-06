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

internal class OverstyrArbeidsgiveropplysningerMessage(private val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val arbeidsgiveropplysninger = packet["arbeidsgiveropplysninger"].asArbeidsgiveropplysninger()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, OverstyrArbeidsgiveropplysninger(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiveropplysninger = arbeidsgiveropplysninger
            ),
            context
        )

    private fun JsonNode.asArbeidsgiveropplysninger() =
        this.fields().asSequence().toList().map { (orgnummer, arbeidsgiveropplysning) ->
            val månedligInntekt = arbeidsgiveropplysning["månedligInntekt"].asDouble().månedlig
            val forklaring = arbeidsgiveropplysning["forklaring"].asText()
            val subsumsjon = arbeidsgiveropplysning["subsumsjon"].asSubsumsjon()

            val saksbehandlerinntekt = Saksbehandler(skjæringstidspunkt, id, månedligInntekt, forklaring, subsumsjon, opprettet)
            val refusjonsopplysninger = arbeidsgiveropplysning["refusjonsopplysninger"].asRefusjonsopplysninger()

            ArbeidsgiverInntektsopplysning(orgnummer, saksbehandlerinntekt, refusjonsopplysninger)
        }

    private fun JsonNode.asSubsumsjon() = this.takeUnless(JsonNode::isMissingOrNull)?.let {
        Subsumsjon(
            paragraf = it["paragraf"].asText(),
            ledd = it.path("ledd").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
            bokstav = it.path("bokstav").takeUnless(JsonNode::isMissingOrNull)?.asText()
        )
    }

    private fun JsonNode.asRefusjonsopplysninger() = RefusjonsopplysningerBuilder().also { builder ->
        this.map { refusjonsopplysning ->
            builder.leggTil(
                Refusjonsopplysning(
                    meldingsreferanseId = id,
                    fom = refusjonsopplysning.path("fom").asLocalDate(),
                    tom = refusjonsopplysning.path("tom").asOptionalLocalDate(),
                    beløp = refusjonsopplysning.path("beløp").asDouble().månedlig
                ), opprettet)
        }
    }.build()

}


