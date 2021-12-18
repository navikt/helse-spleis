package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Sykdom
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FlereArbeidsgivereWarningsTest : AbstractEndToEndTest() {

    @Test
    fun `En arbeidsgiver med inntekter fra flere arbeidsgivere de siste tre månedene - får ikke warning om at det er flere arbeidsgivere, får warning om at det er flere inntektskilder enn arbeidsforhold`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 1000.månedlig
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(a1.inspektør, "Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret.")
        assertFalse(
            a1.inspektør.personLogg.toString().contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold")
        )
    }

    @Test
    fun `Første arbeidsgiver blir ferdig behandlet før vi mottar sykmelding på neste arbeidsgiver`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                )
            ),
            arbeidsforhold = listOf(Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null), Arbeidsforhold(a2.toString(), LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )

        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte."
        )
    }

    @Test
    fun `Første arbeidsgiver har blitt sendt til godkjenning før vi mottar sykmelding på neste arbeidsgiver`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                )
            ),
            arbeidsforhold = listOf(Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null), Arbeidsforhold(a2.toString(), LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )

        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte."
        )
    }
}
