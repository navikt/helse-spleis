package no.nav.helse.søknad.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Event
import no.nav.helse.person.domain.Sykdomshendelse
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.KildeHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

const val SØKNAD_SENDT = "SENDT"
const val SØKNAD_NY = "NY"
const val SØKNAD_FREMTIDIG = "FREMTIDIG"

@JsonSerialize(using = SykepengesøknadSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
data class Sykepengesøknad(val jsonNode: JsonNode) : Event, Sykdomshendelse {

    val id = jsonNode["id"].asText()!!
    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!
    val status = jsonNode["status"].asText()!!
    val aktørId = jsonNode["aktorId"].asText()!!
    val fom get() = jsonNode["fom"].asText().let { LocalDate.parse(it) }
    val tom get() = jsonNode["tom"].asText().let { LocalDate.parse(it) }
    val opprettet get() = jsonNode["opprettet"].asText().let { LocalDateTime.parse(it) }
    val egenmeldinger get() = jsonNode["egenmeldinger"]?.map { Periode(it) } ?: emptyList()
    val sykeperioder get() = jsonNode["soknadsperioder"]?.map { Periode(it) } ?: emptyList()
    val fraværsperioder get() = jsonNode["fravar"]?.map { FraværsPeriode(it) } ?: emptyList()
    val arbeidGjenopptatt get() = jsonNode["arbeidGjenopptatt"]?.safelyUnwrapDate()
    val korrigerer get() = jsonNode["korrigerer"]?.asText()

    override fun aktørId() = aktørId
    override fun organisasjonsnummer(): String = jsonNode["arbeidsgiver"].get("orgnummer").asText()
    override fun rapportertdato(): LocalDateTime = opprettet
    override fun compareTo(other: KildeHendelse): Int = opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = sykeperioder.map {
            Sykdomstidslinje.sykedager(it.fom, it.tom, this)
        }
    private val egenmeldingsTidslinje
        get(): List<Sykdomstidslinje> = egenmeldinger.map {
            Sykdomstidslinje.sykedager(it.fom, it.tom, this)
        }
    private val ferieTidslinje
        get(): List<Sykdomstidslinje> = fraværsperioder.filter { it.type == Fraværstype.FERIE }.map {
            Sykdomstidslinje.ferie(it.fom, it.tom, this)
        }
    private val arbeidGjenopptattTidslinje
        get(): List<Sykdomstidslinje> = arbeidGjenopptatt?.let {
            listOf(Sykdomstidslinje.ikkeSykedager(it, tom, this))
        } ?: emptyList()

    override fun sykdomstidslinje() = (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje)
        .reduce { resultatTidslinje, delTidslinje -> resultatTidslinje + delTidslinje }

    override fun eventType(): Event.Type {
        return when (status) {
            SØKNAD_SENDT -> Event.Type.SendtSykepengesøknad
            in arrayOf(SØKNAD_NY, SØKNAD_FREMTIDIG) -> Event.Type.NySykepengesøknad
            else -> throw IllegalStateException("Kunne ikke mappe søknadstype $status til en event")
        }
    }
}

data class Periode(val jsonNode: JsonNode) {
    val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
    val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
}

data class FraværsPeriode(val jsonNode: JsonNode) {
    val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
    val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
    val type: Fraværstype = enumValueOf(jsonNode["type"].textValue())
}

enum class Fraværstype {
    FERIE,
    PERMISJON,
    UTLANDSOPPHOLD,
    UTDANNING_FULLTID,
    UTDANNING_DELTID
}

class SykepengesøknadSerializer : StdSerializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    override fun serialize(søknad: Sykepengesøknad?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(søknad?.jsonNode)
    }
}

class SykepengesøknadDeserializer : StdDeserializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Sykepengesøknad(objectMapper.readTree(parser))

}
