package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SakskompleksServiceTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSykmelding = SykmeldingMessage(objectMapper.readTree("/sykmelding.json".readResource()))

        private val testSøknad =
            Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
    }

    @Test
    fun `skal ikke finne sak når søknaden ikke er tilknyttet en sak`() {
        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSøknad.aktørId)
        } returns emptyList()

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnSak(testSøknad)

        assertNull(sak)

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSøknad.aktørId)
        }
    }

    @Test
    fun `skal finne sak når søknaden er tilknyttet en sak`() {
        val sakForBruker = etSakskompleks(testSykmelding.sykmelding, testSøknad)

        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSøknad.aktørId)
        } returns listOf(sakForBruker)

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnSak(testSøknad)

        assertEquals(sakForBruker, sak)

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSøknad.aktørId)
        }
    }

    @Test
    fun `skal ikke finne sak når sykmeldingen ikke er tilknyttet en sak`() {
        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns emptyList()

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnSak(testSykmelding.sykmelding)

        assertNull(sak)

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        }
    }

    @Test
    fun `skal finne sak når sykmeldingen er tilknyttet en sak`() {
        val sakForBruker = etSakskompleks(testSykmelding.sykmelding)

        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns listOf(sakForBruker)

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnSak(testSykmelding.sykmelding)

        assertEquals(sakForBruker, sak)

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        }
    }

    @Disabled("The state machine in Sakskompleks doesn't support consecutive sykmeldinger yet")
    @Test
    fun `skal oppdatere sak når aktøren har en sak`() {
        val sakForBruker = etSakskompleks(testSykmelding.sykmelding)

        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns listOf(sakForBruker)

        every {
            sakskompleksDao.oppdaterSak(sakForBruker)
        } returns 1

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnEllerOpprettSak(testSykmelding.sykmelding)

        assertEquals(sakForBruker, sak)

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
            sakskompleksDao.oppdaterSak(sakForBruker)
        }
        verify(exactly = 0) {
            sakskompleksDao.opprettSak(any())
        }
    }

    @Test
    fun `skal opprette sak når sykmeldingen ikke er tilknyttet en sak`() {
        val sakskompleksDao = mockk<SakskompleksDao>()

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns emptyList()

        every {
            sakskompleksDao.opprettSak(match { sak ->
                sak.aktørId == testSykmelding.sykmelding.aktørId
                        && sak.har(testSykmelding.sykmelding)
            })
        } returns 1

        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val sak = sakskompleksService.finnEllerOpprettSak(testSykmelding.sykmelding)

        assertEquals(testSykmelding.sykmelding.aktørId, sak.aktørId)
        assertTrue(sak.har(testSykmelding.sykmelding))

        verify(exactly = 1) {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
            sakskompleksDao.opprettSak(any())
        }
    }

    @Test
    fun `skal oppdatere sakskompleks`() {
        val sakskompleksDao = mockk<SakskompleksDao>(relaxed = true)
        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val etSakskompleks = etSakskompleks(testSykmelding.sykmelding)

        sakskompleksService.leggSøknadPåSak(etSakskompleks, testSøknad)

        verify(exactly = 1) {
            sakskompleksDao.oppdaterSak(etSakskompleks)
        }
    }

    @Test
    fun `sykmelding utenfor 16 dager av sakskompleks blir nytt sakskompleks`() {
        val sakskompleksDao = mockk<SakskompleksDao>(relaxed = true)
        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val førsteSykmelding =
            Sykmelding(objectMapper.readTree("/case-to-adskilte-sykmeldinger/1-sykmelding.json".readResource())["sykmelding"])
        val andreSykmelding =
            Sykmelding(objectMapper.readTree("/case-to-adskilte-sykmeldinger/2-sykmelding.json".readResource())["sykmelding"])

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns listOf(etSakskompleks(førsteSykmelding))

        sakskompleksService.finnEllerOpprettSak(andreSykmelding)

        verify(exactly = 1) {
            sakskompleksDao.opprettSak(any())
        }

        verify(exactly = 0) {
            sakskompleksDao.oppdaterSak(any())
        }
    }

    @Disabled("The state machine in Sakskompleks doesn't support consecutive sykmeldinger yet")
    @Test
    fun `sykmelding innenfor 16 dager av sakskompleks blir koblet på sakskompleks`() {
        val sakskompleksDao = mockk<SakskompleksDao>(relaxed = true)
        val sakskompleksService = SakskompleksService(sakskompleksDao)

        val førsteSykmelding =
            Sykmelding(objectMapper.readTree("/case-to-påfølgende-sykmeldinger/1-sykmelding.json".readResource())["sykmelding"])
        val andreSykmelding =
            Sykmelding(objectMapper.readTree("/case-to-påfølgende-sykmeldinger/2-sykmelding.json".readResource())["sykmelding"])

        every {
            sakskompleksDao.finnSaker(testSykmelding.sykmelding.aktørId)
        } returns listOf(etSakskompleks(førsteSykmelding))

        sakskompleksService.finnEllerOpprettSak(andreSykmelding)

        verify(exactly = 0) {
            sakskompleksDao.opprettSak(any())
        }

        verify(exactly = 1) {
            sakskompleksDao.oppdaterSak(any())
        }
    }

    @Test
    fun `1 dag mellom fredag og tirsdag`() {
        assertEquals(
            1, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 8, 30),
                LocalDate.of(2019, 9, 3)
            )
        )
    }

    @Test
    fun `2 dager mellom lørdag og tirsdag`() {
        assertEquals(
            2, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 8, 31),
                LocalDate.of(2019, 9, 3)
            )
        )
    }

    @Test
    fun `1 dag mellom søndag og tirsdag`() {
        assertEquals(
            1, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 9, 1),
                LocalDate.of(2019, 9, 3)
            )
        )
    }

    @Test
    fun `1 dag mellom onsdag og fredag`() {
        assertEquals(
            1, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 9, 4),
                LocalDate.of(2019, 9, 6)
            )
        )
    }

    @Test
    fun `2 dager mellom onsdag og lørdag`() {
        assertEquals(
            2, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 9, 4),
                LocalDate.of(2019, 9, 7)
            )
        )
    }

    @Test
    fun `3 dager mellom onsdag og søndag`() {
        assertEquals(
            3, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 9, 4),
                LocalDate.of(2019, 9, 8)
            )
        )
    }

    @Test
    fun `4 dager mellom onsdag og mandag`() {
        assertEquals(
            4, kalenderdagerMellomMinusHelg(
                LocalDate.of(2019, 9, 4),
                LocalDate.of(2019, 9, 9)
            )
        )
    }

    private fun etSakskompleks(
        sykmelding: Sykmelding? = null,
        søknad: Sykepengesøknad? = null,
        id: UUID = UUID.randomUUID(),
        aktørId: String = "1234567890123"
    ) =
        Sakskompleks(
            id = id,
            aktørId = aktørId,
            sykmeldinger = mutableListOf(),
            inntektsmeldinger = mutableListOf(),
            søknader = mutableListOf()
        ).also {
            sykmelding?.let { sykmelding -> it.leggTil(sykmelding) }
            søknad?.let { søknad -> it.leggTil(søknad) }
        }
}
