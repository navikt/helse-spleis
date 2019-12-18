package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.egenmeldingsdag
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.ikkeSykedag
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.permisjonsdag
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.studiedag
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.sykedag
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.utenlandsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.JsonDagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SykdomstidslinjeJsonTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val inntektsmelding = inntektsmeldingHendelse()
    private val sendtSøknad = sendtSøknadHendelse()


    @Test
    fun `gitt en tidslinje så serialiseres den med en json pr hendelse, som refereses til med id fra dag`() {

        val tidslinje = ConcreteSykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), sendtSøknad
        )

        val tidslinjeJson = objectMapper.readTree(tidslinje.toJson())
        tidslinjeJson["hendelser"].elements().forEach {
            assertEquals(sendtSøknad.hendelseId(), UUID.fromString(it["hendelseId"].asText()))
        }
        tidslinjeJson["dager"].elements().forEach {
            assertEquals(sendtSøknad.hendelseId(), UUID.fromString(it["hendelseId"].asText()))
        }
    }

    @Test
    fun `hendeler på erstattede dager blir også normalisert`() {

        val tidslinjeB = ConcreteSykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding
        )
        val tidslinjeC = ConcreteSykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), sendtSøknad
        )

        val combined = tidslinjeB + tidslinjeC

        val tidslinjeJson = objectMapper.readTree(combined.toJson()).also { println(it) }


        val hendelser = tidslinjeJson["hendelser"]
        assertEquals(hendelser.size(), 2)
        val hendelseMap = hendelser.groupBy { it["hendelseId"].asText() }
        tidslinjeJson["dager"].elements().forEach {
            val hendelseId = it["hendelseId"].asText()
            assertTrue(hendelseMap.containsKey(hendelseId))
            if (!it["erstatter"].isEmpty)
                it["erstatter"].onEach {
                    assertTrue(hendelseMap.containsKey(it["hendelseId"].asText()))
                }
        }
    }

    @Test
    fun `lagring og restoring av en sykdomstidslinje med har de samme egenskapene som den opprinnelige`() {
        val tidslinjeA = ConcreteSykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 1),
            LocalDate.of(2019, 10, 3), inntektsmelding
        )
        val tidslinjeB = ConcreteSykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding
        )
        val tidslinjeC = ConcreteSykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), sendtSøknad
        )

        val combined = tidslinjeA + tidslinjeB + tidslinjeC
        val json = combined.toJson()

        val restored = ConcreteSykdomstidslinje.fromJson(json)

        assertSykdomstidslinjerEquals(combined, restored)
    }

    @Test
    fun `lagring og restoring av en sykdomstidslinje med søknader og inntektsmeldinger har like egenskaper`() {
        val egenmelding =
            ConcreteSykdomstidslinje.egenmeldingsdager(
                LocalDate.of(2019, 9, 30),
                LocalDate.of(2019, 10, 1),
                sendtSøknad
            )
        val sykedagerA = ConcreteSykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 2),
            LocalDate.of(2019, 10, 4), sendtSøknad
        )
        val ikkeSykedager = ConcreteSykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding
        )
        val sykedagerB = ConcreteSykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), sendtSøknad
        )

        val combined = egenmelding + sykedagerA + ikkeSykedager + sykedagerB
        val json = combined.toJson()

        val restored = ConcreteSykdomstidslinje.fromJson(json)
        assertSykdomstidslinjerEquals(combined, restored)
    }

    @Test
    fun `sykdomstidslinje med alle typer dager blir serialisert riktig`() {
        egenmeldingsdag(LocalDate.of(2019, 10, 7), inntektsmelding).also {
            assertJsonDagType(JsonDagType.EGENMELDINGSDAG, it)
        }

        sykedag(LocalDate.of(2019, 10, 8), sendtSøknad).also {
            assertJsonDagType(JsonDagType.SYKEDAG, it)
        }

        ferie(LocalDate.of(2019, 10, 9), sendtSøknad).also {
            assertJsonDagType(JsonDagType.FERIEDAG, it)
        }

        permisjonsdag(LocalDate.of(2019, 10, 11), sendtSøknad).also {
            assertJsonDagType(JsonDagType.PERMISJONSDAG, it)
        }

        studiedag(LocalDate.of(2019, 10, 17), sendtSøknad).also {
            assertJsonDagType(JsonDagType.STUDIEDAG, it)
        }

        ikkeSykedag(LocalDate.of(2019, 10, 18), sendtSøknad).also {
            assertJsonDagType(JsonDagType.ARBEIDSDAG, it)
        }

        utenlandsdag(LocalDate.of(2019, 10, 22), sendtSøknad).also {
            assertJsonDagType(JsonDagType.UTENLANDSDAG, it)
        }
    }

    private fun assertJsonDagType(expectedType: JsonDagType, dag: Dag) {
        assertTrue(dag.toJson().contains("\"${expectedType.name}\""), "Tidslinje inneholder ikke dag-type ${expectedType.name}")
    }

    private fun assertSykdomstidslinjerEquals(expected: ConcreteSykdomstidslinje, actual: ConcreteSykdomstidslinje) {
        assertEquals(expected.førsteDag(), actual.førsteDag())
        assertEquals(expected.sisteDag(), actual.sisteDag())
        assertEquals(expected.length(), actual.length())

        val actualDager = actual.flatten()
        expected.flatten().forEachIndexed { key, dag ->
            assertDagEquals(dag, actualDager[key])
        }
    }

    private fun assertDagEquals(expected: Dag, actual: Dag) {
        assertEquals(expected::class, actual::class)
        assertEquals(expected.førsteDag(), actual.førsteDag())
        assertEquals(expected.sisteDag(), actual.sisteDag())
        assertEquals(expected.dagerErstattet().size, actual.dagerErstattet().size)

        val actualDager = actual.dagerErstattet()
        expected.dagerErstattet().forEachIndexed { key, dag ->
            assertDagEquals(dag, actualDager[key])
        }
    }
}
