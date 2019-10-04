package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

const val SØKNAD_SENDT = "SENDT"
const val SØKNAD_NY = "NY"
const val SØKNAD_FREMTIDIG = "FREMTIDIG"

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
    override fun compareTo(other: Sykdomshendelse): Int = opprettet.compareTo(other.rapportertdato())

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
