package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.SimuleringMessage.Simuleringstatus.*

internal class SimuleringMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()

    private val status = valueOf(packet["@løsning.${Behovtype.Simulering.name}.status"].asText())
    private val simuleringOK = status == OK
    private val melding = packet["@løsning.${Behovtype.Simulering.name}.feilmelding"].asText()
    private val simuleringResultat =
        packet["@løsning.${Behovtype.Simulering.name}.simulering"].takeUnless(JsonNode::isMissingOrNull)
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
                                            periode = Periode(
                                                detalj.path("faktiskFom").asLocalDate(),
                                                detalj.path("faktiskTom").asLocalDate()
                                            ),
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

    private val simulering
        get() = Simulering(
            meldingsreferanseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            simuleringOK = simuleringOK,
            melding = melding,
            simuleringResultat = simuleringResultat
        )

    override fun behandle(mediator: IHendelseMediator) {
        // dont send message into the model if Oppdrag/UR is closed for biz.
        if (status in listOf(TEKNISK_FEIL, OPPDRAG_UR_ER_STENGT)) return
        mediator.behandle(this, simulering)
    }

    internal enum class Simuleringstatus {
        OK, FUNKSJONELL_FEIL, TEKNISK_FEIL, OPPDRAG_UR_ER_STENGT
    }
}
