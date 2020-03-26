package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SykmeldingHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `Sykmelding skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertTrue(inspektør.personLogg.hasMessages())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `En ny Sykmelding er ugyldig`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `To forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"))
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer2"))
        assertTrue(inspektør.personLogg.hasErrors())
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(6.januar, 10.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertTrue(inspektør.personLogg.hasMessages())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        assertEquals(TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
    }

    @Test
    internal fun `To søknader med overlapp`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder)
        )
}
