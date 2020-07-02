package no.nav.helse.person

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Søknadsperiode
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SøknadArbeidsgiverHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør.person(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 50)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `mangler Sykmelding`() {
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `andre søknad ugyldig`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `ignorer andre sendt søknad`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
    }


    @Test
    internal fun `ignorer andre søknad til arbeidsgiver`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertEquals(5, inspektør.sykdomstidslinje.count())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `To søknader med opphold`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        assertEquals(5, inspektør.sykdomstidslinje.count())
        person.håndter(sykmelding(Sykmeldingsperiode(15.januar, 19.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(15.januar, 19.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1))
        assertEquals(19, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `forlengelse etter avsluttet periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `gjenopptar første periode etter avslutting av avsluttet periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        assertEquals(MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
        assertEquals(MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(1))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `avslutter andre periode før første periode behandles`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(0))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(4.januar, 10.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(0))
    }

    @Test
    fun `to forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100), orgnummer = "orgnummer1"))
        person.håndter(
            søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100), orgnummer = "orgnummer2")
        )
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    private fun søknad(vararg perioder: Søknad.Søknadsperiode, orgnummer: String = "987654321") =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false
        )

    private fun søknadArbeidsgiver(
        vararg perioder: Søknadsperiode,
        orgnummer: String = "987654321"
    ) =
        SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder)
        )

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map{ it.fom }.min()?.plusMonths(3)?.atStartOfDay() ?: LocalDateTime.now()
        )
}
