package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsavklaringspengerV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.DagpengerV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForBeregning
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.SelvstendigForsikring
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
        ArbeidsavklaringspengerV2,
        InntekterForBeregning,
        DagpengerV2
    )

    override val riverName = "Ytelser"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")

        // Foreldrepenger (& Svangerskapspenger)
        message.requireKey("@løsning.${Foreldrepenger.name}")
        message.interestedInArray("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse.perioder") {
            validerGradertPeriode()
        }
        message.interestedInArray("@løsning.${Foreldrepenger.name}.Svangerskapsytelse.perioder") {
            validerGradertPeriode()
        }

        // Kapittel 9 ytelser
        message.requireArray("@løsning.${Pleiepenger.name}") {
            validerGradertPeriode()
        }
        message.requireArray("@løsning.${Omsorgspenger.name}") {
            validerGradertPeriode()
        }
        message.requireArray("@løsning.${Opplæringspenger.name}") {
            validerGradertPeriode()
        }

        // Dagpenger & AAP
        message.requireArray("@løsning.${DagpengerV2.name}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }

        message.requireArray("@løsning.${ArbeidsavklaringspengerV2.name}.utbetalingsperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }

        // Ting som ikke har noe med ytelser å gjøre
        message.requireArray("@løsning.${Institusjonsopphold.name}") {
            interestedIn("startdato") { it.asLocalDate() }
            interestedIn("faktiskSluttdato") { it.asLocalDate() }
        }

        message.requireArray("@løsning.${InntekterForBeregning.name}.inntekter") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
            require("inntektskilde", {
                check(it.asText().isNotBlank())
            })
            interestedIn("daglig", JsonNode::asDouble)
            interestedIn("måndelig", JsonNode::asDouble)
            interestedIn("årlig", JsonNode::asDouble)
        }

        message.interestedInArray("@løsning.${SelvstendigForsikring.name}") {
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
