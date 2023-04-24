package no.nav.helse.spleis.e2e

import java.time.YearMonth
import no.nav.helse.desember
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class FrilanserTest : AbstractEndToEndTest() {

    @Test
    fun `frilansarbeidsforhold med feriepenger siste tre måneder før skjæringstidspunktet`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                },
                arbeidsforhold = listOf(
                    Arbeidsforhold(a2, listOf(
                        InntektForSykepengegrunnlag.MånedligArbeidsforhold(november(2017), true)
                    ))
                )
            ),
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.november(2017).somPeriode() feriepenger {
                    a2 inntekt 6000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertIngenFunksjonelleFeil()
        assertVarsel(Varselkode.RV_IV_1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

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
            AVVENTER_INNTEKTSMELDING,
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
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK
        )
    }
}
