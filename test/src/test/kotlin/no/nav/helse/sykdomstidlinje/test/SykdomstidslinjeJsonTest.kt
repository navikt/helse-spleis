package no.nav.helse.sykdomstidlinje.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.JsonDagType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomstidslinjeJsonTest {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    val inntektsmelding =
        Inntektsmelding(objectMapper.readTree(SykdomstidslinjeJsonTest::class.java.getResourceAsStream("/inntektsmelding.json")))
    val søknadSendt =
        SendtSykepengesøknad(objectMapper.readTree(SykdomstidslinjeJsonTest::class.java.getResourceAsStream("/søknad_arbeidstaker_sendt_nav.json")))


    @Test
    fun `gitt en tidslinje så serialiseres den med et json pr hendelse, som refereses til med id fra dag`() {

        val tidslinje = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), søknadSendt
        )

        val tidslinjeJson = objectMapper.readTree(tidslinje.toJson())
        tidslinjeJson.elements().forEach {
            assertEquals(søknadSendt.hendelsetype().name, it["hendelse"]["type"].asText())
            assertEquals(søknadSendt.hendelseId(), it["hendelse"]["hendelseid"])
            assertNull(it["hendelse"]["json"])
        }
    }

    @Test
    fun `lagring og restoring av en sykdomstidslinje med har de samme egenskapene som den opprinnelige`() {
        val tidslinjeA = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 1),
            LocalDate.of(2019, 10, 3), inntektsmelding
        )
        val tidslinjeB = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding
        )
        val tidslinjeC = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), søknadSendt
        )

        val combined = tidslinjeA + tidslinjeB + tidslinjeC
        val json = combined.toJson()

        val restored = Sykdomstidslinje.fromJson(json)

        assertSykdomstidslinjerEquals(combined, restored)
    }

    @Test
    fun `lagring og restoring av en sykdomstidslinje med søknader og inntektsmeldinger har like egenskaper`() {
        val egenmelding =
            Sykdomstidslinje.egenmeldingsdager(LocalDate.of(2019, 9, 30), LocalDate.of(2019, 10, 1), søknadSendt)
        val sykedagerA = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 2),
            LocalDate.of(2019, 10, 4), søknadSendt
        )
        val ikkeSykedager = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding
        )
        val sykedagerB = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), søknadSendt
        )

        val combined = egenmelding + sykedagerA + ikkeSykedager + sykedagerB
        val json = combined.toJson()

        val restored = Sykdomstidslinje.fromJson(json)
        assertSykdomstidslinjerEquals(combined, restored)
    }

    @Test
    fun `sykdomstidslinje med alle typer dager blir serialisert riktig`() {
        val egenmeldingsdag = Sykdomstidslinje.egenmeldingsdag(LocalDate.of(2019, 10, 7), inntektsmelding)
        val sykedag = Sykdomstidslinje.sykedag(LocalDate.of(2019, 10, 8), søknadSendt)
        val feriedag = ferie(LocalDate.of(2019, 10, 9), søknadSendt)
        val permisjonsdager =
            Sykdomstidslinje.permisjonsdager(LocalDate.of(2019, 10, 11), LocalDate.of(2019, 10, 12), søknadSendt)
        val sykedager = Sykdomstidslinje.sykedager(LocalDate.of(2019, 10, 13), LocalDate.of(2019, 10, 15), søknadSendt)

        val permisjonsdagForUbestemt = Sykdomstidslinje.permisjonsdag(LocalDate.of(2019, 10, 16), søknadSendt)
        val sykedagForUbestemt = Sykdomstidslinje.sykedag(LocalDate.of(2019, 10, 16), søknadSendt)
        val ubestemtdag = permisjonsdagForUbestemt + sykedagForUbestemt
        val studiedag = Sykdomstidslinje.studiedag(LocalDate.of(2019, 10, 17), søknadSendt)
        val arbeidsdag = Sykdomstidslinje.ikkeSykedag(LocalDate.of(2019, 10, 18), søknadSendt)
        val utenlandsdag = Sykdomstidslinje.utenlandsdag(LocalDate.of(2019, 10, 22), søknadSendt)

        val tidslinje =
            egenmeldingsdag + sykedag + feriedag + permisjonsdager + sykedager + ubestemtdag + studiedag + utenlandsdag + arbeidsdag

        val json = tidslinje.toJson()

        val restored = Sykdomstidslinje.fromJson(json)

        assertSykdomstidslinjerEquals(tidslinje.also { println(it) }, restored.also { println(it) })

        JsonDagType.values().forEach {
            assertTrue(json.contains("\"${it.name}\""), "Tidslinje inneholder ikke dag-type $it")
        }
    }

    private fun assertJsonEquals(expectedJsonPayload: String, json: String) {
        val mapper = ObjectMapper()
        assertEquals(mapper.readTree(expectedJsonPayload), mapper.readTree(json))

    }

    private fun assertSykdomstidslinjerEquals(expected: Sykdomstidslinje, actual: Sykdomstidslinje) {
        assertEquals(expected.startdato(), actual.startdato())
        assertEquals(expected.sluttdato(), actual.sluttdato())
        assertEquals(expected.antallSykedagerHvorViIkkeTellerMedHelg(), actual.antallSykedagerHvorViIkkeTellerMedHelg())
        assertEquals(expected.antallSykedagerHvorViTellerMedHelg(), actual.antallSykedagerHvorViTellerMedHelg())
        assertEquals(expected.length(), actual.length())

        val actualDager = actual.flatten()
        expected.flatten().forEachIndexed { key, dag ->
            assertDagEquals(dag, actualDager[key])
        }
    }

    private fun assertDagEquals(expected: Dag, actual: Dag) {
        assertEquals(expected::class, actual::class)
        assertEquals(expected.startdato(), actual.startdato())
        assertEquals(expected.sluttdato(), actual.sluttdato())
        assertEquals(expected.dagerErstattet().size, actual.dagerErstattet().size)

        val actualDager = actual.dagerErstattet()
        expected.dagerErstattet().forEachIndexed { key, dag ->
            assertDagEquals(dag, actualDager[key])
        }
    }
}
