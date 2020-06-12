package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList

private typealias Datoer = Pair<LocalDate, LocalDate>

internal class V21SykdomshistorikkMergeTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun test() {
        val sykmelding = DagKilde.Sykmelding()
        val inntektsmelding = DagKilde.Inntektsmelding()
        val søknad = DagKilde.Søknad()

        val sykmeldingElement = element(
            kilde = sykmelding,
            hendelseSykdomstidslinje = (11.januar to 31.januar).sykedager(sykmelding),
            beregnetSykdomstidslinje = (11.januar to 31.januar).sykedager(sykmelding)
        )

        val inntektsmeldingElement = element(
            kilde = inntektsmelding,
            hendelseSykdomstidslinje = (1.januar to 10.januar).arbeidsdager(inntektsmelding),
            beregnetSykdomstidslinje = (1.januar to 5.januar).arbeidsdager(inntektsmelding) +
                (5.januar to 10.januar).sykedager(inntektsmelding) +
                (11.januar to 31.januar).sykedager(sykmelding)
        )

        val søknadElement = element(
            kilde = søknad,
            hendelseSykdomstidslinje = (11.januar to 28.januar).sykedager(søknad) + (29.januar to 31.januar).arbeidsdager(
                søknad
            ),
            beregnetSykdomstidslinje = (1.januar to 5.januar).arbeidsdager(inntektsmelding) +
                (5.januar to 10.januar).sykedager(inntektsmelding) +
                (11.januar to 28.januar).sykedager(sykmelding) +
                (29.januar to 31.januar).arbeidsdager(søknad)
        )

        val personJson = personJson(
            vedtaksperioder = vedtaksperiode(
                historikkElementer = søknadElement + inntektsmeldingElement + sykmeldingElement

            ),
            forkastedeVedtaksperioder = objectMapper.createArrayNode(),
            arbeidsgiverHistorikkElementer = objectMapper.createArrayNode()
        )
        val result = 1.januar.dager("SSSSSHH SSSSSHH")
        DagKilde.Søknad().also { kilde ->
            val element = 2.februar.element(
                kilde,
                8.januar.dager("SSSSSHH", kilde),
                1.januar.dager("SSSSSHH SSSSSHH", kilde)
            )
        }

        println(personJson.toPrettyString())
    }

    private fun LocalDate.element(kilde: DagKilde, hendelseSykdomstidslinje: String, beregnetSykdomstidslinje: String) =
        """
            {
            "hendelseId" : ${kilde.uuid},
            "tidsstempel" : ${this.atStartOfDay()},
            "beregnetSykdomstidslinje" : $hendelseSykdomstidslinje,
            "beregnetSykdomstidslinje" : $beregnetSykdomstidslinje
            }
        """

    private fun LocalDate.dager(dagString: String, kilde: DagKilde = DagKilde.Søknad()): String {
        var dato = this
        return dagString.toCharArray().filterNot { it == ' ' }.map {
            when (it) {
                'S' -> dagString("Sykedag", dato, kilde)
                'H' -> dagString("SykHelgedag", dato, kilde)
                'A' -> dagString("Arbeidsdag", dato, kilde)
                'U' -> dagString("Arbeidsgiverdag", dato, kilde)
                'G' -> dagString("ArbeidsgiverHelgedag", dato, kilde)
                'F' -> dagString("Feriedag", dato, kilde)
                'R' -> dagString("FriskHelgedag", dato, kilde)
                else -> throw IllegalArgumentException("Don't recognize character $it")
            }.also { dato = dato.plusDays(1) }
        }.joinToString(",", "[", "]")
    }

    private fun dagString(type: String, dato: LocalDate, kilde: DagKilde) = """
        {
          "dato": "$dato",
          "type": "$type",
          "kilde": {
            "type": "${kilde.type}",
            "id": "${kilde.uuid}"
          },
          "grad": 100.0,
          "arbeidsgiverBetalingProsent": 100.0
        }"""

    sealed class DagKilde(internal val uuid: UUID, internal val type: String) {
        class Sykmelding : DagKilde(UUID.randomUUID(), "SYKMELDING")
        class Søknad : DagKilde(UUID.randomUUID(), "SØKNAD")
        class Inntektsmelding : DagKilde(UUID.randomUUID(), "INNTEKTSMELDING")
    }

    operator fun ArrayNode.plus(other: ArrayNode) = objectMapper.createArrayNode().apply {
        addAll(this@plus)
        addAll(other)
    }

    @Language("JSON")
    fun element(
        kilde: DagKilde,
        hendelseSykdomstidslinje: ArrayNode,
        beregnetSykdomstidslinje: ArrayNode
    ) = objectMapper.createArrayNode().apply {
        add(
            objectMapper.readTree(
                """
        {
          "hendelseId": "${kilde.uuid}",
          "tidsstempel": "${LocalDateTime.now()}",
          "beregnetSykdomstidslinje": $beregnetSykdomstidslinje,
          "hendelseSykdomstidslinje": $hendelseSykdomstidslinje
        }
        """
            )
        )
    }

    fun Datoer.sykedager(kilde: DagKilde) = dager("Sykedag", kilde)
    fun Datoer.arbeidsdager(kilde: DagKilde) = dager("Arbeidsdag", kilde)

    fun Pair<LocalDate, LocalDate>.dager(type: String, kilde: DagKilde) = first.datesUntil(second.plusDays(1))
        .map { it.dag(type, kilde) }
        .toList()
        .fold(objectMapper.createArrayNode()) { acc, node ->
            acc.add(node)
        }

    @Language("JSON")
    fun LocalDate.dag(dagType: String, kilde: DagKilde) = objectMapper.readTree(
        """
        {
          "dato": "$this",
          "type": "$dagType",
          "kilde": {
            "type": "${kilde.type}",
            "id": "${kilde.uuid}"
          },
          "grad": 100.0,
          "arbeidsgiverBetalingProsent": 100.0
        }
    """
    )

    fun vedtaksperiode(
        historikkElementer: ArrayNode
    ) = objectMapper.createArrayNode().apply {
        add(objectMapper.readTree("""{"sykdomshistorikk": $historikkElementer}"""))
    }


    @Language("JSON")
    fun personJson(
        vedtaksperioder: ArrayNode,
        forkastedeVedtaksperioder: ArrayNode,
        arbeidsgiverHistorikkElementer: ArrayNode
    ) = objectMapper.readTree(
        """{
    "aktørId": "12345",
    "fødselsnummer": "12020052345",
    "arbeidsgivere": [
        {
            "sykdomshistorikk": $arbeidsgiverHistorikkElementer,
            "vedtaksperioder": $vedtaksperioder,
            "forkastede": $forkastedeVedtaksperioder
        }
    ],
    "skjemaVersjon": 20
}
"""
    )
}
