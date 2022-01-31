package no.nav.helse.serde.api

import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.spleis.e2e.*
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SpeilBuilderFlereAGTest : AbstractEndToEndTest() {
    @BeforeEach
    fun beforeAllTests() {
        Toggle.SpeilApiV2.enable()
    }

    @AfterEach
    fun afterAllTests() {
        Toggle.SpeilApiV2.pop()
    }

    @Test
    fun `sender med ghost tidslinjer til speil`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder)
        assertEquals(
            listOf(
                GhostPeriodeDTO(
                    fom = 1.januar,
                    tom = 20.januar,
                    skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                    vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk()
                )
            ),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        )
    }

    @Test
    fun `sender med ghost tidslinjer til speil med flere arbeidsgivere ulik fom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(4.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(4.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(4.januar til 19.januar), orgnummer = a2, beregnetInntekt = 5000.månedlig)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                    a3 inntekt 10000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                    a3 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a3, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson1 = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder)
        assertEquals(
            listOf(
                GhostPeriodeDTO(
                    fom = 1.januar,
                    tom = 31.januar,
                    skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                    vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk()
                )
            ),
            speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder
        )
    }

    @Test
    fun `lager ikke ghosts for forkastede perioder med vilkårsgrunnlag fra spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder)
        assertEquals(
            listOf(
                GhostPeriodeDTO(
                    1.februar,
                    20.februar,
                    inspektør.skjæringstidspunkt(1.vedtaksperiode),
                    vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk()
                )
            ),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        )
    }

    @Test
    fun `skal ikke lage ghosts for gamle arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2017), 20.januar(2017), 100.prosent), orgnummer = a3)
        håndterSøknad(Sykdom(1.januar(2017), 20.januar(2017), 100.prosent), orgnummer = a3)
        håndterInntektsmelding(listOf(1.januar(2017) til 16.januar(2017)), orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = true, orgnummer = a3)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder)
        assertEquals(
            listOf(
                GhostPeriodeDTO(
                    1.januar,
                    20.januar,
                    inspektør.skjæringstidspunkt(1.vedtaksperiode),
                    vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk()
                )
            ),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        )
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder)
    }

    @Test
    fun `ghost periode kuttes ved skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val speilJson = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder)
        assertEquals(
            listOf(GhostPeriodeDTO(3.januar, 31.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode), person.nyesteIdForVilkårsgrunnlagHistorikk())),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        )
    }
}
