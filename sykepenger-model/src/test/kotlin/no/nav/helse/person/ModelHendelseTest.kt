package no.nav.helse.person

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelNySøknadTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class ModelHendelseTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "05015112345"
    }

    private lateinit var person: Person
    private lateinit var inspektør: TestPersonObserver

    @BeforeEach internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
        inspektør = TestPersonObserver().also {
            person.addObserver(it)
        }
    }

    @Test internal fun `NySøknad skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertTrue(inspektør.utløst)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstandType)
    }

    @Test internal fun `En ny NySøknad er ugyldig`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertTrue(inspektør.utløst)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstandType)
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) = ModelNySøknad(
        UUID.randomUUID(),
        ModelNySøknadTest.UNG_PERSON_FNR_2018,
        "12345",
        "987654321",
        LocalDateTime.now(),
        listOf(*sykeperioder)
    )

    private class TestPersonObserver : PersonObserver {
        internal var utløst = false
        internal lateinit var tilstandType: TilstandType
        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            utløst = true
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            tilstandType = event.gjeldendeTilstand
        }
    }
}
