package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Dagtype.Sykedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.januar
import no.nav.helse.person.Vedtaksperiode.Companion.RevurderingUtbetalinger
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Sykdomsgradfilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingUtbetalingerTest: AbstractEndToEndTest() {

    @Test
    fun `filter på en førstegangsbehandling`() {
        // Gjøres kun for å få en vedtaksperiode i riktig tilstand (AvventerHistorikkRevurdering)
        nyttVedtak(1.januar, 31.januar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 10)))

        val utbetalinger = RevurderingUtbetalinger(listOf(inspektør.vedtaksperioder(1.vedtaksperiode)), 1.januar, ORGNUMMER, overstyring)
        val tidslinje = tidslinjeOf(16.AP, 1.NAV(INGEN, 10.0), 14.NAV(INGEN))

        assertFalse(tidslinje[17.januar] is AvvistDag)
        utbetalinger.filtrer(listOf(Sykdomsgradfilter), mapOf(inspektør.arbeidsgiver to tidslinje))
        assertTrue(tidslinje[17.januar] is AvvistDag)
    }


    @Test
    fun `Ved revurdering av forlengelse blir den siste vedtaksperiodens gjenstående sykedager plassert på alle vedtaksperiodene som revurderes`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        assertEquals(237, inspektør.gjenståendeSykedager(1.vedtaksperiode))
        assertEquals(217, inspektør.gjenståendeSykedager(2.vedtaksperiode))

        Assertions.assertNotEquals(
            inspektør.gjenståendeSykedager(1.vedtaksperiode),
            inspektør.gjenståendeSykedager(2.vedtaksperiode)
        )
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(4.januar, Feriedag)))

        håndterYtelser(2.vedtaksperiode)
        assertForventetFeil(
            forklaring = "ved revurdering av forlengelser kjører vi maksimum sykepengefilteret for siste vedtaksperiode " +
                    "over alle sammenhengende perioder og gjenstående sykedager på de tidligere vedtaksperiodene blir feil," +
                    "bør se på hvordan vi beregner utbetalingstidslinjer i revurderingsløpet, " +
                    "kanskje maksimumsykepengefilter må kjøres per vedtaksperiode",
            nå = {
                assertEquals(217, inspektør.gjenståendeSykedager(1.vedtaksperiode))
                assertEquals(217, inspektør.gjenståendeSykedager(2.vedtaksperiode))
            },
            ønsket = {
                assertEquals(237, inspektør.gjenståendeSykedager(1.vedtaksperiode))
                assertEquals(217, inspektør.gjenståendeSykedager(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `filter på en forlengelse`() {
        // Gjøres kun for å få en vedtaksperiode i riktig tilstand (AvventerHistorikkRevurdering)
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        val overstyring = håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(17.januar, Sykedag, 10),
                ManuellOverskrivingDag(1.februar, Sykedag, 10)
            )
        )

        val utbetalinger = RevurderingUtbetalinger(
            listOf(
                inspektør.vedtaksperioder(1.vedtaksperiode),
                inspektør.vedtaksperioder(2.vedtaksperiode)
            ), 1.januar, ORGNUMMER, overstyring
        )
        val tidslinje = tidslinjeOf(16.AP, 1.NAV(INGEN, 10.0), 14.NAV(INGEN), 1.NAV(INGEN, 10.0), 27.NAV(INGEN))

        assertFalse(tidslinje[17.januar] is AvvistDag)
        assertFalse(tidslinje[1.februar] is AvvistDag)
        utbetalinger.filtrer(listOf(Sykdomsgradfilter), mapOf(inspektør.arbeidsgiver to tidslinje))
        assertTrue(tidslinje[17.januar] is AvvistDag)
        assertTrue(tidslinje[1.februar] is AvvistDag)
    }

    @Test
    fun `filter på flere ag`() {
        nyeVedtak(1.januar, 31.januar, a1, a2, inntekt = 20000.månedlig)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        val overstyring = håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(17.januar, Sykedag, 10),
                ManuellOverskrivingDag(1.februar, Sykedag, 10)
            ),
            a1
        )

        val utbetalinger = RevurderingUtbetalinger(
            listOf(
                inspektør(a1).vedtaksperioder(1.vedtaksperiode),
                inspektør(a1).vedtaksperioder(2.vedtaksperiode),
                inspektør(a2).vedtaksperioder(1.vedtaksperiode),
                inspektør(a2).vedtaksperioder(2.vedtaksperiode)
            ), 1.januar, ORGNUMMER, overstyring
        )
        val tidslinjea1 = tidslinjeOf(16.AP, 1.NAV(INGEN, 10.0), 14.NAV(INGEN), 1.NAV(INGEN, 10.0), 27.NAV(INGEN))
        val tidslinjea2 = tidslinjeOf(16.AP, 1.NAV(INGEN, 10.0), 14.NAV(INGEN), 1.NAV(INGEN, 10.0), 27.NAV(INGEN))

        assertFalse(tidslinjea1[17.januar] is AvvistDag)
        assertFalse(tidslinjea1[1.februar] is AvvistDag)
        assertFalse(tidslinjea2[17.januar] is AvvistDag)
        assertFalse(tidslinjea2[1.februar] is AvvistDag)
        utbetalinger.filtrer(listOf(Sykdomsgradfilter), mapOf(inspektør(a1).arbeidsgiver to tidslinjea1, inspektør(a2).arbeidsgiver to tidslinjea2))
        assertTrue(tidslinjea1[17.januar] is AvvistDag)
        assertTrue(tidslinjea1[1.februar] is AvvistDag)
        assertTrue(tidslinjea2[17.januar] is AvvistDag)
        assertTrue(tidslinjea2[1.februar] is AvvistDag)
    }

}
