package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDate
import java.util.*

abstract class SøknadHendelse protected constructor(
    hendelseId: UUID,
    hendelsetype: Hendelsetype,
    protected val søknad: JsonNode
) : SykdomstidslinjeHendelse(hendelseId, hendelsetype) {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadHendelse::class.java)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromSøknad(json: String): SøknadHendelse? {
            return try {
                objectMapper.readTree(json).let { jsonNode ->
                    when (val type = jsonNode["status"].textValue()) {
                        in listOf("NY", "FREMTIDIG") -> NySøknad(jsonNode)
                        "SENDT" -> SendtSøknad(jsonNode)
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

    private val aktørId = søknad["aktorId"].asText()!!
    private val fnr = søknad["fnr"].asText()!!

    protected val sykeperioder
        get() = søknad["soknadsperioder"]?.map {
            Sykeperiode(
                it
            )
        } ?: emptyList()

    private val arbeidsgiver: Arbeidsgiver
        get() = søknad["arbeidsgiver"].let {
            Arbeidsgiver(
                it
            )
        }

    override fun aktørId() = aktørId

    override fun fødselsnummer(): String = fnr

    override fun organisasjonsnummer(): String = arbeidsgiver.orgnummer

    override fun kanBehandles(): Boolean {
        return søknad.hasNonNull("fnr")
            && søknad["arbeidsgiver"]?.hasNonNull("orgnummer") == true
    }

    override fun toJsonNode(): JsonNode {
        return objectMapper.readTree(toJson())
    }

    override fun toJson(): String = objectMapper.writeValueAsString(
        mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype(),
            "søknad" to søknad
        )
    )

    protected class Arbeidsgiver(val jsonNode: JsonNode) {
        val orgnummer: String get() = jsonNode["orgnummer"].textValue()
    }

    protected class Sykeperiode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val sykmeldingsgrad: Int = jsonNode["sykmeldingsgrad"].intValue()
        val faktiskGrad: Int? = jsonNode["faktiskGrad"]?.let {
            if (!it.isNull) it.intValue() else null
        }
    }
}
