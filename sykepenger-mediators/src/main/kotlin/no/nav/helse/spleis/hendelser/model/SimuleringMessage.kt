package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

internal class SimuleringMessage(originalMessage: String, problems: MessageProblems) :
    BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Behovtype.Simulering)
        requireKey("@løsning.${Behovtype.Simulering.name}.status")
        require("@løsning.${Behovtype.Simulering.name}") {
            if (it["status"].asText() == "OK") {
                requireKey("@løsning.${Behovtype.Simulering.name}.simulering")
                interestedIn("@løsning.${Behovtype.Simulering.name}.feilmelding")
            } else {
                requireKey("@løsning.${Behovtype.Simulering.name}.feilmelding")
                interestedIn("@løsning.${Behovtype.Simulering.name}.simulering")
            }
        }
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    fun asSimulering(): Simulering {
        return Simulering(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            orgnummer = this["organisasjonsnummer"].asText(),
            simuleringOK = this["@løsning.${Behovtype.Simulering.name}.status"].asText() == "OK",
            melding = this["@løsning.${Behovtype.Simulering.name}.feilmelding"].asText(),
            simuleringResultat = this["@løsning.${Behovtype.Simulering.name}.simulering"].takeUnless(JsonNode::isMissingOrNull)
                ?.let {
                    Simulering.SimuleringResultat(
                        totalbeløp = it.path("totalBelop").asInt(),
                        perioder = it.path("periodeList").map { periode ->
                            Simulering.SimulertPeriode(
                                periode = Periode(periode.path("fom").asLocalDate(), periode.path("tom").asLocalDate()),
                                utbetalinger = periode.path("utbetaling").map { utbetaling ->
                                    Simulering.SimulertUtbetaling(
                                        forfallsdato = utbetaling.path("forfall").asLocalDate(),
                                        utbetalesTil = Simulering.Mottaker(
                                            id = utbetaling.path("utbetalesTilId").asText(),
                                            navn = utbetaling.path("utbetalesTilNavn").asText()
                                        ),
                                        feilkonto = utbetaling.path("feilkonto").asBoolean(),
                                        detaljer = utbetaling.path("detaljer").map { detalj ->
                                            Simulering.Detaljer(
                                                periode = Periode(detalj.path("faktiskFom").asLocalDate(), detalj.path("faktiskTom").asLocalDate()),
                                                konto = detalj.path("konto").asText(),
                                                beløp = detalj.path("belop").asInt(),
                                                klassekode = Simulering.Klassekode(
                                                    kode = detalj.path("klassekode").asText(),
                                                    beskrivelse = detalj.path("klassekodeBeskrivelse").asText()
                                                ),
                                                uføregrad = detalj.path("uforegrad").asInt(),
                                                utbetalingstype = detalj.path("utbetalingsType").asText(),
                                                refunderesOrgnummer = detalj.path("refunderesOrgNr").asText(),
                                                tilbakeføring = detalj.path("tilbakeforing").asBoolean(),
                                                sats = Simulering.Sats(
                                                    sats = detalj.path("sats").asInt(),
                                                    antall = detalj.path("antallSats").asInt(),
                                                    type = detalj.path("typeSats").asText()
                                                )
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
        )
    }

    object Factory : MessageFactory<HendelseMessage> {
        override fun createMessage(message: String, problems: MessageProblems): HendelseMessage {
            return SimuleringMessage(message, problems)
        }
    }
}
