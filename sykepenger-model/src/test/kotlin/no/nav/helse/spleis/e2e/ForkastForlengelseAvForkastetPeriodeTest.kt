package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ForkastForlengelseAvForkastetPeriodeTest : AbstractEndToEndTest() {

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertError(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertInfo(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
    }

    @Test
    fun `forkaster periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertError(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
    }

    @Test
    fun `logger periode som er forlengelse av forkastet periode over helg`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertInfo(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
    }

    @Test
    fun `forkaster ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoErrors(2.vedtaksperiode)
    }

    @Test
    fun `logger ikke periode som har gap til forkastet periode`() = Toggle.ForkastForlengelseAvForkastetPeriode.disable {
        (1.januar til 19.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoInfo(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
    }

    @Test
    fun `fortsetter å forkaste gjentatte forlengelser av kastede perioder`() = Toggle.ForkastForlengelseAvForkastetPeriode.enable {
        (1.januar til 15.januar).forkast()
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertError(2.vedtaksperiode, "Perioden forlenger en forkastet periode")
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertError(3.vedtaksperiode, "Perioden forlenger en forkastet periode")
        håndterSykmelding(Sykmeldingsperiode(2.mars, 31.mars, 100.prosent))
        assertSisteTilstand(4.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)
        assertNoErrors(4.vedtaksperiode)
    }

    private fun Periode.forkast() {
        håndterSykmelding(Sykmeldingsperiode(start, endInclusive, 100.prosent))
        person.søppelbøtte(hendelselogg, this)
    }
}
