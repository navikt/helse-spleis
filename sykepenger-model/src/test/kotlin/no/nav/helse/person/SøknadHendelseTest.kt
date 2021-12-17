package no.nav.helse.person

import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Egenmelding
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SøknadHendelseTest : AbstractPersonTest() {

    @Test
    fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 50.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `søknad kan ikke utvide sykdomstidslinje frem i tid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100.prosent), Egenmelding(9.januar, 10.januar)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(5, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `søknad kan ikke utvide sykdomstidslinje tilbake i tid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknad(Egenmelding(28.desember(2017), 29.desember(2017)), Sykdom(1.januar,  5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(5, inspektør.sykdomstidslinje.count()) { inspektør.sykdomstidslinje.toString() }
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(6.januar,  10.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(1.januar,  5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(10, inspektør.sykdomstidslinje.count())
    }

    @Test
    fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(4.januar, 10.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    private fun søknad(vararg perioder: Søknadsperiode, orgnummer: String = "987654321") =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = emptyList(),
            sendtTilNAV = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        )

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
        )
}
