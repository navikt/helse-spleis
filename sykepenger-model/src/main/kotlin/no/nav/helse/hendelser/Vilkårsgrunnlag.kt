package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.util.UUID

class Vilkårsgrunnlag private constructor(hendelseId: UUID, private val behov: Behov) :
    ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag),
    VedtaksperiodeHendelse {

    private constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): Vilkårsgrunnlag? {
            return try {
                val behov = Behov.fromJson(json)
                if (!behov.erLøst() || Hendelsestype.Vilkårsgrunnlag != behov.hendelsetype())
                    return null
                Vilkårsgrunnlag(behov)
            } catch (err: Exception) {
                null
            }
        }
    }

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Vilkårsgrunnlag {
            return objectMapper.readTree(json).let {
                Vilkårsgrunnlag(UUID.fromString(it["hendelseId"].textValue()), Behov.fromJson(it["vilkårsgrunnlag"].toString()))
            }
        }
    }

    internal fun erEgenAnsatt(): Boolean {
        val løsning = behov.løsning() as Map<*, *>
        return løsning["EgenAnsatt"] as Boolean
    }

    override fun aktørId() = behov.aktørId()

    override fun fødselsnummer() = behov.fødselsnummer()

    override fun organisasjonsnummer() = behov.organisasjonsnummer()

    override fun vedtaksperiodeId() = behov.vedtaksperiodeId()

    override fun rapportertdato() = requireNotNull(behov.besvart())

    override fun toJson(): String {
        return objectMapper.convertValue<ObjectNode>(
            mapOf(
                "hendelseId" to hendelseId(),
                "type" to hendelsetype()
            )
        )
            .putRawValue("vilkårsgrunnlag", RawValue(behov.toJson()))
            .toString()
    }
}
