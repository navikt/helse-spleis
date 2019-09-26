package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.readResource
import no.nav.helse.sykmelding.domain.Periode
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SakskompleksKtTest {
    companion object {
        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSykmelding = SykmeldingMessage(objectMapper.readTree("/sykmelding.json".readResource()))

        private val testSøknad =
            Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))

        private val enInntektsmeldingSomJson = InntektsmeldingConsumer.inntektsmeldingObjectMapper.readTree("/inntektsmelding.json".readResource())
        private val enInntektsmelding = Inntektsmelding(enInntektsmeldingSomJson)
    }

    @Test
    fun `toJson gir en tilstand`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        val json = sakskompleks.toJson()
        val node = objectMapper.readTree(json)

        assertNotNull(node["tilstand"])
        assertFalse(node["tilstand"].isNull)
        assertTrue(node["tilstand"].textValue().isNotEmpty())
    }

    @Test
    fun `toJson gir aktørId og sakskompleksId`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        val json = sakskompleks.toJson()
        val node = objectMapper.readTree(json)

        assertEquals(id.toString(), node["id"].textValue())
        assertEquals("aktørId", node["aktørId"].textValue())
    }

    @Test
    fun `toJson gir sykmeldinger`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(testSykmelding.sykmelding)

        val json = sakskompleks.toJson()
        val node = objectMapper.readTree(json)

        assertTrue(node["sykmeldinger"].isArray)
        assertEquals(testSykmelding.sykmelding.id, node["sykmeldinger"][0]["id"].textValue())
    }

    @Test
    fun `toJson gir inntektsmeldinger`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(testSykmelding.sykmelding)
        sakskompleks.leggTil(enInntektsmelding)

        val json = sakskompleks.toJson()
        val node = objectMapper.readTree(json)

        assertTrue(node["inntektsmeldinger"].isArray)
        assertEquals(enInntektsmelding.inntektsmeldingId, node["inntektsmeldinger"][0]["inntektsmeldingId"].textValue())
    }

    @Test
    fun `toJson gir søknader`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(testSykmelding.sykmelding)
        sakskompleks.leggTil(testSøknad)

        val json = sakskompleks.toJson()
        val node = objectMapper.readTree(json)

        assertTrue(node["søknader"].isArray)
        assertEquals(testSøknad.id, node["søknader"][0]["id"].textValue())
    }

    @Test
    fun `fromJson bygger opp likt objekt fra lagret tilstand`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
            id = id,
            aktørId = "aktørId"
        )

        val innJson = sakskompleks.toJson()

        val nyttSakskompleks = Sakskompleks(innJson)
        val outJson = nyttSakskompleks.toJson()
        val inNode = objectMapper.readTree(innJson)
        val outNode = objectMapper.readTree(outJson)

        assertEquals(inNode, outNode)
    }

    @Test
    fun `bruker datoer fra egenmeldingen om den er før sykmeldingen`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            egenmeldinger = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 16),
                    tom = LocalDate.of(2019, 8, 18)
                )
            )
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)

        assertEquals(LocalDate.of(2019, 8, 16), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 27), sakskompleks.tom())
    }

    @Test
    fun `bruker syketilefelleStartDato om den peker på en dato tidligere enn sykmeldingen`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 1),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            egenmeldinger = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 16),
                    tom = LocalDate.of(2019, 8, 18)
                )
            )
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)

        assertEquals(LocalDate.of(2019, 8, 1), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 27), sakskompleks.tom())
    }

    @Test
    fun `bruker syketilefelle | fom om vi ikke har andre tidligere datoer`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)

        assertEquals(LocalDate.of(2019, 8, 19), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 27), sakskompleks.tom())
    }

    @Test
    fun `arbeidGjennopptatt overstyrer sykmeldingsperiode om den er satt`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            arbeidGjenopptatt = LocalDate.of(2019, 8, 26)
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)

        assertEquals(LocalDate.of(2019, 8, 19), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 26), sakskompleks.tom())
    }

    @Test
    fun `bruker ikke korrigerte søknader til av beregne arbeidGjenopptatt i søknader`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        val søknad = søknad(
            id = "id1",
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
        )

        val korrigering = søknad(
            id = "id2",
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 26),
            arbeidGjenopptatt = LocalDate.of(2019, 8, 26),
            korrigerer = "id1"
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)
        sakskompleks.leggTil(korrigering)

        sakskompleks.leggTil(sykmelding)

        assertEquals(LocalDate.of(2019, 8, 19), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 26), sakskompleks.tom())
    }

    @Test
    fun `testsykmelding overskriver felter riktig`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            perioder = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        assertEquals(LocalDate.of(2019, 8, 19), sykmelding.syketilfelleStartDato)

        assertEquals(1, sykmelding.perioder.size)
        assertEquals(LocalDate.of(2019, 8, 19), sykmelding.perioder[0].fom)
        assertEquals(LocalDate.of(2019, 8, 27), sykmelding.perioder[0].tom)
    }

    @Test
    fun `testSøknad overskriver felter riktig`() {
        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            egenmeldinger = listOf(
                periode(
                    fom = LocalDate.of(2019, 8, 16),
                    tom = LocalDate.of(2019, 8, 18)
                )
            ),
            arbeidGjenopptatt = LocalDate.of(2019, 8, 26)
        )
        assertEquals(LocalDate.of(2019, 8, 19), søknad.fom)
        assertEquals(LocalDate.of(2019, 8, 27), søknad.tom)

        assertEquals(1, søknad.egenmeldinger.size)
        assertEquals(LocalDate.of(2019, 8, 16), søknad.egenmeldinger[0].fom)
        assertEquals(LocalDate.of(2019, 8, 18), søknad.egenmeldinger[0].tom)

        assertEquals(LocalDate.of(2019, 8, 26), søknad.arbeidGjenopptatt)
    }

    @Test
    fun `testPeriode kan mappes til Periode`() {
        val periode = periode(
            fom = LocalDate.of(2019, 8, 1),
            tom = LocalDate.of(2019, 8, 10)
        )
        assertEquals(LocalDate.of(2019, 8, 1), periode.fom)
        assertEquals(LocalDate.of(2019, 8, 10), periode.tom)
    }
}


fun sykmelding(
    syketilfelleStartDato: LocalDate = LocalDate.of(2019, 6, 1),
    perioder: List<Periode> = listOf(
        periode(
            fom = LocalDate.of(2019, 6, 1),
            tom = LocalDate.of(2019, 6, 14)
        )
    )
): Sykmelding {
    val json = SakskompleksKtTest.objectMapper.readTree("/sykmelding.json".readResource())

    (json["sykmelding"] as ObjectNode).put("syketilfelleStartDato", syketilfelleStartDato.toString())

    (json["sykmelding"] as ObjectNode).replace(
        "perioder", JsonNodeFactory.instance.arrayNode().addAll(
            perioder.map {
                JsonNodeFactory.instance.objectNode()
                    .put("fom", it.fom.toString())
                    .put("tom", it.tom.toString())
            }
        )
    )

    return SykmeldingMessage(json).sykmelding
}

fun søknad(
    id: String = "68da259c-ff7f-47cf-8fa0-c348ae95e220",
    fom: LocalDate = LocalDate.of(2019, 8, 1),
    tom: LocalDate = LocalDate.of(2019, 8, 14),
    egenmeldinger: List<Periode> = emptyList(),
    arbeidGjenopptatt: LocalDate? = null,
    korrigerer: String? = null
): Sykepengesøknad {
    val json =
        SakskompleksKtTest.objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()) as ObjectNode

    json.put("id", id)
    json.put("fom", fom.toString())
    json.put("tom", tom.toString())
    json.put("korrigerer", korrigerer)

    json.replace(
        "egenmeldinger", JsonNodeFactory.instance.arrayNode().addAll(
            egenmeldinger.map {
                JsonNodeFactory.instance.objectNode()
                    .put("fom", it.fom.toString())
                    .put("tom", it.tom.toString())
            }
        )
    )

    json.put("arbeidGjenopptatt", arbeidGjenopptatt?.toString())

    return Sykepengesøknad(json)
}

fun periode(fom: LocalDate, tom: LocalDate): Periode =
    Periode(
        JsonNodeFactory.instance.objectNode()
            .put("fom", fom.toString())
            .put("tom", tom.toString())
    )
