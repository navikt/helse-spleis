package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykmeldingHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `Sykmelding skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertTrue(inspektør.personLogg.hasActivities())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    fun `En ny Sykmelding er ugyldig`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarningsOrWorse())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertTrue(inspektør.personLogg.hasActivities())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        assertEquals(TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
    }

    @Test
    fun `To søknader med overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarningsOrWorse())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
        )
}
