package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkMessage(packet: MessageDelegate) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()

    private val utbetalinger = packet["@løsning.${Sykepengehistorikk.name}"].flatMap {
        it.path("utbetalteSykeperioder")
    }.mapNotNull { utbetaling ->
        val fom = utbetaling["fom"].asOptionalLocalDate()
        val tom = utbetaling["tom"].asOptionalLocalDate()
        val dagsats = utbetaling["dagsats"].asInt()
        val grad = utbetaling["utbetalingsGrad"].asInt()
        val orgnummer = utbetaling["orgnummer"].asText()
        if (fom == null || tom == null || fom > tom) Periode.Ugyldig(fom, tom)
        else when (utbetaling["typeKode"].asText()) {
            "0" -> Periode.Utbetaling(fom, tom, dagsats, grad, orgnummer)
            "1" -> Periode.ReduksjonMedlem(fom, tom, dagsats, grad, orgnummer)
            "2", "3" -> Periode.Etterbetaling(fom, tom)
            "4" -> Periode.KontertRegnskap(fom, tom)
            "5" -> Periode.RefusjonTilArbeidsgiver(fom, tom, dagsats, grad, orgnummer)
            "6" -> Periode.ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats, grad, orgnummer)
            "7" -> Periode.Tilbakeført(fom, tom)
            "8" -> Periode.Konvertert(fom, tom)
            "9" -> Periode.Ferie(fom, tom)
            "O" -> Periode.Opphold(fom, tom)
            "S" -> Periode.Sanksjon(fom, tom)
            "" -> Periode.Ukjent(fom, tom)
            else -> null // filtered away in river validation
        }
    }

    private val inntektshistorikk = packet["@løsning.${Sykepengehistorikk.name}"].flatMap {
        it.path("inntektsopplysninger")
    }.map { opplysning ->
        Utbetalingshistorikk.Inntektsopplysning(
            sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
            inntektPerMåned = opplysning["inntekt"].asDouble().månedlig,
            orgnummer = opplysning["orgnummer"].asText(),
            refusjonTilArbeidsgiver = opplysning["refusjonTilArbeidsgiver"].asBoolean(),
            refusjonTom = opplysning["refusjonTom"].asOptionalLocalDate()
        )
    }

    internal fun utbetalingshistorikk(aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) =
        Utbetalingshistorikk(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            aktivitetslogg = aktivitetslogg
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingshistorikk())
    }
}
