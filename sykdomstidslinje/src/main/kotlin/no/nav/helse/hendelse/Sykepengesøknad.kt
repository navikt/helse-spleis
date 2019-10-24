package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private const val SØKNAD_SENDT = "SENDT"
private const val SØKNAD_NY = "NY"
private const val SØKNAD_FREMTIDIG = "FREMTIDIG"

class NySøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad): PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): NySøknadHendelse {
            return NySøknadHendelse(
                jsonNode["hendelseId"].textValue(),
                Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun aktørId() =
        søknad.aktørId

    override fun organisasjonsnummer(): String? =
        søknad.arbeidsgiver?.orgnummer

    override fun rapportertdato(): LocalDateTime =
        søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
        søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = søknad.sykeperioder
            .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }

    override fun sykdomstidslinje() =
        sykeperiodeTidslinje.reduce { resultatTidslinje, delTidslinje ->
            resultatTidslinje + delTidslinje
        }

    override fun hendelsetype() =
        Type.NySøknadMottatt

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).set("søknad", søknad.toJson())
    }
}

class SendtSøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad): PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): SendtSøknadHendelse {
            return SendtSøknadHendelse(
                jsonNode["hendelseId"].textValue(),
                Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun aktørId() =
        søknad.aktørId

    override fun organisasjonsnummer(): String? =
        søknad.arbeidsgiver?.orgnummer

    override fun rapportertdato(): LocalDateTime =
        søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
        søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = søknad.sykeperioder
            .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }

    private val egenmeldingsTidslinje
        get(): List<Sykdomstidslinje> = søknad.egenmeldinger
            .map { Sykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }

    private val ferieTidslinje
        get(): List<Sykdomstidslinje> = søknad.fraværsperioder
            .filter { it.type == Sykepengesøknad.Fraværstype.FERIE }
            .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }

    private val permisjonTidslinje
        get(): List<Sykdomstidslinje> = søknad.fraværsperioder
            .filter { it.type == Sykepengesøknad.Fraværstype.PERMISJON }
            .map { Sykdomstidslinje.permisjonsdager(it.fom, it.tom, this) }

    private val arbeidGjenopptattTidslinje
        get(): List<Sykdomstidslinje> = søknad.arbeidGjenopptatt
            ?.let { listOf(Sykdomstidslinje.ikkeSykedager(it, søknad.tom, this)) }
            ?: emptyList()

    private val studiedagertidslinje = søknad.utdanningsperioder.map {
        Sykdomstidslinje.studiedager(it.fom, søknad.tom, this)
    }

    override fun sykdomstidslinje() =
        (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje + permisjonTidslinje)
            .reduce { resultatTidslinje, delTidslinje ->
                resultatTidslinje + delTidslinje
            }

    override fun hendelsetype() =
        Type.SendtSøknadMottatt

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).set("søknad", søknad.toJson())
    }
}

@JsonSerialize(using = SykepengesøknadSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
data class Sykepengesøknad(private val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!
    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!
    val status = jsonNode["status"].asText()!!
    val aktørId = jsonNode["aktorId"].asText()!!
    val fom get() = jsonNode["fom"].asText().let { LocalDate.parse(it) }
    val tom get() = jsonNode["tom"].asText().let { LocalDate.parse(it) }
    val opprettet get() = jsonNode["opprettet"].asText().let { LocalDateTime.parse(it) }
    val egenmeldinger get() = jsonNode["egenmeldinger"]?.map { Periode(it) } ?: emptyList()
    val sykeperioder get() = jsonNode["soknadsperioder"]?.map { Periode(it) } ?: emptyList()
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

    val arbeidsgiver: Arbeidsgiver? get() = jsonNode["arbeidsgiver"]?.let { Arbeidsgiver(it) }

    fun toJson(): JsonNode = jsonNode

    data class Arbeidsgiver(val jsonNode: JsonNode) {
        val orgnummer: String? get() = jsonNode["orgnummer"]?.textValue()
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
}
