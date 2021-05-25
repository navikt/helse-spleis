package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.SykepengehistorikkForFeriepenger
import no.nav.helse.person.infotrygdhistorikk.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
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
                    val grad = utbetaling["utbetalingsGrad"].asInt().prosent
                    // inntektbeløpet i Infotrygd-utbetalingene er gradert; justerer derfor "opp igjen"
                    val inntekt = utbetaling["dagsats"].asInt().daglig(grad)
                    val orgnummer = utbetaling["orgnummer"].asText()
                    PersonUtbetalingsperiode(orgnummer, fom, tom, grad, inntekt)
                }
                "5", "6" -> {
                    val grad = utbetaling["utbetalingsGrad"].asInt().prosent
                    // inntektbeløpet i Infotrygd-utbetalingene er gradert; justerer derfor "opp igjen"
                    val inntekt = utbetaling["dagsats"].asInt().daglig(grad)
                    val orgnummer = utbetaling["orgnummer"].asText()
                    ArbeidsgiverUtbetalingsperiode(orgnummer, fom, tom, grad, inntekt)
                }
                "9" -> Friperiode(fom, tom)
                "" -> UkjentInfotrygdperiode(fom, tom)
                else -> null
            }
        }

    private val feriepengehistorikk = packet["@løsning.${SykepengehistorikkForFeriepenger.name}.feriepengehistorikk"]
        .map { feriepenge ->
            Feriepenger(
                orgnummer = feriepenge["orgnummer"].asText(),
                beløp = feriepenge["beløp"].asInt(),
                fom = feriepenge["fom"].asLocalDate(),
                tom = feriepenge["tom"].asLocalDate()
            )
        }

    private val opptjeningsår = packet["${SykepengehistorikkForFeriepenger.name}.historikkFom"]
        .asLocalDate()
        .let(Year::from)

    private fun erGyldigPeriode(node: JsonNode): Boolean {
        val fom = node["fom"].asOptionalLocalDate()
        val tom = node["tom"].asOptionalLocalDate()
        return fom != null && tom != null && fom <= tom
    }

    internal fun utbetalingshistorikkForFeriepenger(aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = skalBeregnesManuelt,
            aktivitetslogg = aktivitetslogg
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingshistorikkForFeriepenger())
    }
}
