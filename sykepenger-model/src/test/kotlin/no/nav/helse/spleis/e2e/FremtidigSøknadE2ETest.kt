package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class FremtidigSøknadE2ETest : AbstractEndToEndTest() {
    private companion object {
        private val inneværendeMåned = YearMonth.now()
        private val nesteMåned = inneværendeMåned.plusMonths(1)
        private val fom = inneværendeMåned.atDay(14)
        private val tom = nesteMåned.atDay(14)
        private val sisteArbeidsgiverdag = fom.plusDays(15)
    }

    @Test
    fun `kan sende inn søknad før periode er gått ut`() {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent))
        håndterInntektsmelding(listOf(Periode(fom, sisteArbeidsgiverdag)), førsteFraværsdag = fom)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode) // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertEquals(sisteArbeidsgiverdag.plusDays(1), inspektør.utbetalinger.first().arbeidsgiverOppdrag().førstedato)
        assertEquals(tom, inspektør.utbetalinger.first().arbeidsgiverOppdrag().sistedato)
    }

}
