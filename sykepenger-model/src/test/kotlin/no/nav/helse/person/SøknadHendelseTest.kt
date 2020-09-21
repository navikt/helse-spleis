package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SøknadHendelseTest {

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
    internal fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 50)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `mangler Sykmelding`() {
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `søknad kan ikke utvide sykdomstidslinje frem i tid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100), Egenmelding(9.januar, 10.januar)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `søknad kan ikke utvide sykdomstidslinje tilbake i tid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Egenmelding(28.desember(2017), 29.desember(2017)), Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje.count()) { inspektør.sykdomstidslinje.toString() }
    }

    @Test
    fun `søknad med utdanning avvist`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100), Utdanning(4.januar, 5.januar)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `andre søknad ugyldig`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknad(Sykdom(6.januar,  10.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
        assertEquals(10, inspektør.sykdomstidslinje.length())
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(4.januar, 10.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
    }

    private fun søknad(vararg perioder: Søknadsperiode, orgnummer: String = "987654321") =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false
        )

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
