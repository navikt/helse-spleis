package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

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
        val inntekt = utbetaling["dagsats"].asInt().daglig
        val grad = utbetaling["utbetalingsGrad"].asInt().prosent
        val orgnummer = utbetaling["orgnummer"].asText()
        if (fom == null || tom == null || fom > tom) Infotrygdperiode.Ugyldig(fom, tom)
        else when (utbetaling["typeKode"].asText()) {
            "0" -> Infotrygdperiode.Utbetaling(fom, tom, inntekt, grad, fødselsnummer)
            "1" -> Infotrygdperiode.ReduksjonMedlem(fom, tom, inntekt, grad, fødselsnummer)
            "2", "3" -> Infotrygdperiode.Etterbetaling(fom, tom)
            "4" -> Infotrygdperiode.KontertRegnskap(fom, tom)
            "5" -> Infotrygdperiode.RefusjonTilArbeidsgiver(fom, tom, inntekt, grad, orgnummer)
            "6" -> Infotrygdperiode.ReduksjonArbeidsgiverRefusjon(fom, tom, inntekt, grad, orgnummer)
            "7" -> Infotrygdperiode.Tilbakeført(fom, tom)
            "8" -> Infotrygdperiode.Konvertert(fom, tom)
            "9" -> Infotrygdperiode.Ferie(fom, tom)
            "O" -> Infotrygdperiode.Opphold(fom, tom)
            "S" -> Infotrygdperiode.Sanksjon(fom, tom)
            "" -> Infotrygdperiode.Ukjent(fom, tom)
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
            meldingsreferanseId = id,
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
