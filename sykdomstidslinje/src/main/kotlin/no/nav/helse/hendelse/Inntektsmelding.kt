package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

@JsonSerialize(using = SykdomsheldelseSerializer::class)
@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Inntektsmelding(val jsonNode: JsonNode): Event, Sykdomshendelse {
    val arbeidsgiverFnr: String? get() = jsonNode["arbeidsgiverFnr"]?.textValue()

    val førsteFraværsdag: LocalDate get() = LocalDate.parse(jsonNode["forsteFravarsdag"].textValue())
    val rapportertDato: LocalDateTime get() = LocalDateTime.parse(jsonNode["rapportertDato"].textValue())

    val ferie
        get() = jsonNode["ferie"].map {
            Periode(it)
        }

    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String

    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String

    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()

    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()

    val arbeidsgiverperioder
        get() = jsonNode["arbeidsgiverperioder"].map {
            Periode(it)
        }

    val sisteDagIArbeidsgiverPeriode
        get() = arbeidsgiverperioder.maxBy {
            it.tom
        }?.tom

    override fun rapportertdato() = rapportertDato

    override fun eventType() = Event.Type.Inntektsmelding

    override fun aktørId(): String = arbeidstakerAktorId

    override fun organisasjonsnummer(): String? = virksomhetsnummer

    override fun sykdomstidslinje(): Sykdomstidslinje {
        val arbeidsgiverperiodetidslinjer = arbeidsgiverperioder
            .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }
        val ferietidslinjer = ferie
            .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }
        val førsteFraværsdagTidslinje = listOf(Sykdomstidslinje.sykedager(gjelder = førsteFraværsdag, hendelse = this))

        return (førsteFraværsdagTidslinje + arbeidsgiverperiodetidslinjer + ferietidslinjer)
            .reduce { resultatTidslinje, delTidslinje -> resultatTidslinje + delTidslinje }
    }

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

    override fun toJson(): JsonNode = jsonNode
}

