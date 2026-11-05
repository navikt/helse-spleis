package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Rettighetskategori
import no.nav.helse.spleis.Behov.Behovstype.*
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import tools.jackson.databind.JsonNode
import java.util.*
import no.nav.helse.hendelser.Avslagsdager as AvslagsdagerHendelse
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
                interestedIn("organisasjonsnummer")
                require("yrkesaktivitetstype") {
                    GraderteAndreYtelserForBeregningHendelse.yrkesaktivitet(
                        yrkesaktivitetstype = it.asString(),
                        organisasjonsnummer = this["organisasjonsnummer"].takeUnless(JsonNode::isMissingOrNull)?.asString(),
                    )
                }
                require("versjon") { UUID.fromString(it.asString()) }
                requireArray("dager") {
                    require("dato", JsonNode::asLocalDate)
                    require("trekkFraSykdomsgrad", ::validerProsent)
                    require("sykdomsgradFørFordeling", ::validerProsent)
                }
            }
        }

        if (Toggle.Avslagsdagerbehov.enabled) {
            message.interestedIn("@løsning.${Avslagsdager.utgåendeNavn}") {
                message.require("@løsning.${Avslagsdager.utgåendeNavn}.vurderingIder") { vurderingIder ->
                    check(vurderingIder.isArray) { "Forventet vurderingIder som liste" }
                    vurderingIder.values().forEach { UUID.fromString(it.asString()) }
                }
                message.requireArray("@løsning.${Avslagsdager.utgåendeNavn}.avslagsdager") {
                    require("vurderingId") { UUID.fromString(it.asString()) }
                    require("dato", JsonNode::asLocalDate)
                    require("vilkårskoder") { vilkårskoder ->
                        check(vilkårskoder.isArray) { "Forventet vilkårskoder som liste" }
                        AvslagsdagerHendelse.begrunnelse(vilkårskoder.values().map { it.asString() })
                    }
                }
            }
        }

        message.requireKey("@løsning.${OpptjeningsvurderingResultat.utgåendeNavn}.ok")

        message.interestedIn("@løsning.${MedlemskapsvurderingResultat.utgåendeNavn}.svar")

        message.interestedIn("@løsning.${RettighetskategoriResultat.utgåendeNavn}.hovedkategori") { hovedkategori ->
            Rettighetskategori.Hovedkategori.valueOf(hovedkategori.asString())
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
                check(it.asString().isNotBlank())
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
                message.requireKey(
                    "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.dekning.grad",
                    "@løsning.${ForsikringsvurderingResultat.utgåendeNavn}.dekning.iVentetid",
                )
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
                    fødselsnummer = packet["fødselsnummer"].asString(),
                ),
        )

    private fun JsonMessage.validerGradertPeriode() {
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
        requireKey("grad")
    }

    private fun validerProsent(node: JsonNode) {
        check(node.isInt && node.asInt() in 0..100) { "Forventet heltall mellom 0 og 100" }
    }
}
