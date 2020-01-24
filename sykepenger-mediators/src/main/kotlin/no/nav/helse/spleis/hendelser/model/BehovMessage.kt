package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelSykepengehistorikk.Inntektsopplysning
import no.nav.helse.hendelser.ModelSykepengehistorikk.Periode.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import java.util.*

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(originalMessage: String, aktivitetslogger: Aktivitetslogger) :
    JsonMessage(originalMessage, aktivitetslogger) {
    init {
        requiredKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "hendelse", "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requiredValue("@final", true)
    }
}

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
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

    internal fun asModelYtelser(): ModelYtelser {
        val foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = this["@løsning.Foreldrepenger.Foreldrepengeytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            svangerskapsytelse = this["@løsning.Foreldrepenger.Svangerskapsytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            aktivitetslogger = aktivitetslogger
        )

        val sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("utbetalteSykeperioder")
            }.map { utbetaling ->
                val fom = utbetaling["fom"].asLocalDate()
                val tom = utbetaling["fom"].asLocalDate()
                val typekode = utbetaling["typeKode"].asText()
                val dagsats = utbetaling["dagsats"].asInt()
                when(typekode) {
                    "1" -> { ReduksjonMedlem(fom, tom, dagsats) }
                    in listOf("2", "3") -> { Etterbetaling(fom, tom, dagsats) }
                    "4" -> { KontertRegnskap(fom, tom, dagsats) }
                    "5" -> { RefusjonTilArbeidsgiver(fom, tom, dagsats) }
                    "6" -> { ReduksjonArbRef(fom, tom, dagsats) }
                    "7" -> { Tilbakeført(fom, tom, dagsats) }
                    "8" -> { Konvertert(fom, tom, dagsats) }
                    "9" -> { Ferie(fom, tom, dagsats) }
                    "O" -> { Opphold(fom, tom, dagsats) }
                    "S" -> { Sanksjon(fom, tom, dagsats) }
                    "" -> { Ukjent(fom, tom, dagsats) }
                    else -> aktivitetslogger.severe("Fikk en ukjent typekode:$typekode")
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
            aktivitetslogger = aktivitetslogger
        )

        return ModelYtelser(
            hendelseId = UUID.randomUUID(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            sykepengehistorikk = sykepengehistorikk,
            foreldrepenger = foreldrepenger,
            rapportertdato = this["@besvart"].asLocalDateTime(),
            originalJson = this.toJson()
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): YtelserMessage {
            return YtelserMessage(message, problems)
        }
    }
}

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValues("@behov", Behovstype.Inntektsberegning, Behovstype.EgenAnsatt)
        requiredKey("@løsning.${Behovstype.Inntektsberegning.name}")
        requiredKey("@løsning.${Behovstype.EgenAnsatt.name}")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelVilkårsgrunnlag(): ModelVilkårsgrunnlag {
        return ModelVilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            orgnummer = this["organisasjonsnummer"].asText(),
            rapportertDato = this["@besvart"].asLocalDateTime(),
            inntektsmåneder = this["@løsning.${Behovstype.Inntektsberegning.name}"].map {
                ModelVilkårsgrunnlag.Måned(
                    årMåned = it["årMåned"].asYearMonth(),
                    inntektsliste = it["inntektsliste"].map { ModelVilkårsgrunnlag.Inntekt(it["beløp"].asDouble()) }
                )
            },
            erEgenAnsatt = this["@løsning.${Behovstype.EgenAnsatt.name}"].asBoolean(),
            aktivitetslogger = aktivitetslogger,
            originalJson = this.toJson()
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): VilkårsgrunnlagMessage {
            return VilkårsgrunnlagMessage(message, problems)
        }
    }
}

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValues("@behov", Behovstype.GodkjenningFraSaksbehandler)
        requiredKey("@løsning.${Behovstype.GodkjenningFraSaksbehandler.name}.godkjent")
        requiredKey("saksbehandlerIdent")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelManuellSaksbehandling() =
        ModelManuellSaksbehandling(
            hendelseId = UUID.randomUUID(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            saksbehandler = this["saksbehandlerIdent"].asText(),
            utbetalingGodkjent = this["@løsning.${Behovstype.GodkjenningFraSaksbehandler.name}.godkjent"].asBoolean(),
            rapportertdato = this["@besvart"].asLocalDateTime()
        )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): ManuellSaksbehandlingMessage {
            return ManuellSaksbehandlingMessage(message, problems)
        }
    }
}
