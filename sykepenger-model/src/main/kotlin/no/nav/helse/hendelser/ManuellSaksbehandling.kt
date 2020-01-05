package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovtype
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.util.*

class ManuellSaksbehandling private constructor(hendelseId: UUID, private val behov: Behov) :
    ArbeidstakerHendelse(hendelseId, Hendelsetype.ManuellSaksbehandling),
    VedtaksperiodeHendelse {

    private constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): ManuellSaksbehandling? {
            return try {
                val behov = Behov.fromJson(json)
                require(behov.erLøst())
                require(Hendelsetype.ManuellSaksbehandling == behov.hendelsetype())

                ManuellSaksbehandling(behov)
            } catch (err: Exception) {
                null
            }
        }
    }

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
                hendelsetype = Hendelsetype.ManuellSaksbehandling,
                behov = listOf(Behovtype.GodkjenningFraSaksbehandler),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = emptyMap()
            )
        }

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

    override fun rapportertdato() = requireNotNull(behov.besvart())

    override fun toJson(): String {
        return objectMapper.convertValue<ObjectNode>(mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype()
        ))
            .putRawValue("ytelser", RawValue(behov.toJson()))
            .toString()
    }
}
