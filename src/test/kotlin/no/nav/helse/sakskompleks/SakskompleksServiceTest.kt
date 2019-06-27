package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

class SakskompleksServiceTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
    }

    @Test
    fun `skal ikke finne sak når søknaden ikke finnes`() {
        val sakskompleksService = SakskompleksService(SakskompleksDao(emptyList()))

        val sak = sakskompleksService.finnSak(testSøknad)

        assertNull(sak)
    }

    @Test
    fun `skal finne sak når søknaden finnes`() {
        val sakForBruker = etSakskompleks(
                søknader = listOf(testSøknad)
        )

        val sakskompleksService = SakskompleksService(SakskompleksDao(listOf(sakForBruker)))

        val sak = sakskompleksService.finnSak(testSøknad)

        assertEquals(sakForBruker, sak)
    }

    private fun etSakskompleks(id: UUID = UUID.randomUUID(),
                               aktørId: String = "1234567890123",
                               sykmeldinger: List<Sykmelding> = emptyList(),
                               søknader: List<Sykepengesøknad> = emptyList()) =
            Sakskompleks(
                    id = id,
                    aktørId = aktørId,
                    sykmeldinger = sykmeldinger,
                    søknader = søknader
            )
}
