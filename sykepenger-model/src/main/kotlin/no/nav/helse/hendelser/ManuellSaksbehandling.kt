package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.util.*

class ManuellSaksbehandling private constructor(hendelseId: UUID, private val behov: Behov) :
    ArbeidstakerHendelse(hendelseId, Hendelsetype.ManuellSaksbehandling), VedtaksperiodeHendelse {

    constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ManuellSaksbehandling {
            return objectMapper.readTree(json).let {
                ManuellSaksbehandling(UUID.fromString(it["hendelseId"].textValue()), Behov.fromJson(it["ytelser"].toString()))
            }
        }
    }

    fun saksbehandler(): String = requireNotNull(behov["saksbehandlerIdent"])

    fun utbetalingGodkjent(): Boolean =
        (behov.løsning() as Map<*, *>?)?.get("godkjent") == true

    override fun vedtaksperiodeId(): String = behov.vedtaksperiodeId()

    override fun aktørId(): String = behov.aktørId()

    override fun fødselsnummer(): String = behov.fødselsnummer()

    override fun organisasjonsnummer(): String = behov.organisasjonsnummer()

    override fun opprettet() = requireNotNull(behov.besvart())

    override fun toJson(): String {
        return objectMapper.convertValue<ObjectNode>(mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype()
        ))
            .putRawValue("ytelser", RawValue(behov.toJson()))
            .toString()
    }
}
