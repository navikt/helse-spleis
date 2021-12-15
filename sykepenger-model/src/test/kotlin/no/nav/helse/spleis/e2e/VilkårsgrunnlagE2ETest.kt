package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagE2ETest : AbstractEndToEndTest() {

    @Test
    fun `gjør ikke vilkårsprøving om vi ikke har inntekt fra inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober(2020), 8.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.oktober(2020), 8.november(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 22.november(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(9.november(2020), 22.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.oktober(2021), 8.november(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.oktober(2021), 8.november(2021), 100.prosent))

        håndterYtelser(2.vedtaksperiode)

        assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertError(2.vedtaksperiode, "Forventer minst ett sykepengegrunnlag som er fra inntektsmelding eller Infotrygd")
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `mer enn 25% avvik lager kun én errormelding i aktivitetsloggen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 31.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT/2
                }
            }
        ))

        assertEquals(listOf("Har mer enn 25 % avvik"), collectErrors(1.vedtaksperiode, ORGNUMMER))
    }

    @Test
    fun `ingen sammenligningsgrunlag fører til error om 25% avvik`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 31.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(emptyList()))

        assertEquals(listOf("Har mer enn 25 % avvik"), collectErrors(1.vedtaksperiode, ORGNUMMER))
    }

    @Test
    fun `Forkaster etterfølgende perioder dersom vilkårsprøving feilet pga avvik i inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 17.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar(2021), 17.januar(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.januar(2021), 20.januar(2021), 100.prosent))

        val arbeidsgiverperioder = listOf(
            1.januar(2021) til 16.januar(2021)
        )
        val inntektsmeldingId = håndterInntektsmelding(
            arbeidsgiverperioder, førsteFraværsdag = 1.januar(2021)
        )

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2
                }
            }
        ))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forkaster ikke etterfølgende perioder dersom vilkårsprøving feiler pga minimum inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 17.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar(2021), 17.januar(2021), 100.prosent))

        val arbeidsgiverperioder = listOf(
            1.januar(2021) til 16.januar(2021)
        )

        håndterInntektsmelding(
            arbeidsgiverperioder = arbeidsgiverperioder,
            førsteFraværsdag = 1.januar(2021),
            beregnetInntekt = 1000.månedlig,
            refusjon = Inntektsmelding.Refusjon(1000.månedlig, null, emptyList())
        )

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt 1000
                }
            }
        ))

        håndterSykmelding(Sykmeldingsperiode(18.januar(2021), 20.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(18.januar(2021), 20.januar(2021), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
        )

        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `25 % avvik i inntekt lager error`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 17.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar(2021), 17.januar(2021), 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2 // 25 % avvik vs inntekt i inntektsmeldingen
                }
            }
        ))

        assertErrorTekst(inspektør, "Har mer enn 25 % avvik")
    }
}
