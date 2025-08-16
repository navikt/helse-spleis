package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ForespørselOmArbeidsgiveropplysningerInfotrygdTest : AbstractDslTest() {

    @Test
    fun `sender ikke flagget trengerArbeidsgiveropplysninger i tilstander som har rukket å sende ut egne forespørsler`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }

    @Test
    fun `trenger arbeidsgiveropplysninger -- mer enn 16 dager inkl forkastede perioder`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)

            nyPeriode(5.februar til 8.februar)
            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }

    @Test
    fun `trenger ikke arbeidsgiveropplysninger -- mindre enn 16 dager inkl forkastede perioder`() {
        a1 {
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)

            håndterSøknad(15.januar til 16.januar, sendTilGosys = true)
            assertTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }

    @Test
    fun `trenger ikke arbeidsgiveropplysninger -- stort gap til forkastet periode`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)

            nyPeriode(18.februar til 28.februar)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }

    @Test
    fun `trenger arbeidsgiveropplysninger -- stort gap til forkastet periode, mer enn 16 dager i nåværende periode`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)

            nyPeriode(18.februar til 18.mars)
            assertTrue(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }

    @Test
    fun `trenger arbeidsgiveropplysninger -- mange korte perioder i speil og infotrygd og speil`() {
        a1 {
            nyPeriode(1.januar til 5.januar)

            håndterSøknad(10.januar til 14.januar, sendTilGosys = true)
            assertFalse(observatør.forkastet(2.vedtaksperiode).trengerArbeidsgiveropplysninger)

            håndterSøknad(20.januar til 24.januar)
            assertFalse(observatør.forkastet(3.vedtaksperiode).trengerArbeidsgiveropplysninger)

            håndterSøknad(1.februar til 5.februar)
            assertTrue(observatør.forkastet(4.vedtaksperiode).trengerArbeidsgiveropplysninger)

        }
    }

    @Test
    fun `trenger ikke arbeidsgiveropplysninger -- overlappende forkastede perioder`() {
        a1 {
            håndterSøknad(1.januar til 10.januar, sendTilGosys = true)
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)

            håndterSøknad(1.januar til 10.januar)
            assertTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)

            håndterSøknad(15.januar til 20.januar)
            assertFalse(observatør.forkastet(3.vedtaksperiode).trengerArbeidsgiveropplysninger)

        }
    }
}
