package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VedtakFattetE2ETest : AbstractEndToEndTest() {

    @Test
    fun `sender vedtak fattet for perioder innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 10.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(0, inspektør.utbetalinger.size)
        assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(0, observatør.utbetaltEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertNull(event.utbetalingId)
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1, inspektør.utbetalinger.size)
        assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(1, observatør.utbetaltEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertEquals(inspektør.utbetaling(0).inspektør.utbetalingId, event.utbetalingId)
        assertEquals(Utbetaling.Utbetalt, inspektør.utbetaling(0).inspektør.tilstand)
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden med bare ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 20.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(17.januar, 20.januar))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1, inspektør.utbetalinger.size)
        assertEquals(1, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(1, observatør.utbetaltEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertEquals(inspektør.utbetaling(0).inspektør.utbetalingId, event.utbetalingId)
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetaling(0).inspektør.tilstand)
    }
}
