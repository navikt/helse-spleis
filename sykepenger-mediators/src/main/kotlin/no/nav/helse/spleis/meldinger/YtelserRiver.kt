package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.Toggle
import no.nav.helse.hendelser.GraderteAndreYtelserType
import no.nav.helse.spleis.Behov.Behovstype.*
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning as GraderteAndreYtelserForBeregningHendelse

internal class YtelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov =
        buildList {
            addAll(
                listOf(
                    Foreldrepenger,
                    Pleiepenger,
                    Omsorgspenger,
                    Opplæringspenger,
                    Institusjonsopphold,
                    Arbeidsavklaringspenger,
                    InntekterForBeregning,
                    Dagpenger,
                    OpptjeningsvurderingResultat,
                ),
            )
            if (Toggle.GraderteAndreYtelser.enabled) add(GraderteAndreYtelserForBeregning)
        }

    override val riverName = "Ytelser"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")

        // Foreldrepenger (& Svangerskapspenger)
        message.requireKey("@løsning.${Foreldrepenger.utgåendeNavn}")
        message.interestedInArray("@løsning.${Foreldrepenger.utgåendeNavn}.Foreldrepengeytelse.perioder") {
            validerGradertPeriode()
        }
        message.interestedInArray("@løsning.${Foreldrepenger.utgåendeNavn}.Svangerskapsytelse.perioder") {
            validerGradertPeriode()
        }

        // Kapittel 9 ytelser
        message.requireArrayEllerObjectMedArray("@løsning.${Pleiepenger.utgåendeNavn}", "perioder") {
            validerGradertPeriode()
        }
        message.requireArrayEllerObjectMedArray("@løsning.${Omsorgspenger.utgåendeNavn}", "perioder") {
            validerGradertPeriode()
        }
        message.requireArrayEllerObjectMedArray("@løsning.${Opplæringspenger.utgåendeNavn}", "perioder") {
            validerGradertPeriode()
        }

        if (Toggle.GraderteAndreYtelser.enabled) {
            message.interestedInArray("@løsning.${GraderteAndreYtelserForBeregning.utgåendeNavn}") {
                require("yrkesaktivitet") { GraderteAndreYtelserForBeregningHendelse.yrkesaktivitet(it.asText()) }
                require("graderteAndreYtelserType") { GraderteAndreYtelserType.valueOf(it.asText()) }
                requireArray("graderteAndreYtelserPerioder") {
                    validerGradertPeriode()
                }
            }
        }

        message.requireKey("@løsning.${OpptjeningsvurderingResultat.utgåendeNavn}.ok")

        // Dagpenger & AAP
        message.requireArray("@løsning.${Dagpenger.utgåendeNavn}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }

        message.requireArray("@løsning.${Arbeidsavklaringspenger.utgåendeNavn}.utbetalingsperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }

        // Ting som ikke har noe med ytelser å gjøre
        message.requireArrayEllerObjectMedArray("@løsning.${Institusjonsopphold.utgåendeNavn}", "perioder") {
            require("startdato", JsonNode::asLocalDate)
            interestedIn("faktiskSluttdato") { it.asLocalDate() }
        }

        message.requireArray("@løsning.${InntekterForBeregning.utgåendeNavn}.inntekter") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
            require("inntektskilde", {
                check(it.asText().isNotBlank())
            })
            interestedIn("daglig", JsonNode::asDouble)
            interestedIn("måndelig", JsonNode::asDouble)
            interestedIn("årlig", JsonNode::asDouble)
        }

        message.interestedIn("@løsning.${ForsikringsvurderingResultat.utgåendeNavn}") {
            message.requireKey(
                "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.forsikringsvurderingId",
                "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.villeHattForsikringOmDenVarBetalt",
                "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.harForsikringSomIkkePasserMedSøknadstype",
                "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.harIndividuellForsikring",
            )
            message.interestedIn("@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.dekning") {
                message.requireKey("@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.dekning.grad", "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.dekning.iVentetid")
            }
            message.interestedIn("@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.opphørsdato")
        }
    }

    override fun createMessage(packet: JsonMessage) =
        YtelserMessage(
            packet = packet,
            meldingsporing =
                Meldingsporing(
                    id = packet.meldingsreferanseId(),
                    fødselsnummer = packet["fødselsnummer"].asText(),
                ),
        )

    private fun JsonMessage.validerGradertPeriode() {
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
        requireKey("grad")
    }
}
