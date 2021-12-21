package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class FrilanserTest : AbstractEndToEndTest() {

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene sendes til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(ORGNUMMER.toString(), (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                            yearMonth = YearMonth.of(2017, it),
                            type = LØNNSINNTEKT,
                            inntekt = INNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    })
                ),
                arbeidsforhold = listOf(
                    Arbeidsforhold(
                        ORGNUMMER.toString(), listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 10),
                                erFrilanser = true
                            )
                        )
                    )
                )
            )
        )
        assertError(1.vedtaksperiode, "Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene")
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Person med frilanserarbeidsforhold uten inntekt i løpet av de siste 3 månedene skal `() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(a1.toString(), (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                            yearMonth = YearMonth.of(2017, it),
                            type = LØNNSINNTEKT,
                            inntekt = INNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    }),
                    ArbeidsgiverInntekt(a2.toString(), (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                            yearMonth = YearMonth.of(2017, it),
                            type = LØNNSINNTEKT,
                            inntekt = INNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    })
                ),
                arbeidsforhold = listOf(
                    Arbeidsforhold(
                        a3.toString(), listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 1),
                                erFrilanser = true
                            )
                        )
                    )
                )
            )
        )
        assertNoErrors(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
    }
}
