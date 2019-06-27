package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.domain.Sakskompleks
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
    }

    @Test
    fun `skal ikke finne sak når søknaden ikke finnes`() {
        val saker = emptyList<Sakskompleks>()

        val dao = SakskompleksDao(saker)
        val sakskompleksService = SakskompleksService(dao)

        val søknad_json = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
        val søknad = Sykepengesøknad(søknad_json)

        val sak = sakskompleksService.finnSak(søknad)

        assertNull(sak)
    }

    @Test
    fun `skal finne sak når søknaden finnes`() {
        val søknad_json = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
        val søknad = Sykepengesøknad(søknad_json)

        val sakForBruker = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "1234567890123",
                sykmeldinger = emptyList(),
                søknader = listOf(søknad)
        )

        val saker = listOf(sakForBruker)

        val dao = SakskompleksDao(saker)
        val sakskompleksService = SakskompleksService(dao)

        val sak = sakskompleksService.finnSak(søknad)

        assertEquals(sakForBruker, sak)
    }
}
