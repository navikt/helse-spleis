package no.nav.helse.unit.sakskompleks.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.søknad
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.readResource
import no.nav.helse.person.domain.Sakskompleks
import no.nav.syfo.kafka.sykepengesoknad.dto.PeriodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SakskompleksKtTest {
    companion object {
        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val standardNySøknad = søknad(status = SoknadsstatusDTO.NY)
        private val standardSendtSøknad = søknad(status = SoknadsstatusDTO.SENDT)

        private val enInntektsmeldingSomJson = InntektsmeldingConsumer.inntektsmeldingObjectMapper.readTree("/inntektsmelding.json".readResource())
        private val enInntektsmelding = Inntektsmelding(enInntektsmeldingSomJson)
    }

    @Test
    fun `state inneholder en tilstand`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId"
        )

        val memento = sakskompleks.memento()
        val node = objectMapper.readTree(memento.state)

        assertNotNull(node["tilstand"])
        assertFalse(node["tilstand"].isNull)
        assertTrue(node["tilstand"].textValue().isNotEmpty())
    }

    @Test
    fun `state inneholder aktørId og sakskompleksId`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId"
        )

        val memento = sakskompleks.memento()
        val node = objectMapper.readTree(memento.state)

        assertEquals(id.toString(), node["id"].textValue())
        assertEquals("aktørId", node["aktørId"].textValue())
    }

    @Test
    fun `state inneholder inntektsmeldinger`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId"
        )

        sakskompleks.leggTil(standardNySøknad)
        sakskompleks.leggTil(enInntektsmelding)

        val memento = sakskompleks.memento()
        val node = objectMapper.readTree(memento.state)

        assertTrue(node["inntektsmeldinger"].isArray)
        assertEquals(enInntektsmelding.inntektsmeldingId, node["inntektsmeldinger"][0]["inntektsmeldingId"].textValue())
    }

    @Test
    fun `restore bygger opp likt objekt fra lagret state`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId"
        )
        sakskompleks.leggTil(standardNySøknad)
        sakskompleks.leggTil(standardSendtSøknad)
        sakskompleks.leggTil(enInntektsmelding)

        val inMemento = sakskompleks.memento()

        val nyttSakskompleks = Sakskompleks.restore(inMemento)
        val outMemento = nyttSakskompleks.memento()
        val inNode = objectMapper.readTree(inMemento.state)
        val outNode = objectMapper.readTree(outMemento.state)

        assertEquals(inNode, outNode)
    }

    @Test
    fun `bruker datoer fra egenmeldingen om den er før sykmeldingen`() {
        val sykmelding = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            ),
            egenmeldinger = emptyList()
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            ),
            egenmeldinger = listOf(
                PeriodeDTO(
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
    }

    @Test
    fun `bruker syketilefelle fom om vi ikke har andre tidligere datoer`() {
        val sykmelding = søknad(
            fom = LocalDate.of(2019, 8, 19),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
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
    }

    @Test
    fun `arbeidGjennopptatt overstyrer sykmeldingsperiode om den er satt`() {
        val sykmelding = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            ),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )

        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            ),
            egenmeldinger = emptyList(),
            arbeidGjenopptatt = LocalDate.of(2019, 8, 26),
            fravær = emptyList()
        )

        val sakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "aktørId"
        )

        sakskompleks.leggTil(sykmelding)
        sakskompleks.leggTil(søknad)

        assertEquals(LocalDate.of(2019, 8, 19), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 25), sakskompleks.sisteSykdag())
    }

    @Disabled("denne må testes på nytt når sykdomstidslinjen er på plass")
    @Test
    fun `bruker ikke korrigerte søknader til av beregne arbeidGjenopptatt i søknader`() {
        val nySøknad = søknad(
                status = SoknadsstatusDTO.NY,
                fom = LocalDate.of(2019, 8, 19),
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(
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

        sakskompleks.leggTil(nySøknad)
        sakskompleks.leggTil(søknad)
        sakskompleks.leggTil(korrigering)

        sakskompleks.leggTil(nySøknad)

        assertEquals(LocalDate.of(2019, 8, 19), sakskompleks.fom())
        assertEquals(LocalDate.of(2019, 8, 26), sakskompleks.tom())
    }

    @Test
    fun `testsykmelding overskriver felter riktig`() {
        val nySøknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            søknadsperioder = listOf(
                SoknadsperiodeDTO(
                    fom = LocalDate.of(2019, 8, 19),
                    tom = LocalDate.of(2019, 8, 27)
                )
            )
        )

        assertEquals(LocalDate.of(2019, 8, 19), nySøknad.fom)

        assertEquals(1, nySøknad.sykeperioder.size)
        assertEquals(LocalDate.of(2019, 8, 19), nySøknad.sykeperioder[0].fom)
        assertEquals(LocalDate.of(2019, 8, 27), nySøknad.sykeperioder[0].tom)
    }

    @Test
    fun `sendtSøknad overskriver felter riktig`() {
        val søknad = søknad(
            fom = LocalDate.of(2019, 8, 19),
            tom = LocalDate.of(2019, 8, 27),
            egenmeldinger = listOf(
                PeriodeDTO(
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
        val periode = PeriodeDTO(
            fom = LocalDate.of(2019, 8, 1),
            tom = LocalDate.of(2019, 8, 10)
        )
        assertEquals(LocalDate.of(2019, 8, 1), periode.fom)
        assertEquals(LocalDate.of(2019, 8, 10), periode.tom)
    }
}
