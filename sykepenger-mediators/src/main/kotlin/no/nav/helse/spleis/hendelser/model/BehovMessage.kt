package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import java.util.*

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(
    originalMessage: String,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) :
    JsonMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requiredValue("@final", true)
    }

    override val id: UUID get() = UUID.fromString(this["@id"].asText())
}

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(
    originalMessage: String,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    BehovMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredValues("@behov", Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        requiredKey("@løsning.${Behovstype.Foreldrepenger.name}")
        requiredKey("@løsning.${Behovstype.Sykepengehistorikk.name}")
        interestedIn("@løsning.${Behovstype.Foreldrepenger.name}.Foreldrepengeytelse")
        interestedIn("@løsning.${Behovstype.Foreldrepenger.name}.Svangerskapsytelse")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asYtelser(): Ytelser {
        val foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = this["@løsning.Foreldrepenger.Foreldrepengeytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            svangerskapsytelse = this["@løsning.Foreldrepenger.Svangerskapsytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )

        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("utbetalteSykeperioder")
            }.map { utbetaling ->
                val fom = utbetaling["fom"].asLocalDate()
                val tom = utbetaling["tom"].asLocalDate()
                val typekode = utbetaling["typeKode"].asText()
                val dagsats = utbetaling["dagsats"].asInt()
                when (typekode) {
                    "0" -> {
                        Utbetaling(fom, tom, dagsats)
                    }
                    "1" -> {
                        ReduksjonMedlem(fom, tom, dagsats)
                    }
                    in listOf("2", "3") -> {
                        Etterbetaling(fom, tom, dagsats)
                    }
                    "4" -> {
                        KontertRegnskap(fom, tom, dagsats)
                    }
                    "5" -> {
                        RefusjonTilArbeidsgiver(fom, tom, dagsats)
                    }
                    "6" -> {
                        ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats)
                    }
                    "7" -> {
                        Tilbakeført(fom, tom, dagsats)
                    }
                    "8" -> {
                        Konvertert(fom, tom, dagsats)
                    }
                    "9" -> {
                        Ferie(fom, tom, dagsats)
                    }
                    "O" -> {
                        Opphold(fom, tom, dagsats)
                    }
                    "S" -> {
                        Sanksjon(fom, tom, dagsats)
                    }
                    "" -> {
                        Ukjent(fom, tom, dagsats)
                    }
                    else -> aktivitetslogger.severeOld("Fikk en ukjent typekode:$typekode")
                }
            },
            inntektshistorikk = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("inntektsopplysninger")
            }.map { opplysning ->
                Inntektsopplysning(
                    sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
                    inntektPerMåned = opplysning["inntekt"].asInt(),
                    orgnummer = opplysning["orgnummer"].asText()
                )
            },
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )

        return Ytelser(
            hendelseId = this.id,
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = foreldrepermisjon,
            rapportertdato = this["@besvart"].asLocalDateTime(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(
            message: String,
            problems: Aktivitetslogger,
            aktivitetslogg: Aktivitetslogg
        ): YtelserMessage {
            return YtelserMessage(message, problems, aktivitetslogg)
        }
    }
}

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(
    originalMessage: String,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    BehovMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredValues("@behov", Behovstype.Inntektsberegning, Behovstype.EgenAnsatt, Behovstype.Opptjening)
        requiredKey("@løsning.${Behovstype.Inntektsberegning.name}")
        requiredKey("@løsning.${Behovstype.EgenAnsatt.name}")
        requiredKey("@løsning.${Behovstype.Opptjening.name}")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asVilkårsgrunnlag(): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            hendelseId = this.id,
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            orgnummer = this["organisasjonsnummer"].asText(),
            rapportertDato = this["@besvart"].asLocalDateTime(),
            inntektsmåneder = this["@løsning.${Behovstype.Inntektsberegning.name}"].map {
                Vilkårsgrunnlag.Måned(
                    årMåned = it["årMåned"].asYearMonth(),
                    inntektsliste = it["inntektsliste"].map { it["beløp"].asDouble() }
                )
            },
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(this["@løsning.${Behovstype.Opptjening.name}"]
                .map {
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer = it["orgnummer"].asText(),
                        fom = it["ansattSiden"].asLocalDate(),
                        tom = it["ansattTil"].asOptionalLocalDate()
                    )
                }
            ),
            erEgenAnsatt = this["@løsning.${Behovstype.EgenAnsatt.name}"].asBoolean(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(
            message: String,
            problems: Aktivitetslogger,
            aktivitetslogg: Aktivitetslogg
        ): VilkårsgrunnlagMessage {
            return VilkårsgrunnlagMessage(message, problems, aktivitetslogg)
        }
    }
}

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(
    originalMessage: String,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    BehovMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredValues("@behov", Behovstype.Godkjenning)
        requiredKey("@løsning.${Behovstype.Godkjenning.name}.godkjent")
        requiredKey("saksbehandlerIdent")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asManuellSaksbehandling() =
        ManuellSaksbehandling(
            hendelseId = this.id,
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            saksbehandler = this["saksbehandlerIdent"].asText(),
            utbetalingGodkjent = this["@løsning.${Behovstype.Godkjenning.name}.godkjent"].asBoolean(),
            rapportertdato = this["@besvart"].asLocalDateTime(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg): ManuellSaksbehandlingMessage {
            return ManuellSaksbehandlingMessage(message, problems, aktivitetslogg)
        }
    }
}
