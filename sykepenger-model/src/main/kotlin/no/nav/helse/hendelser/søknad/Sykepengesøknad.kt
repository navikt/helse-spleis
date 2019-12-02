package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.serde.safelyUnwrapDate
import java.time.LocalDate
import java.time.LocalDateTime

@JsonSerialize(using = SykepengesøknadSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
data class Sykepengesøknad(private val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!
    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!
    val status = jsonNode["status"].asText()!!
    val aktørId = jsonNode["aktorId"].asText()!!
    val fnr = jsonNode["fnr"].asText()!!
    val fom get() = jsonNode["fom"].asText().let { LocalDate.parse(it) }
    val tom get() = jsonNode["tom"].asText().let { LocalDate.parse(it) }
    val opprettet get() = jsonNode["opprettet"].asText().let { LocalDateTime.parse(it) }
    val egenmeldinger get() = jsonNode["egenmeldinger"]?.map { Periode(it) } ?: emptyList()
    val sykeperioder get() = jsonNode["soknadsperioder"]?.map { Sykeperiode(it) } ?: emptyList()
    val sendtNav = jsonNode["sendtNav"]?.let { if (it.isNull) null else LocalDateTime.parse(it.asText()) }
    val fraværsperioder
        get() = jsonNode["fravar"]?.filterNot {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                    Fraværstype.UTDANNING_FULLTID,
                    Fraværstype.UTDANNING_DELTID
            )
        }?.map { FraværsPeriode(it) } ?: emptyList()

    val utdanningsperioder
        get() = jsonNode["fravar"]?.filter {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                    Fraværstype.UTDANNING_FULLTID,
                    Fraværstype.UTDANNING_DELTID
            )
        }?.map { Utdanningsfraværsperiode(it) } ?: emptyList()

    val arbeidGjenopptatt get() = jsonNode["arbeidGjenopptatt"]?.safelyUnwrapDate()
    val korrigerer get() = jsonNode["korrigerer"]?.asText()

    val arbeidsgiver: Arbeidsgiver get() = jsonNode["arbeidsgiver"].let { Arbeidsgiver(it) }

    fun kanBehandles(): Boolean {
        return jsonNode.hasNonNull("fnr")
            && jsonNode["arbeidsgiver"]?.hasNonNull("orgnummer") == true
    }

    fun toJson(): JsonNode = jsonNode

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
