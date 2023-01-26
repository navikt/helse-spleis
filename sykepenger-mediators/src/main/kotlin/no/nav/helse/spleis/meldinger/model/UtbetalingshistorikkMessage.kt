package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.UgyldigPeriode
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

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

    private val ugyldigePerioder = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { it.path("utbetalteSykeperioder") }
        .filterNot(::erGyldigPeriode)
        .map { utbetaling ->
            val fom = utbetaling["fom"].asOptionalLocalDate()
            val tom = utbetaling["tom"].asOptionalLocalDate()
            val utbetalingsGrad = utbetaling["utbetalingsGrad"].asInt()
            UgyldigPeriode(fom, tom, utbetalingsGrad)
        }

    private val arbeidskategorikoder: Map<String, LocalDate> = packet["@løsning.${Sykepengehistorikk.name}"]
        .flatMap { element ->
            element.path("utbetalteSykeperioder").mapNotNull { utbetaling ->
                utbetaling["fom"].asOptionalLocalDate()?.let {
                    element.path("arbeidsKategoriKode").asText() to it
                }
            }
        }.sortedBy { (_, dato) -> dato }.toMap()

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
        return if (erUtbetalingsperiode(node)) {
            harGyldigUtbetalingsgrad(node) && harGyldigTidsintervall(node)
        } else harGyldigTidsintervall(node)
    }

    private fun harGyldigUtbetalingsgrad(node: JsonNode): Boolean {
        val utbetalingsGrad = node["utbetalingsGrad"].asInt()
        return utbetalingsGrad > 0
    }

    private fun erUtbetalingsperiode(node: JsonNode) = node["typeKode"].asText() in listOf("0", "1", "5", "6")

    private fun harGyldigTidsintervall(node: JsonNode): Boolean {
        val fom = node["fom"].asOptionalLocalDate()
        val tom = node["tom"].asOptionalLocalDate()
        return fom != null && tom != null && tom >= fom
    }

    internal fun infotrygdhistorikk(meldingsreferanseId: UUID) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = meldingsreferanseId,
            perioder = utbetalinger,
            inntekter = inntektshistorikk,
            arbeidskategorikoder = arbeidskategorikoder,
            ugyldigePerioder = ugyldigePerioder,
            harStatslønn = harStatslønn
        )

    private fun utbetalingshistorikk() =
        Utbetalingshistorikk(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            element = infotrygdhistorikk(id),
            aktivitetslogg = Aktivitetslogg()
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, utbetalingshistorikk(), context)
    }
}
