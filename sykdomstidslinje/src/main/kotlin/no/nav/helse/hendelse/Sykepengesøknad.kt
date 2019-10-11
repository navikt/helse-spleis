package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

private const val SØKNAD_SENDT = "SENDT"
private const val SØKNAD_NY = "NY"
private const val SØKNAD_FREMTIDIG = "FREMTIDIG"

@JsonSerialize(using = SykdomsheldelseSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
abstract class Sykepengesøknad(private val jsonNode: JsonNode) : Sykdomshendelse {

    val id = jsonNode["id"].asText()!!
    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!
    val status = jsonNode["status"].asText()!!
    val aktørId = jsonNode["aktorId"].asText()!!
    val fom get() = jsonNode["fom"].asText().let { LocalDate.parse(it) }
    val tom get() = jsonNode["tom"].asText().let { LocalDate.parse(it) }
    val opprettet get() = jsonNode["opprettet"].asText().let { LocalDateTime.parse(it) }
    val egenmeldinger get() = jsonNode["egenmeldinger"]?.map { Periode(it) } ?: emptyList()
    val sykeperioder get() = jsonNode["soknadsperioder"]?.map { Periode(it) } ?: emptyList()
    val fraværsperioder get() = jsonNode["fravar"]?.filterNot {
        Fraværstype.valueOf(it["type"].textValue()) in listOf(Fraværstype.UTDANNING_FULLTID, Fraværstype.UTDANNING_DELTID)
    }?.map { FraværsPeriode(it) } ?: emptyList()

    val utdanningsperioder get() = jsonNode["fravar"]?.filter {
        Fraværstype.valueOf(it["type"].textValue()) in listOf(Fraværstype.UTDANNING_FULLTID, Fraværstype.UTDANNING_DELTID)
    }?.map { Utdanningsfraværsperiode(it) } ?: emptyList()

    val arbeidGjenopptatt get() = jsonNode["arbeidGjenopptatt"]?.safelyUnwrapDate()
    val korrigerer get() = jsonNode["korrigerer"]?.asText()

    override fun aktørId() = aktørId
    override fun organisasjonsnummer(): String? = jsonNode["arbeidsgiver"]?.get("orgnummer")?.textValue()
    override fun rapportertdato(): LocalDateTime = opprettet
    override fun compareTo(other: Sykdomshendelse): Int = opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = sykeperioder.map {
            Sykdomstidslinje.sykedager(it.fom, it.tom, this)
        }
    private val egenmeldingsTidslinje
        get(): List<Sykdomstidslinje> = egenmeldinger.map {
            Sykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this)
        }
    private val ferieTidslinje
        get(): List<Sykdomstidslinje> = fraværsperioder.filter { it.type == Fraværstype.FERIE }.map {
            Sykdomstidslinje.ferie(it.fom, it.tom, this)
        }
    private val arbeidGjenopptattTidslinje
        get(): List<Sykdomstidslinje> = arbeidGjenopptatt?.let {
            listOf(Sykdomstidslinje.ikkeSykedager(it, tom, this))
        } ?: emptyList()

    private val studiedagertidslinje = utdanningsperioder.map {
        Sykdomstidslinje.studiedager(it.fom, tom, this)
    }

    override fun sykdomstidslinje() = (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje)
        .reduce { resultatTidslinje, delTidslinje -> resultatTidslinje + delTidslinje }

    override fun toJson(): JsonNode = jsonNode
}

class NySykepengesøknad(jsonNode: JsonNode) : Sykepengesøknad(jsonNode) {
    init {
        require(status == SØKNAD_NY || status == SØKNAD_FREMTIDIG) { "Søknaden må være ny eller fremtidig" }
    }

    override fun hendelsetype() =
        Sykdomshendelse.Type.NySykepengesøknad
}

class SendtSykepengesøknad(jsonNode: JsonNode) : Sykepengesøknad(jsonNode) {
    init {
        require(status == SØKNAD_SENDT) { "Søknaden må være sendt" }
    }

    override fun hendelsetype() =
        Sykdomshendelse.Type.SendtSykepengesøknad
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
