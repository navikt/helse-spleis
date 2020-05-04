package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.NyTestPersonInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class NySøknadHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = NyTestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).count())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
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
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100), Egenmelding(9.januar, 10.januar)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).count())
    }

    @Test
    internal fun `søknad kan utvide sykdomstidslinje tilbake i tid`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Egenmelding(28.desember(2017), 29.desember(2017)), Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(9, inspektør.sykdomstidslinje(0).count()) { inspektør.sykdomstidslinje(0).toString() }
    }

    @Test
    internal fun `søknad med utdanning avvist`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100), Utdanning(4.januar, 5.januar)))
        assertTrue(inspektør.personLogg.hasBehov())
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `andre søknad ugyldig`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(6.januar, 10.januar, 100)))
        person.håndter(søknad(Sykdom(6.januar,  10.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).count())
        assertEquals(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100)))
        person.håndter(sykmelding(Triple(4.januar, 10.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `to forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"))
        person.håndter(
                søknad(Sykdom(1.januar,  5.januar, 100), orgnummer = "orgnummer2")
            )
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(0))
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

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder)
        )
}
