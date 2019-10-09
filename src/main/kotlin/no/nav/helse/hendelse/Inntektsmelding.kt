package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.Event
import no.nav.helse.person.domain.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.KildeHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

@JsonSerialize(using = SykdomsheldelseSerializer::class)
@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Inntektsmelding(val jsonNode: JsonNode): Event, Sykdomshendelse {
    override fun toJson() = jsonNode.toString()

    override fun compareTo(other: KildeHendelse): Int {
        TODO("not implemented")
    }

    override fun rapportertdato(): LocalDateTime {
        TODO("not implemented")
    }

    override fun aktørId() = arbeidstakerAktorId

    override fun organisasjonsnummer() = virksomhetsnummer

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented")
    }

    override fun eventType() = Event.Type.Inntektsmelding

    val inntektsmeldingId = jsonNode["inntektsmeldingId"].asText() as String

    val arbeidstakerAktorId = jsonNode["arbeidstakerAktorId"].textValue() as String

    val virksomhetsnummer: String? get() = jsonNode["virksomhetsnummer"]?.textValue()

    val arbeidsgiverAktorId: String? get() = jsonNode["arbeidsgiverAktorId"]?.textValue()

    val arbeidsgiverperioder get() = jsonNode["arbeidsgiverperioder"].map {
        Periode(it)
    }

    val sisteDagIArbeidsgiverPeriode get() = arbeidsgiverperioder.maxBy {
        it.tom
    }?.tom

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

}

