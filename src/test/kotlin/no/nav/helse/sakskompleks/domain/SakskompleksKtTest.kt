package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import no.nav.helse.sykmelding.domain.Periode
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SakskompleksKtTest {
    companion object {
        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `bruker datoer fra egenmeldingen om den er før sykmeldingen`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
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
            aktørId = "aktørId",
            sykmeldinger = listOf(sykmelding),
            søknader = listOf(søknad)
        )

        assertEquals(LocalDate.of(2019, 8 ,16) ,sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8 ,27) ,sakskompleks.tom())
    }

    @Test
    fun `bruker syketilefelleStartDato om den peker på en dato tidligere enn sykmeldingen`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 1),
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
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
            aktørId = "aktørId",
            sykmeldinger = listOf(sykmelding),
            søknader = listOf(søknad)
        )

        assertEquals(LocalDate.of(2019, 8 ,1) ,sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8 ,27) ,sakskompleks.tom())
    }

    @Test
    fun `bruker syketilefelle | fom om vi ikke har andre tidligere datoer`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId",
            sykmeldinger = listOf(sykmelding),
            søknader = listOf(søknad)
        )

        assertEquals(LocalDate.of(2019, 8 ,19) ,sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8 ,27) ,sakskompleks.tom())
    }

    @Test
    fun `arbeidGjennopptatt overstyrer sykmeldingsperiode om den er satt`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            arbeidGjenopptatt = LocalDate.of(2019,8,26)
        )

        val sakskompleks = Sakskompleks(
            id = UUID.randomUUID(),
            aktørId = "aktørId",
            sykmeldinger = listOf(sykmelding),
            søknader = listOf(søknad)
        )

        assertEquals(LocalDate.of(2019, 8 ,19) ,sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8 ,26) ,sakskompleks.tom())
    }

    @Test
    fun `testsykmelding overskriver felter riktig`() {
        val sykmelding = sykmelding(
            syketilfelleStartDato = LocalDate.of(2019, 8, 19),
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27)
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
        assertEquals(LocalDate.of(2019,8,19), søknad.fom)
        assertEquals(LocalDate.of(2019,8,27), søknad.tom)

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
    fom: LocalDate = LocalDate.of(2019, 6, 1),
    tom: LocalDate = LocalDate.of(2019, 6, 14)
): Sykmelding {
    val json = SakskompleksKtTest.objectMapper.readTree("/sykmelding.json".readResource())

    (json["sykmelding"] as ObjectNode).put("syketilfelleStartDato", syketilfelleStartDato.toString())

    (json["sykmelding"] as ObjectNode).replace(
        "perioder", JsonNodeFactory.instance.arrayNode().add(
            JsonNodeFactory.instance.objectNode()
                .put("fom", fom.toString())
                .put("tom", tom.toString())
        )
    )

    return SykmeldingMessage(json).sykmelding
}

fun søknad(
    fom: LocalDate = LocalDate.of(2019, 8, 1),
    tom: LocalDate = LocalDate.of(2019, 8, 14),
    egenmeldinger: List<Periode> = emptyList(),
    arbeidGjenopptatt: LocalDate? = null
): Sykepengesøknad {
    val json =
        SakskompleksKtTest.objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()) as ObjectNode

    json.put("fom", fom.toString())
    json.put("tom", tom.toString())

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
