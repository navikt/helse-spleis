package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDateTime
import java.util.*

class ModelManuellSaksbehandling(
    private val hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val utbetalingGodkjent: Boolean,
    private val rapportertdato: LocalDateTime,
    private val originalJson: String) : ArbeidstakerHendelse(hendelseId, Hendelsestype.ManuellSaksbehandling), VedtaksperiodeHendelse {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun lagBehov(
            vedtaksperiodeId: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String
        ): Behov {
            return Behov.nyttBehov(
                hendelsestype = Hendelsestype.ManuellSaksbehandling,
                behov = listOf(Behovstype.GodkjenningFraSaksbehandler),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = emptyMap()
            )
        }

        fun fromJson(json: String): ModelManuellSaksbehandling {
            return objectMapper.readTree(json).let {
                ModelManuellSaksbehandling(
                    hendelseId = UUID.fromString(it["hendelseId"].textValue()),
                    aktørId = it["ytelser"]["aktørId"].asText(),
                    fødselsnummer = it["ytelser"]["fødselsnummer"].asText(),
                    organisasjonsnummer = it["ytelser"]["organisasjonsnummer"].asText(),
                    vedtaksperiodeId = it["ytelser"]["vedtaksperiodeId"].asText(),
                    saksbehandler = it["ytelser"]["saksbehandlerIdent"].asText(),
                    utbetalingGodkjent = it["ytelser"]["@løsning.${Behovstype.GodkjenningFraSaksbehandler.name}"]["godkjent"].asBoolean(),
                    rapportertdato = it["ytelser"]["@besvart"].asLocalDateTime(),
                    originalJson = objectMapper.writeValueAsString(it["ytelser"])
                )
            }
        }

        private fun JsonNode.asLocalDateTime() =
            asText().let { LocalDateTime.parse(it) }
    }

    override fun toJson() = objectMapper.writeValueAsString(objectMapper.convertValue<ObjectNode>(mapOf(
        "hendelseId" to hendelseId(),
        "type" to hendelsetype()
    )).putRawValue("ytelser", RawValue(originalJson)))

    internal fun saksbehandler() = saksbehandler
    internal fun utbetalingGodkjent() = utbetalingGodkjent

    override fun rapportertdato() = rapportertdato
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
}
