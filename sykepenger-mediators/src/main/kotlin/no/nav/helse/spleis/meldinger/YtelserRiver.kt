package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Arbeidsavklaringspenger
import no.nav.helse.spleis.Behov.Behovstype.Dagpenger
import no.nav.helse.spleis.Behov.Behovstype.Foreldrepenger
import no.nav.helse.spleis.Behov.Behovstype.InntekterForBeregning
import no.nav.helse.spleis.Behov.Behovstype.Institusjonsopphold
import no.nav.helse.spleis.Behov.Behovstype.Omsorgspenger
import no.nav.helse.spleis.Behov.Behovstype.Opplæringspenger
import no.nav.helse.spleis.Behov.Behovstype.Pleiepenger
import no.nav.helse.spleis.Behov.Behovstype.SelvstendigForsikring
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.YtelserMessage

internal class YtelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(
        Foreldrepenger,
        Pleiepenger,
        Omsorgspenger,
        Opplæringspenger,
        Institusjonsopphold,
        Arbeidsavklaringspenger,
        InntekterForBeregning,
        Dagpenger
    )

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

        message.interestedInArrayEllerObjectMedArray("@løsning.${SelvstendigForsikring.utgåendeNavn}", "forsikringer") {
            require("startdato", JsonNode::asLocalDate)
            require("forsikringstype", JsonNode::asText)
            require("premiegrunnlag", JsonNode::asInt)
            interestedIn("sluttdato", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = YtelserMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
    )

    private fun JsonMessage.validerGradertPeriode() {
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
        requireKey("grad")
    }
}
