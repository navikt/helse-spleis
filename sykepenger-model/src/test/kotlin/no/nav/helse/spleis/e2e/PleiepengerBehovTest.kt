package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class PleiepengerBehovTest : AbstractEndToEndTest() {

    @Test
    fun `Periode for person der det ikke foreligger pleiepengerytelse blir behandlet og sendt til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterYtelser(1.vedtaksperiode, pleiepenger = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, pleiepenger = emptyList())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse blir sendt til Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(1.januar(2020) til 31.januar(2020)))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.TIL_INFOTRYGD
        )
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse i starten av perioden blir sendt til Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(1.desember(2019) til 1.januar(2020)))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.TIL_INFOTRYGD
        )
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse i slutten av perioden blir sendt til Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(31.januar(2020) til 14.februar(2020)))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.TIL_INFOTRYGD
        )
    }

    @Test
    fun `Periode som ikke overlapper med pleiepengerytelse blir behandlet og sendt til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar(2020), 16.januar(2020))))
        val pleiepenger = listOf(1.desember(2019) til 31.desember(2019), 1.februar(2020) til 29.februar(2020))
        håndterYtelser(1.vedtaksperiode, pleiepenger = pleiepenger)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, pleiepenger = pleiepenger)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET
        )
    }
}
