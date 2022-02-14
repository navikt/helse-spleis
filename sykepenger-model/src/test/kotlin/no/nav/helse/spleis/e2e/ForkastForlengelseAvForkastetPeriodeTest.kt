package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForkastForlengelseAvForkastetPeriodeTest : AbstractEndToEndTest() {

    @Test
    fun `forlenger tidligere overlappende sykmelding`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 10.januar).forkast()
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `sykmeldinger som overlapper uten toggle`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 30.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(31.januar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        assertEquals(4, inspektør.vedtaksperiodeTeller)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `sykmeldinger som overlapper med toggle`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 30.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(31.januar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent)) // overlapper med vedtaksperiode 1 og vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent)) // overlapper med vedtaksperiode 2
        håndterSykmelding(Sykmeldingsperiode(19.februar, 8.mars, 100.prosent)) // forlenger overlappende
        assertEquals(4, inspektør.vedtaksperiodeTeller)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, START, TIL_INFOTRYGD)
        assertError("Mottatt overlappende sykmeldinger - slutter etter vedtaksperioden, starter inni", 1.vedtaksperiode.filter())
    }

    @Test
    fun `ny periode etter forkastede`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 10.januar).forkast()
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertInfo("Sykmelding forlenger forkastet vedtaksperiode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertInfo("Sykmelding forlenger forkastet vedtaksperiode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `forkaster ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoErrors(2.vedtaksperiode.filter())
    }

    @Test
    fun `logger ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoInfo("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
    }

    @Test
    fun `fortsetter å forkaste gjentatte forlengelser av kastede perioder`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 2.vedtaksperiode.filter())
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
        assertError("Sykmelding forlenger en forkastet periode", 3.vedtaksperiode.filter())
        håndterSykmelding(Sykmeldingsperiode(2.mars, 31.mars, 100.prosent))
        assertSisteTilstand(4.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoErrors(4.vedtaksperiode.filter())
    }

    private fun Periode.forkast() {
        håndterSykmelding(Sykmeldingsperiode(start, endInclusive, 100.prosent))
        person.søppelbøtte(hendelselogg, this)
    }
}
