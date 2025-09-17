package no.nav.helse.spleis.e2e.søknad

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.lørdag
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.NY
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.OVERFØRT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.UTBETALT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AnnulleringOgUtbetalingTest : AbstractDslTest() {

    @Test
    fun `annullere revurdering til godkjenning etter annen revurdering`() = a1 {
        nyttVedtak(januar)
        nyttVedtak(mars)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)

        assertEquals(4, inspektør.antallUtbetalinger)
        val marsutbetalingen = inspektør.utbetaling(1)
        håndterAnnullering(marsutbetalingen.utbetalingId)
        håndterUtbetalt()

        assertEquals(5, inspektør.antallUtbetalinger)
        val annulleringen = inspektør.utbetaling(4)
        assertTrue(annulleringen.erAnnullering)
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    @Test
    fun `korrigerer periode med bare ferie`() = a1 {
        nyttVedtak(3.januar til 26.januar)
        nyttVedtak(3.mars til 26.mars)
        nullstillTilstandsendringer()

        håndterSøknad(Sykdom(3.mars, 26.mars, 100.prosent), Ferie(3.mars, 26.mars))
        håndterYtelser(2.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(3, inspektør.antallUtbetalinger)
        val januarutbetaling = inspektør.utbetaling(0)
        val marsutbetaling = inspektør.utbetaling(1)
        val revurderingAvMars = inspektør.utbetaling(2)

        assertEquals(9, observatør.utbetaltEndretEventer.size)
        assertUtbetalingtilstander(januarutbetaling.utbetalingId, NY, IKKE_UTBETALT, OVERFØRT, UTBETALT)
        assertUtbetalingtilstander(marsutbetaling.utbetalingId, NY, IKKE_UTBETALT, OVERFØRT, UTBETALT)
        assertUtbetalingtilstander(revurderingAvMars.utbetalingId, NY, IKKE_UTBETALT, OVERFØRT, UTBETALT)

        assertEquals(revurderingAvMars.korrelasjonsId, marsutbetaling.korrelasjonsId)
        assertNotEquals(januarutbetaling.korrelasjonsId, marsutbetaling.korrelasjonsId)

        assertEquals(UTBETALT, revurderingAvMars.tilstand)

        assertEquals(1, revurderingAvMars.arbeidsgiverOppdrag.size)
        assertEquals(0, revurderingAvMars.personOppdrag.size)
        revurderingAvMars.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(19.mars, linje.fom)
            assertEquals(26.mars, linje.tom)
            assertEquals(19.mars, linje.datoStatusFom)
            assertEquals("OPPH", linje.statuskode)
        }

        assertEquals(Endringskode.ENDR, revurderingAvMars.arbeidsgiverOppdrag.inspektør.endringskode)
    }

    @Test
    fun `arbeidsgiverperiode slås sammen pga avviklet ferie - med endring uten utbetalingsendring i forrige periode - ny eldre periode mens til utbetaling`() {
        a1 {
            nyttVedtak(1.juni til lørdag.den(30.juni))
            nyttVedtak(august)
            håndterOverstyrTidslinje(lørdag.den(30.juni).til(30.juli).map {
                ManuellOverskrivingDag(it, Dagtype.Feriedag)
            }.plusElement(ManuellOverskrivingDag(31.juli, Dagtype.Arbeidsdag)))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertEquals(2, observatør.avsluttetMedVedtakEventer.getValue(1.vedtaksperiode).size)
            assertEquals(1, observatør.avsluttetMedVedtakEventer.getValue(2.vedtaksperiode).size)

            håndterSøknad(Sykdom(1.april, 16.april, 100.prosent))

            assertEquals(2, observatør.avsluttetMedVedtakEventer.getValue(1.vedtaksperiode).size)
            assertEquals(1, observatør.avsluttetMedVedtakEventer.getValue(2.vedtaksperiode).size)

            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(3, observatør.avsluttetMedVedtakEventer.getValue(1.vedtaksperiode).size)
            assertEquals(2, observatør.avsluttetMedVedtakEventer.getValue(2.vedtaksperiode).size)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    private fun assertUtbetalingtilstander(utbetalingId: UUID, vararg status: Utbetalingstatus) {
        observatør.utbetaltEndretEventer
            .filter { it.utbetalingId == utbetalingId }
            .also { events ->
                assertEquals(status.map(Utbetalingstatus::name).toList(), events.map { it.forrigeStatus }.plus(events.last().gjeldendeStatus))
            }
    }
}
