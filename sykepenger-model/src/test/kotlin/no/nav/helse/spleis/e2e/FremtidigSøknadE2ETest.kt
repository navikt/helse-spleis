package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
import no.nav.helse.nesteArbeidsdag

internal class FremtidigSøknadE2ETest : AbstractEndToEndTest() {
    private companion object {
        private val inneværendeMåned = YearMonth.now()
        private val nesteMåned = inneværendeMåned.plusMonths(1)
        private val fom = inneværendeMåned.atDay(14)
        private val tom = nesteMåned.atDay(14).nesteArbeidsdag()
        private val sisteArbeidsgiverdag = fom.plusDays(15)
    }

    @Test
    fun `kan sende inn søknad før periode er gått ut`() {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent))
        håndterInntektsmelding(listOf(Periode(fom, sisteArbeidsgiverdag)), førsteFraværsdag = fom)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                fom.minusYears(1) til fom.minusMonths(1) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertEquals(sisteArbeidsgiverdag.plusDays(1), inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag.førstedato)
        assertEquals(tom, inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag.sistedato)
    }

}
