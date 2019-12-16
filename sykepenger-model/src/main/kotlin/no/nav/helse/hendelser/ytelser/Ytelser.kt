package no.nav.helse.hendelser.ytelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovtype
import no.nav.helse.hendelser.Hendelsetype
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.time.LocalDate
import java.util.*

class Ytelser(private val behov: Behov) : ArbeidstakerHendelse, VedtaksperiodeHendelse {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun lagBehov(
            vedtaksperiodeId: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utgangspunktForBeregningAvYtelse: LocalDate
        ): Behov {
            val params = mutableMapOf(
                "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
            )

            return Behov.nyttBehov(
                hendelsetype = Hendelsetype.Ytelser,
                behov = listOf(Behovtype.Sykepengehistorikk, Behovtype.Foreldrepenger, Behovtype.Svangerskapspenger),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = params
            )
        }
    }

    internal fun foreldrepenger(): Foreldrepenger {
        TODO()
    }

    internal fun svangerskapspenger(): Svangerskapspenger {
        TODO()
    }

    internal fun sykepengehistorikk(): Sykepengehistorikk {
        val løsning = behov.løsning() as Map<*, *>
        val sykepengehistorikkløsninger = løsning["Sykepengehistorikk"] as Map<*, *>

        return Sykepengehistorikk(objectMapper.convertValue<JsonNode>(sykepengehistorikkløsninger))
    }

    override fun aktørId() = behov.aktørId()

    override fun fødselsnummer() = behov.fødselsnummer()

    override fun organisasjonsnummer() = behov.organisasjonsnummer()

    override fun vedtaksperiodeId() = behov.vedtaksperiodeId()

    internal class Foreldrepenger {}

    internal class Svangerskapspenger {}
}
