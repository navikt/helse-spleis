package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkMessage(packet: JsonMessage) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val besvart = packet["@besvart"].asLocalDateTime()

    private val harStatslønn = packet["@løsning.${Sykepengehistorikk.name}"].any { it["statslønn"].asBoolean() }

    private val utbetalinger = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { it.path("utbetalteSykeperioder") }
        .filter(::erGyldigPeriode)
        .mapNotNull { utbetaling ->
            val fom = utbetaling["fom"].asLocalDate()
            val tom = utbetaling["tom"].asLocalDate()
            val periode = Periode(fom, tom)
            when (utbetaling["typeKode"].asText()) {
                "0", "1", "5", "6" -> {
                    val grad = utbetaling["utbetalingsGrad"].asInt().prosent
                    // inntektbeløpet i Infotrygd-utbetalingene er gradert; justerer derfor "opp igjen"
                    val inntekt = utbetaling["dagsats"].asInt().daglig(grad)
                    val orgnummer = utbetaling["orgnummer"].asText()
                    Utbetalingsperiode(orgnummer, periode, grad, inntekt)
                }
                "9" -> Friperiode(periode)
                "" -> UkjentInfotrygdperiode(periode)
                else -> null
            }
        }

    private val ugyldigePerioder = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { it.path("utbetalteSykeperioder") }
        .filterNot(::erGyldigPeriode)
        .mapNotNull { utbetaling ->
            val fom = utbetaling["fom"].asOptionalLocalDate()
            val tom = utbetaling["tom"].asOptionalLocalDate()
            if (fom == null || tom == null || fom > tom) fom to tom else null
        }

    private val arbeidskategorikoder: Map<String, LocalDate> = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { element ->
            element.path("utbetalteSykeperioder").mapNotNull { utbetaling ->
                utbetaling["fom"].asOptionalLocalDate()?.let {
                    element.path("arbeidsKategoriKode").asText() to it
                }
            }
        }.toMap()

    private val inntektshistorikk = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { it.path("inntektsopplysninger") }
        .map { opplysning ->
            Inntektsopplysning(
                sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
                inntekt = opplysning["inntekt"].asDouble().månedlig,
                orgnummer = opplysning["orgnummer"].asText(),
                refusjonTilArbeidsgiver = opplysning["refusjonTilArbeidsgiver"].asBoolean(),
                refusjonTom = opplysning["refusjonTom"].asOptionalLocalDate()
            )
        }

    private fun erGyldigPeriode(node: JsonNode): Boolean {
        val fom = node["fom"].asOptionalLocalDate()
        val tom = node["tom"].asOptionalLocalDate()
        return fom != null && tom != null && tom >= fom
    }

    internal fun utbetalingshistorikk(aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) =
        Utbetalingshistorikk(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidskategorikoder = arbeidskategorikoder,
            harStatslønn = harStatslønn,
            perioder = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            ugyldigePerioder = ugyldigePerioder,
            aktivitetslogg = aktivitetslogg,
            besvart = besvart
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingshistorikk())
    }
}
