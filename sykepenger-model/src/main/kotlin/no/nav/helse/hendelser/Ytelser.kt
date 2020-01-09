package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
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
import java.time.LocalDate
import java.util.*

class Ytelser private constructor(hendelseId: UUID, private val behov: Behov) :
    ArbeidstakerHendelse(hendelseId, Hendelsetype.Ytelser),
    VedtaksperiodeHendelse {

    private constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): Ytelser? {
            return try {
                val behov = Behov.fromJson(json)
                require(behov.erLøst())
                require(Hendelsetype.Ytelser == behov.hendelsetype())

                Ytelser(behov)
            } catch (err: Exception) {
                null
            }
        }
    }

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

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
                behov = listOf(Behovtype.Sykepengehistorikk, Behovtype.Foreldrepenger),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = params
            )
        }

        fun fromJson(json: String): Ytelser {
            return objectMapper.readTree(json).let {
                Ytelser(UUID.fromString(it["hendelseId"].textValue()), Behov.fromJson(it["ytelser"].toString()))
            }
        }
    }

    internal fun foreldrepenger(): Foreldrepenger {
        val løsning = behov.løsning() as Map<*, *>
        val foreldrepengerJsonNode = objectMapper.convertValue(løsning["Foreldrepenger"], JsonNode::class.java)
        return Foreldrepenger(foreldrepengerJsonNode)
    }

    internal fun sykepengehistorikk(): Sykepengehistorikk {
        val løsning = behov.løsning() as Map<*, *>
        val sykepengehistorikkløsninger = løsning["Sykepengehistorikk"] as List<*>

        return Sykepengehistorikk(
            objectMapper.convertValue<JsonNode>(
                sykepengehistorikkløsninger
            )
        )
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
            .putRawValue("ytelser", RawValue(behov.toJson()))
            .toString()
    }

    internal class Foreldrepenger(jsonNode: JsonNode) {
        fun overlapperMedSyketilfelle(syketilfelleFom: LocalDate, syketilfelleTom: LocalDate): Boolean {
            val syketilfelleRange = syketilfelleFom.rangeTo(syketilfelleTom)
            if (Foreldrepengeytelse == null && Svangerskapsytelse == null) {
                return false
            }

            return listOfNotNull(Foreldrepengeytelse, Svangerskapsytelse).any { ytelse ->
                ytelse.overlapperMed(syketilfelleRange)
            }
        }

        private val Foreldrepengeytelse: Ytelse? = objectMapper.convertValue(jsonNode["Foreldrepengeytelse"], Ytelse::class.java)
        private val Svangerskapsytelse: Ytelse? = objectMapper.convertValue(jsonNode["Svangerskapsytelse"], Ytelse::class.java)

        private class Ytelse(
            internal val fom: LocalDate,
            internal val tom: LocalDate
        ) {
            fun overlapperMed(range: ClosedRange<LocalDate>) =
                range.contains(fom) || range.contains(tom) || (tom > range.start && fom < range.endInclusive)
        }
    }

    // Embedded document - json er output fra Spole
    internal data class Sykepengehistorikk(private val jsonNode: JsonNode) {
        private val perioder get() = jsonNode.map { Periode(it) }

        fun sisteFraværsdag() =
            perioder.maxBy { it.tom }?.tom

        internal data class Periode(val jsonNode: JsonNode) {
            val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
            val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
            val grad = jsonNode["grad"].textValue()
        }
    }
}
