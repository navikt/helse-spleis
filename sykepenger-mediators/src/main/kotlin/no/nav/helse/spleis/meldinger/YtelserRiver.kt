package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.Toggle
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsavklaringspengerV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger
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
    override val behov = buildList {
        if (Toggle.ArbeidsavklaringspengerV2.enabled) {
            add(ArbeidsavklaringspengerV2)
        }

        addAll(
            listOf(
                Foreldrepenger,
                Pleiepenger,
                Omsorgspenger,
                Opplæringspenger,
                Institusjonsopphold,
                Arbeidsavklaringspenger,
                InntekterForBeregning,
                Dagpenger
            )
        )
    }

    override val riverName = "Ytelser"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")
        message.requireKey("@løsning.${Foreldrepenger.name}")
        message.requireKey("@løsning.${Pleiepenger.name}")
        message.requireKey("@løsning.${Omsorgspenger.name}")
        message.requireKey("@løsning.${Opplæringspenger.name}")
        message.requireKey("@løsning.${Institusjonsopphold.name}")
        message.interestedIn("@løsning.${SelvstendigForsikring.name}")
        message.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse")
        message.interestedIn("@løsning.${Foreldrepenger.name}.Svangerskapsytelse")
        message.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse.fom") { it.asLocalDate() }
        message.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse.tom") { it.asLocalDate() }
        message.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse.perioder") { perioder ->
            (perioder as ArrayNode).forEach {
                it.path("fom").asLocalDate()
                it.path("tom").asLocalDate()
                it.path("grad").asInt()
            }
        }
        message.requireArray("@løsning.${Pleiepenger.name}") {
            interestedIn("fom") { it.asLocalDate() }
            interestedIn("tom") { it.asLocalDate() }
            interestedIn("grad") { it.asInt() }
        }
        message.requireArray("@løsning.${Omsorgspenger.name}") {
            interestedIn("fom") { it.asLocalDate() }
            interestedIn("tom") { it.asLocalDate() }
            interestedIn("grad") { it.asInt() }
        }
        message.requireArray("@løsning.${Opplæringspenger.name}") {
            interestedIn("fom") { it.asLocalDate() }
            interestedIn("tom") { it.asLocalDate() }
            interestedIn("grad") { it.asInt() }
        }
        message.requireArray("@løsning.${Institusjonsopphold.name}") {
            interestedIn("startdato") { it.asLocalDate() }
            interestedIn("faktiskSluttdato") { it.asLocalDate() }
        }
        message.requireArray("@løsning.${Arbeidsavklaringspenger.name}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("@løsning.${Dagpenger.name}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
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
        if (Toggle.ArbeidsavklaringspengerV2.enabled) {
            message.requireArray("@løsning.${ArbeidsavklaringspengerV2.name}.utbetalingsperioder") {
                require("fom", JsonNode::asLocalDate)
                require("tom", JsonNode::asLocalDate)
            }
        }

        message.interestedIn("@løsning.${SelvstendigForsikring.name}") { forsikringer ->
            forsikringer as ArrayNode
            forsikringer.forEach {
                val feltnavn = it.fieldNames().asSequence().toSet()
                it as ObjectNode
                check("forsikringstype" in feltnavn)
                check("startdato" in feltnavn)
                it.path("startdato").asLocalDate()
                it.path("sluttdato").asOptionalLocalDate()
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = YtelserMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
    )
}
