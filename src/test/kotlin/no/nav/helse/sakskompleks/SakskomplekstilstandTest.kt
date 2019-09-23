package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.bytebuddy.dynamic.DynamicType
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.helse.søknad.domain.SykepengesøknadTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class SakskomplekstilstandTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `tester at sykmeldingMottatt er første tilstand`() {
        val etSakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789"
        )

        assertTrue(etSakskompleks.tilstand is Sakskompleks.SykmeldingMottattTilstand)
    }

    @Test
    fun `tester at vi kan motta en søknad når vi har en sykmelding`() {
        val etSakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789"
        )

        val enSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))

        etSakskompleks.leggerTil(enSøknad)

        assertTrue(etSakskompleks.tilstand is Sakskompleks.SøknadMottattTilstand)
    }
}
