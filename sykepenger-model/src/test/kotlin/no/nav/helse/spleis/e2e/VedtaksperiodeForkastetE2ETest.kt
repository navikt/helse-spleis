package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetE2ETest : AbstractDslTest() {

    @Test
    fun `vedtaksperioder forkastes`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            nyPeriode(april)
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterUtbetalingsgodkjenning(4.vedtaksperiode, false) // <- TIL_INFOTRYGD
            assertEquals(1, observatør.forkastedePerioder())
            assertEquals(AVVENTER_GODKJENNING, observatør.forkastet(4.vedtaksperiode).gjeldendeTilstand)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(4.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `forkaster kort periode`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 15.januar)
            håndterSøknad(16.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            håndterAnnullering(3.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(1, observatør.forkastedePerioder())
            assertEquals(TIL_ANNULLERING, observatør.forkastet(3.vedtaksperiode).gjeldendeTilstand)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger for søknad som forkastes pga sendTilGosys = true`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar, sendTilGosys = true)

            assertTrue(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(januar), observatør.forkastet(1.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse av spleis`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar, sendTilGosys = true)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse av spleis over helg`() {
        a1 {
            nyttVedtak(1.januar til 26.januar)
            håndterSykmelding(Sykmeldingsperiode(29.januar, 28.februar))
            håndterSøknad(29.januar til 28.februar, sendTilGosys = true)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse forkastet periode`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar, sendTilGosys = true)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse forkastet periode over helg`() {
        a1 {
            tilGodkjenning(1.januar til 26.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(29.januar, 28.februar))
            håndterSøknad(29.januar til 28.februar, sendTilGosys = true)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger ved kort gap til spleis`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar))
            håndterSøknad(2.februar til 28.februar, sendTilGosys = true)

            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(januar, 2.februar til 28.februar), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger ved kort gap til forkastet periode`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar))
            håndterSøknad(2.februar til 28.februar)

            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(januar, 2.februar til 28.februar), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forkasting av AUU`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            val event = observatør.forkastet(1.vedtaksperiode)
            assertFalse(event.trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(1.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse av kort spleisperiode, ny periode er fortsatt innenfor AGP`() {
        a1 {
            nyPeriode(1.januar til 10.januar)

            håndterSykmelding(Sykmeldingsperiode(11.januar, 15.januar))
            håndterSøknad(11.januar til 15.januar, sendTilGosys = true)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger ved forlengelse av kort spleisperiode, ny periode går utover AGP`() {
        a1 {
            nyPeriode(1.januar til 10.januar)

            håndterSykmelding(Sykmeldingsperiode(11.januar, 17.januar))
            håndterSøknad(11.januar til 17.januar, sendTilGosys = true)

            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(1.januar til 10.januar, 11.januar til 17.januar), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger ved forlengelse av kort forkastet periode, ny periode er fortsatt innenfor AGP`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(11.januar, 15.januar))
            håndterSøknad(11.januar til 15.januar)

            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger ved forlengelse av kort forkastet periode, ny periode går utover AGP`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar))
            håndterSøknad(11.januar til 31.januar)

            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(1.januar til 10.januar, 11.januar til 31.januar), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger ved forlengelse av kort forkastet periode, ny periode går utover AGP, men er kortere enn 16 dager`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(11.januar, 17.januar))
            håndterSøknad(11.januar til 17.januar)

            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(1.januar til 10.januar, 11.januar til 17.januar), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeisgiveropplysninger ved kort periode med gap til kort forkastet periode, ny periode går ikke utover AGP`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSøknad(1.januar til 5.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(10.januar, 15.januar))
            håndterSøknad(10.januar til 15.januar)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeisgiveropplysninger ved kort periode med gap til kort forkastet periode, ny periode går utover AGP`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.desember(2017), 10.desember(2017)))
            håndterSøknad(1.desember(2017) til 10.desember(2017), sendTilGosys = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(15.januar, 25.januar))
            håndterSøknad(15.januar til 25.januar)
            assertTrue(observatør.forkastet(3.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(1.januar til 10.januar, 15.januar til 25.januar), observatør.forkastet(3.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer arbeidsgiveropplysninger fra kort periode når den går utover AGP pga kort periode behandlet i spleis og kort forkastet periode`() {
        a1 {
            nyPeriode(1.januar til 5.januar)

            håndterSykmelding(Sykmeldingsperiode(10.januar, 15.januar))
            håndterSøknad(10.januar til 15.januar, sendTilGosys = true)
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)

            håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar))
            håndterSøknad(20.januar til 31.januar)
            assertTrue(observatør.forkastet(3.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(listOf(1.januar til 5.januar, 10.januar til 15.januar, 20.januar til 31.januar), observatør.forkastet(3.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger fra periode der arbeidsgiver har sendt inntektsmelding før vi mottar søknad`() {
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            nyPeriode(januar)
            håndterAnmodningOmForkasting(1.vedtaksperiode, force = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(1.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Forventer ikke arbeidsgiveropplysninger fra periode med utbetaling som mottar overlappende søknad`() {
        a1 {
            nyPeriode(januar)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            nyPeriode(31.januar til 31.januar)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertEquals(emptyList<Periode>(), observatør.forkastet(2.vedtaksperiode).sykmeldingsperioder)
        }
    }

    @Test
    fun `Sender ikke med senere sykmeldingsperioder enn vedtaksperioden som forkastes`() {
        a1 {
            håndterSøknad(januar, sendTilGosys = true)
            nyPeriode(februar)
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            assertEquals(listOf(januar), observatør.forkastet(1.vedtaksperiode).sykmeldingsperioder)
            assertTrue(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }
}
