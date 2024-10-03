package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.hendelser.Simulering
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.SimuleringMessage.Simuleringstatus.OK
import no.nav.helse.spleis.meldinger.model.SimuleringMessage.Simuleringstatus.valueOf

internal class SimuleringMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())

    private val fagsystemId = packet["Simulering.fagsystemId"].asText()
    private val fagområde = packet["Simulering.fagområde"].asText()
    private val status = valueOf(packet["@løsning.${Behovtype.Simulering.name}.status"].asText())
    private val simuleringOK = status == OK
    private val melding = packet["@løsning.${Behovtype.Simulering.name}.feilmelding"].asText() + " (status=$status)"
    private val simuleringResultat =
        packet["@løsning.${Behovtype.Simulering.name}.simulering"].takeUnless(JsonNode::isMissingOrNull)
            ?.let {
                SimuleringResultatDto(
                    totalbeløp = it.path("totalBelop").asInt(),
                    perioder = it.path("periodeList").map { periode ->
                        SimuleringResultatDto.SimulertPeriode(
                            fom = periode.path("fom").asLocalDate(),
                            tom = periode.path("tom").asLocalDate(),
                            utbetalinger = periode.path("utbetaling").map { utbetaling ->
                                SimuleringResultatDto.SimulertUtbetaling(
                                    forfallsdato = utbetaling.path("forfall").asLocalDate(),
                                    utbetalesTil = SimuleringResultatDto.Mottaker(
                                        id = utbetaling.path("utbetalesTilId").asText(),
                                        navn = utbetaling.path("utbetalesTilNavn").asText()
                                    ),
                                    feilkonto = utbetaling.path("feilkonto").asBoolean(),
                                    detaljer = utbetaling.path("detaljer").map { detalj ->
                                        SimuleringResultatDto.Detaljer(
                                            fom = detalj.path("faktiskFom").asLocalDate(),
                                            tom = detalj.path("faktiskTom").asLocalDate(),
                                            konto = detalj.path("konto").asText(),
                                            beløp = detalj.path("belop").asInt(),
                                            klassekode = SimuleringResultatDto.Klassekode(
                                                kode = detalj.path("klassekode").asText(),
                                                beskrivelse = detalj.path("klassekodeBeskrivelse").asText()
                                            ),
                                            uføregrad = detalj.path("uforegrad").asInt(),
                                            utbetalingstype = detalj.path("utbetalingsType").asText(),
                                            refunderesOrgnummer = detalj.path("refunderesOrgNr").asText(),
                                            tilbakeføring = detalj.path("tilbakeforing").asBoolean(),
                                            sats = SimuleringResultatDto.Sats(
                                                sats = detalj.path("sats").asDouble(),
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
            fagsystemId = fagsystemId,
            fagområde = fagområde,
            simuleringOK = simuleringOK,
            melding = melding,
            simuleringsResultat = simuleringResultat,
            utbetalingId = utbetalingId
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, simulering, context)
    }

    internal enum class Simuleringstatus {
        OK, FUNKSJONELL_FEIL, TEKNISK_FEIL, OPPDRAG_UR_ER_STENGT
    }
}
