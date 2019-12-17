package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

abstract class SøknadHendelse protected constructor(
    hendelseId: String,
    private val hendelsetype: SykdomshendelseType,
    protected val søknad: JsonNode
): ArbeidstakerHendelse, SykdomstidslinjeHendelse(hendelseId) {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadHendelse::class.java)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromSøknad(json: String): SøknadHendelse? {
            return try {
                objectMapper.readTree(json).let { jsonNode ->
                    when (val type = jsonNode["status"].textValue()) {
                        "NY" -> NySøknadHendelse(jsonNode)
                        "FREMTIDIG" -> NySøknadHendelse(jsonNode)
                        "SENDT" -> SendtSøknadHendelse(jsonNode)
                        else -> null.also {
                            log.info("kunne ikke lese sykepengesøknad, ukjent type: $type")
                        }
                    }
                }
            } catch (err: IOException) {
                log.info("kunne ikke lese sykepengesøknad som json: ${err.message}", err)
                null
            }
        }
    }

    val type = søknad["soknadstype"]?.textValue() ?: søknad["type"].textValue()
    val id = søknad["id"].asText()!!
    val sykmeldingId = søknad["sykmeldingId"].asText()!!
    val status = søknad["status"].asText()!!
    val aktørId = søknad["aktorId"].asText()!!
    val fnr = søknad["fnr"].asText()!!
    val fom get() = søknad["fom"].asText().let { LocalDate.parse(it) }
    val tom get() = søknad["tom"].asText().let { LocalDate.parse(it) }
    val opprettet get() = søknad["opprettetDato"]?.takeUnless { it.isNull }?.let { LocalDate.parse(it.textValue()).atStartOfDay() } ?: LocalDateTime.parse(søknad["opprettet"].asText())
    val egenmeldinger get() = søknad["egenmeldinger"]?.map { Periode(it) } ?: emptyList()
    val sykeperioder get() = søknad["soknadsperioder"]?.map { Sykeperiode(it) } ?: emptyList()
    val sendtNav = søknad["sendtNav"]?.takeUnless { it.isNull }?.let { LocalDateTime.parse(it.asText()) }
    val fraværsperioder
        get() = søknad["fravar"]?.filterNot {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                Fraværstype.UTDANNING_FULLTID,
                Fraværstype.UTDANNING_DELTID
            )
        }?.map { FraværsPeriode(it) } ?: emptyList()

    val utdanningsperioder
        get() = søknad["fravar"]?.filter {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                Fraværstype.UTDANNING_FULLTID,
                Fraværstype.UTDANNING_DELTID
            )
        }?.map { Utdanningsfraværsperiode(it) } ?: emptyList()

    val arbeidGjenopptatt get() = søknad["arbeidGjenopptatt"]?.safelyUnwrapDate()
    val korrigerer get() = søknad["korrigerer"]?.asText()

    val arbeidsgiver: Arbeidsgiver get() = søknad["arbeidsgiver"].let { Arbeidsgiver(it) }

    override fun aktørId() = aktørId

    override fun fødselsnummer(): String = fnr

    override fun organisasjonsnummer(): String = arbeidsgiver.orgnummer

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
        rapportertdato().compareTo(other.rapportertdato())

    override fun kanBehandles(): Boolean {
        return søknad.hasNonNull("fnr")
            && søknad["arbeidsgiver"]?.hasNonNull("orgnummer") == true
    }

    override fun toJsonNode(): JsonNode {
        return objectMapper.readTree(toJson())
    }

    override fun toJson(): String = objectMapper.writeValueAsString(mapOf(
        "hendelseId" to hendelseId(),
        "type" to hendelsetype.name,
        "søknad" to søknad
    ))

    data class Arbeidsgiver(val jsonNode: JsonNode) {
        val orgnummer: String get() = jsonNode["orgnummer"].textValue()
    }

    data class Periode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
    }

    data class Sykeperiode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val sykmeldingsgrad: Int = jsonNode["sykmeldingsgrad"].intValue()
        val faktiskGrad: Int? = jsonNode["faktiskGrad"]?.let {
            if (!it.isNull) it.intValue() else null
        }
    }

    data class FraværsPeriode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val type: Fraværstype = enumValueOf(jsonNode["type"].textValue())
    }

    data class Utdanningsfraværsperiode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val type: Fraværstype = enumValueOf(jsonNode["type"].textValue())
    }

    enum class Fraværstype {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD,
        UTDANNING_FULLTID,
        UTDANNING_DELTID
    }
}
