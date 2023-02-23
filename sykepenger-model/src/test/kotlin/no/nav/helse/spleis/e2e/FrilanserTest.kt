package no.nav.helse.spleis.e2e

import java.time.YearMonth
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class FrilanserTest : AbstractEndToEndTest() {

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene sendes til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(ORGNUMMER, (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt(
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
                        ORGNUMMER, listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 10),
                                erFrilanser = true
                            )
                        )
                    )
                )
            )
        )
        assertFunksjonellFeil("Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene", 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Person med frilanserarbeidsforhold uten inntekt i løpet av de siste 3 månedene skal `() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(a1, (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt(
                            yearMonth = YearMonth.of(2017, it),
                            type = LØNNSINNTEKT,
                            inntekt = INNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    }),
                    ArbeidsgiverInntekt(a2, (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt(
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
                        a3, listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 1),
                                erFrilanser = true
                            )
                        )
                    )
                )
            )
        )
        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
    }
}
