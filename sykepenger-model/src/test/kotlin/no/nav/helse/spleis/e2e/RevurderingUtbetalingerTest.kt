package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Dagtype.Sykedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Vedtaksperiode.Companion.RevurderingUtbetalinger
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Sykdomsgradfilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class RevurderingUtbetalingerTest: AbstractEndToEndTest() {

    @Test
    fun `gjødsle en førstegangsbehandling`() {
        // Gjøres kun for å få en vedtaksperiode i riktig tilstand (AvventerHistorikkRevurdering)
        nyttVedtak(1.januar, 31.januar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        val utbetalinger = RevurderingUtbetalinger(listOf(inspektør.vedtaksperioder(1.vedtaksperiode)), 1.januar, ORGNUMMER, overstyring)
        val tidslinje = tidslinjeOf(31.NAV(INGEN))
        assertEquals(INGEN, tidslinje[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        utbetalinger.gjødsle(mapOf(inspektør.arbeidsgiver to tidslinje), Infotrygdhistorikk())
        assertEquals(INNTEKT, tidslinje[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
    }

    @Test
    fun `gjødsle en forlengelse`() {
        // Gjøres kun for å få en vedtaksperiode i riktig tilstand (AvventerHistorikkRevurdering)
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        val utbetalinger = RevurderingUtbetalinger(
            listOf(
                inspektør.vedtaksperioder(1.vedtaksperiode),
                inspektør.vedtaksperioder(2.vedtaksperiode)
            ), 1.januar, ORGNUMMER, overstyring
        )
        val tidslinje = tidslinjeOf(59.NAV(INGEN))
        assertEquals(INGEN, tidslinje[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(INGEN, tidslinje[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        utbetalinger.gjødsle(mapOf(inspektør.arbeidsgiver to tidslinje), Infotrygdhistorikk())
        assertEquals(INNTEKT, tidslinje[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(INNTEKT, tidslinje[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
    }

    @Test
    fun `gjødsle flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2, inntekt = 20000.månedlig)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        val utbetalinger = RevurderingUtbetalinger(
            listOf(
                inspektør(a1).vedtaksperioder(1.vedtaksperiode),
                inspektør(a1).vedtaksperioder(2.vedtaksperiode),
                inspektør(a2).vedtaksperioder(1.vedtaksperiode),
                inspektør(a2).vedtaksperioder(2.vedtaksperiode)
            ), 1.januar, ORGNUMMER, overstyring
        )
        val tidslinjea1 = tidslinjeOf(59.NAV(INGEN))
        val tidslinjea2 = tidslinjeOf(59.NAV(INGEN))
        assertEquals(INGEN, tidslinjea1[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(INGEN, tidslinjea1[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(INGEN, tidslinjea2[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(INGEN, tidslinjea2[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        utbetalinger.gjødsle(
            mapOf(inspektør(a1).arbeidsgiver to tidslinjea1, inspektør(a2).arbeidsgiver to tidslinjea2),
            Infotrygdhistorikk()
        )
        assertEquals(20000.månedlig, tidslinjea1[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(20000.månedlig, tidslinjea1[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(20000.månedlig, tidslinjea2[18.januar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(20000.månedlig, tidslinjea2[1.februar].økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
    }

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
