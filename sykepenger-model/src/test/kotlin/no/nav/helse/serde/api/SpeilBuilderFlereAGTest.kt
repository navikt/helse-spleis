package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.september
import no.nav.helse.serde.api.v2.Arbeidsgiverinntekt
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.InntekterFraAOrdningen
import no.nav.helse.serde.api.v2.Inntektkilde
import no.nav.helse.serde.api.v2.OmregnetÅrsinntekt
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsforhold
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.speilApi
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderFlereAGTest : AbstractEndToEndTest() {

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
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )


        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }?.ghostPerioder

        assertEquals(1, perioder?.size)

        val actual = perioder!!.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 1.januar,
                tom = 20.januar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = false
            )

        assertTrue(areEquals(expected, actual))
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
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        )

        val perioder = speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder

        assertEquals(1, perioder.size)

        val actual = perioder.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 1.januar,
                tom = 31.januar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = false
            )

        assertTrue(areEquals(expected, actual))
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
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )

        val perioder = speilJson.arbeidsgivere.find { it.organisasjonsnummer == a2 }?.ghostPerioder
        assertEquals(1, perioder?.size)

        val actual = perioder!!.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 1.februar,
                tom = 20.februar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = false
            )

        assertTrue(areEquals(expected, actual))
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
        håndterUtbetalt(orgnummer = a3)

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
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )


        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder

        assertEquals(1, perioder.size)

        val actual = perioder.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 1.januar,
                tom = 20.januar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = false
            )

        assertTrue(areEquals(expected, actual))

        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder
        )
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
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )

        val perioder = speilJson.arbeidsgivere.find { it.organisasjonsnummer == a2 }?.ghostPerioder

        assertEquals(1, perioder?.size)

        val actual = perioder!!.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 3.januar,
                tom = 31.januar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = false
            )

        assertTrue(areEquals(expected, actual))
    }

    @Test
    fun `arbeidsforhold uten sykepengegrunnlag de tre siste månedene før skjæringstidspunktet skal ikke ha ghostperioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        val utbetalinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar(2017), 31.januar(2017), 100.prosent, 1000.daglig)
        )
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a2, 1.januar(2017), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val personDto = serializePersonForSpeil(person)
        val ghostpølser = personDto.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        assertEquals(0, ghostpølser.size)
    }


    @Test
    fun `tar med flere arbeidsforhold som gjelder skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.januar, 1000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, 1000.månedlig.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person)
        assertEquals(listOf(a1, a2), personDto.arbeidsforholdPerSkjæringstidspunkt[1.januar]?.map { it.orgnummer })
    }

    @Test
    fun `tar med deaktiverte arbeidsforhold som gjelder skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.januar, 1000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, 1000.månedlig.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true)))

        val personDto = serializePersonForSpeil(person)
        assertEquals(
            mapOf(
                1.januar to listOf(
                    ArbeidsforholdDTO(a1, LocalDate.EPOCH, null, false),
                    ArbeidsforholdDTO(a2, LocalDate.EPOCH, null, true)
                )
            ),
            personDto.arbeidsforholdPerSkjæringstidspunkt
        )

        val perioder = personDto.arbeidsgivere.find { it.organisasjonsnummer == a2 }?.ghostPerioder

        assertEquals(1, perioder?.size)

        val actual = perioder!!.first()
        val expected =
            GhostPeriodeDTO(
                id = UUID.randomUUID(),
                fom = 1.januar,
                tom = 31.januar,
                skjæringstidspunkt = 1.januar,
                vilkårsgrunnlagHistorikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
                deaktivert = true
            )

        assertTrue(areEquals(expected, actual))
    }

    @Test
    fun `tar med arbeidsforhold som var med i beregning av opptjening, men ikke gjelder skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a2, 19.desember(2017), INNTEKT.repeat(11))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a2, 19.desember(2017), INNTEKT.repeat(2))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 20.desember(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 19.desember(2017))
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person)

        assertEquals(listOf(a1, a2), personDto.arbeidsforholdPerSkjæringstidspunkt[1.januar]?.map { it.orgnummer })
    }

    @Test
    fun `legger ved sammenlignignsgrunnlag og sykepengegrunnlag for deaktiverte arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.januar, 1000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, 1000.månedlig.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true)))

        val personDto = serializePersonForSpeil(person)
        val vilkårsgrunnlag =
            personDto.vilkårsgrunnlagHistorikk[person.nyesteIdForVilkårsgrunnlagHistorikk()]?.get(1.januar)
        assertEquals(listOf(a1, a2), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertEquals(
            Arbeidsgiverinntekt(
                organisasjonsnummer = a2,
                omregnetÅrsinntekt = OmregnetÅrsinntekt(
                    kilde = Inntektkilde.AOrdningen,
                    beløp = 12000.0,
                    månedsbeløp = 1000.0,
                    inntekterFraAOrdningen = listOf(
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.OCTOBER), 1000.0),
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.NOVEMBER), 1000.0),
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.DECEMBER), 1000.0)
                    )
                ),
                sammenligningsgrunnlag = 12000.0,
                deaktivert = true
            ),
            vilkårsgrunnlag?.inntekter?.find { it.organisasjonsnummer == a2 }
        )
    }

    @Test
    fun `legger ved sammenligningsgrunnlag ved manglende sykepengegrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.oktober(2017), 1000.månedlig.repeat(9))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 30.september(2017))
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person)
        val vilkårsgrunnlag =
            personDto.vilkårsgrunnlagHistorikk[person.nyesteIdForVilkårsgrunnlagHistorikk()]?.get(1.januar)
        assertEquals(listOf(a1, a2), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertEquals(
            Arbeidsgiverinntekt(
                organisasjonsnummer = a2,
                omregnetÅrsinntekt = null,
                sammenligningsgrunnlag = 9000.0,
                deaktivert = false
            ),
            vilkårsgrunnlag?.inntekter?.find { it.organisasjonsnummer == a2 }
        )
        assertEquals(listOf(a1, a2), personDto.arbeidsgivere.map { it.organisasjonsnummer })
    }

    @Test
    fun `Skal ikke ta med inntekt på vilkårsgrunnlaget som mangler både sykepengegrunnlag og sammenligningsgrunnlag på skjæringstidspunktet`() {
        nyttVedtak(1.januar(2017), 31.januar(2017), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), null),
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person)
        val vilkårsgrunnlag =
            personDto.vilkårsgrunnlagHistorikk[person.nyesteIdForVilkårsgrunnlagHistorikk()]?.get(1.januar)
        assertEquals(listOf(a1), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertEquals(listOf(a2, a1), personDto.arbeidsgivere.map { it.organisasjonsnummer })
    }

    @Test
    fun `ikke ta med sykepengegrunnlag fra a-ordningen dersom det ikke er rapportert inntekt de 2 siste mnd`() {
        nyttVedtak(1.januar(2017), 31.januar(2017), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.november(2017), listOf(INNTEKT)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.november(2017), listOf(INNTEKT))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null),
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person)
        val vilkårsgrunnlag =
            personDto.vilkårsgrunnlagHistorikk[person.nyesteIdForVilkårsgrunnlagHistorikk()]?.get(1.januar)
        assertNull(vilkårsgrunnlag?.inntekter?.firstOrNull { it.organisasjonsnummer == a2 }?.omregnetÅrsinntekt)
    }

    @Test
    fun `tar med refusjonshistorikk pr arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            orgnummer = a1,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(1000.månedlig), 19.januar),
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(2000.månedlig), 23.januar),
                )
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = speilApi()

        personDto.arbeidsgivere.first().generasjoner.first().perioder.filterIsInstance(BeregnetPeriode::class.java)
            .first().refusjon.let {
                assertNotNull(it)
                assertEquals(2, it.endringer.size)
                assertEquals(32000.0, it.endringer.first().beløp)
                assertEquals(19.januar, it.endringer.first().dato)
                assertEquals(33000.0, it.endringer.last().beløp)
                assertEquals(23.januar, it.endringer.last().dato)
            }
    }

    @Test
    fun `tar med vilkårsgrunnlag med ikke-rapportert inntekt`() {
        // A2 må være først i listen for at buggen skal intreffe
        nyttVedtak(1.januar(2017), 31.januar(2017), 100.prosent, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                    }
                },
                emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = speilApi()

        val inntektsgrunnlag = personDto.inntektsgrunnlag.last().inntekter.firstOrNull { it.arbeidsgiver == a2 }
        assertEquals(
            InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO(
                kilde = InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.IkkeRapportert,
                beløp = 0.0,
                månedsbeløp = 0.0,
                inntekterFraAOrdningen = null
            ),
            inntektsgrunnlag?.omregnetÅrsinntekt
        )
    }

    private fun areEquals(a: GhostPeriodeDTO, b: GhostPeriodeDTO): Boolean =
        a.fom == b.fom && a.tom == b.tom && a.skjæringstidspunkt == b.skjæringstidspunkt && a.vilkårsgrunnlagHistorikkInnslagId == b.vilkårsgrunnlagHistorikkInnslagId && a.deaktivert == b.deaktivert
}

