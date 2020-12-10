package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Søknadsperiode
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SøknadArbeidsgiverHendelseTest : AbstractPersonTest() {

    @Test
    fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 50.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `mangler Sykmelding`() {
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `andre søknad ignoreres`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `logger error ved overlappende søknad`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100.prosent, 0.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode)) // bytter ikke tilstand pga den er avsluttet - men det burde den kanskje?
    }

    @Test
    fun `søknad til arbeidsgiver som overlapper fører til utkasting`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100.prosent, 0.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        assertEquals(5, inspektør.sykdomstidslinje.count())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `To søknader med opphold`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        assertEquals(5, inspektør.sykdomstidslinje.count())
        person.håndter(sykmelding(Sykmeldingsperiode(15.januar, 19.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(15.januar, 19.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(19, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `forlengelse etter avsluttet periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `gjenopptar første periode etter avslutting av avsluttet periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        assertEquals(MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `avslutter andre periode før første periode behandles`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 5.januar, 100.prosent, 0.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknadArbeidsgiver(Søknadsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(4.januar, 10.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVSLUTTET_UTEN_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
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
            sykeperioder = sykeperioder.toList(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.plusMonths(3)?.atStartOfDay() ?: LocalDateTime.now()
        )
}
