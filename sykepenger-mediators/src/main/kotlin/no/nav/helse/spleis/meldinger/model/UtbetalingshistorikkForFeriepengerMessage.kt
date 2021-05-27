package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.SykepengehistorikkForFeriepenger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import java.time.Year

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkForFeriepengerMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val aktørId = packet["aktørId"].asText()
    private val skalBeregnesManuelt = packet["@løsning.${SykepengehistorikkForFeriepenger.name}.feriepengerSkalBeregnesManuelt"].asBoolean()

    private val utbetalinger = packet["@løsning.${SykepengehistorikkForFeriepenger.name}.utbetalinger"]
        .filter(::erGyldigPeriode)
        .mapNotNull { utbetaling ->
            val fom = utbetaling["fom"].asLocalDate()
            val tom = utbetaling["tom"].asLocalDate()
            when (utbetaling["typeKode"].asText()) {
                "0", "1" -> {
                    val beløp = utbetaling["dagsats"].asInt()
                    val orgnummer = utbetaling["orgnummer"].asText()
                    val utbetalt = utbetaling["utbetalt"].asLocalDate()
                    UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Personutbetalingsperiode(orgnummer, fom, tom, beløp, utbetalt)
                }
                "5", "6" -> {
                    val beløp = utbetaling["dagsats"].asInt()
                    val orgnummer = utbetaling["orgnummer"].asText()
                    val utbetalt = utbetaling["utbetalt"].asLocalDate()
                    UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(orgnummer, fom, tom, beløp, utbetalt)
                }
                else -> null
            }
        }

    private val feriepengehistorikk = packet["@løsning.${SykepengehistorikkForFeriepenger.name}.feriepengehistorikk"]
        .map { feriepenge ->
            UtbetalingshistorikkForFeriepenger.Feriepenger(
                orgnummer = feriepenge["orgnummer"].asText(),
                beløp = feriepenge["beløp"].asInt(),
                fom = feriepenge["fom"].asLocalDate(),
                tom = feriepenge["tom"].asLocalDate()
            )
        }

    private val arbeidskategorikoder = packet["@løsning.${SykepengehistorikkForFeriepenger.name}.arbeidskategorikoder"]
        .map {
            val fom = it["fom"].asLocalDate()
            val tom = it["tom"].asLocalDate()
            val kode = it["kode"].asText()
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                periode = Periode(fom, tom),
                arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.finn(kode)
            )
        }
        .let(UtbetalingshistorikkForFeriepenger::Arbeidskategorikoder)

    private val opptjeningsår = packet["${SykepengehistorikkForFeriepenger.name}.historikkFom"]
        .asLocalDate()
        .let(Year::from)

    private fun erGyldigPeriode(node: JsonNode): Boolean {
        val fom = node["fom"].asOptionalLocalDate()
        val tom = node["tom"].asOptionalLocalDate()
        return fom != null && tom != null && fom <= tom
    }

    private fun utbetalingshistorikkForFeriepenger(aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = skalBeregnesManuelt,
            aktivitetslogg = aktivitetslogg
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingshistorikkForFeriepenger())
    }
}
