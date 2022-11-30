package no.nav.helse.spleis

import java.time.ZonedDateTime
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.MaskinellJurist.KontekstType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class SubsumsjonMediator(
    private val jurist: MaskinellJurist,
    private val fødselsnummer: String,
    private val message: HendelseMessage,
    private val versjonAvKode: String
) {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(PersonMediator::class.java)
    }

    fun finalize(context: MessageContext) {
        val events = jurist.events()
        if (events.isEmpty()) return
        logg.info("som følge av hendelse id=${message.id} sendes ${events.size} subsumsjonsmeldinger på rapid")
        events
            .map { subsumsjonMelding(fødselsnummer = fødselsnummer, event = it) }
            .forEach {
                context.publish(fødselsnummer, it.toJson().also { message ->
                    sikkerLogg.info("som følge av hendelse id=${this.message.id} sender subsumsjon: $message")
                })
            }
    }

    private fun subsumsjonMelding(fødselsnummer: String, event: MaskinellJurist.SubsumsjonEvent): JsonMessage {
        return JsonMessage.newMessage("subsumsjon", mapOf(
            "@id" to event.id,
            "subsumsjon" to mutableMapOf(
                "id" to event.id,
                "eventName" to "subsumsjon",
                "tidsstempel" to ZonedDateTime.now(),
                "versjon" to "1.0.0",
                "kilde" to "spleis",
                "versjonAvKode" to versjonAvKode,
                "fodselsnummer" to fødselsnummer,
                "sporing" to event.sporing.map { it.key.tilEkstern() to it.value }.toMap(),
                "lovverk" to event.lovverk,
                "lovverksversjon" to event.ikrafttredelse,
                "paragraf" to event.paragraf,
                "input" to event.input,
                "output" to event.output,
                "utfall" to event.utfall
            ).apply {
                compute("ledd") { _, _ -> event.ledd }
                compute("punktum") { _, _ -> event.punktum }
                compute("bokstav") { _, _ -> event.bokstav }
            }
        ))
    }

    private fun KontekstType.tilEkstern() = when(this) {
        KontekstType.Fødselsnummer -> "fodselsnummer"
        KontekstType.Organisasjonsnummer -> "organisasjonsnummer"
        KontekstType.Vedtaksperiode -> "vedtaksperiode"
        KontekstType.Sykmelding -> "sykmelding"
        KontekstType.Søknad -> "soknad"
        KontekstType.Inntektsmelding -> "inntektsmelding"
        KontekstType.OverstyrTidslinje -> "overstyrtidslinje"
        KontekstType.OverstyrArbeidsgiveropplysninger -> "overstyrarbeidsgiveropplysninger"
        KontekstType.OverstyrInntekt -> "overstyrinntekt"
        KontekstType.OverstyrRefusjon -> "overstyrrefusjon"
        KontekstType.OverstyrArbeidsforhold -> "overstyrarbeidsforhold"
    }
}
