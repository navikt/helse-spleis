package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ForkastForlengelseAvForkastetPeriodeTest : AbstractEndToEndTest() {

    @Test
    fun `forlenger tidligere overlappende sykmelding`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 10.januar).forkast()
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertTrue(observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(3.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `søknader som overlapper uten toggle`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 30.januar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 30.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(31.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(31.januar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        håndterSøknad(Sykdom(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        assertEquals(4, inspektør.vedtaksperiodeTeller)
        assertTrue(observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(3.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `søknader som overlapper med toggle`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 30.januar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 30.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(31.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(31.januar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        håndterSøknad(Sykdom(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        assertEquals(4, inspektør.vedtaksperiodeTeller)
        assertTrue(observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(3.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertTrue(observatør.hendelseider(4.vedtaksperiode.id(ORGNUMMER)).isNotEmpty())
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, START, TIL_INFOTRYGD)
        assertError("Overlappende søknad starter før, eller slutter etter, opprinnelig periode", 1.vedtaksperiode.filter())
    }

    @Test
    fun `ny periode etter forkastede`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 10.januar).forkast()
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertInfo("Sykmelding forlenger forkastet vedtaksperiode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertInfo("Sykmelding forlenger forkastet vedtaksperiode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `forkaster ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertNoErrors(2.vedtaksperiode.filter())
    }

    @Test
    fun `logger ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertNoInfo("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `fortsetter å forkaste gjentatte forlengelser av kastede perioder`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 3.vedtaksperiode.filter())
        håndterSykmelding(Sykmeldingsperiode(2.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(2.mars, 31.mars, 100.prosent))
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertNoErrors(4.vedtaksperiode.filter())
    }

    private fun Periode.forkast() {
        håndterSykmelding(Sykmeldingsperiode(start, endInclusive, 100.prosent))
        håndterSøknad(Sykdom(start, endInclusive, 100.prosent))
        person.søppelbøtte(hendelselogg, this)
    }
}
