package no.nav.helse.spleis.e2e.overstyring

import java.util.*
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import no.nav.helse.økonomi.inspectors.inspektør

internal class SkjønnsmessigFastsettelseTest : AbstractDslTest() {

    @Test
    fun `Når inntekt skjønnsfastsettes til 0 og det finnes andre arbeidsgivere i økonomi-lista`() {
        "a1" {}
        "a2" {}
        "a3" {}
        "a4" {}
        "a5" {}
        "a6" {
            tilGodkjenning(mars)
            håndterSkjønnsmessigFastsettelse(1.mars, listOf(OverstyrtArbeidsgiveropplysning("a6", INGEN)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_1, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
            assertEquals(0, inspektør(1.vedtaksperiode).utbetalingstidslinje[17.mars].økonomi.inspektør.totalGrad)
        }
    }

    @Test
    fun `blir syk fra ghost etter skjønnsfastsettelse - skal ikke medføre ny skjønnsfastsettelse`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSkjønnsmessigFastsettelse(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(a1, INNTEKT + 500.månedlig),
                OverstyrtArbeidsgiveropplysning(a2, INNTEKT - 500.månedlig)
            )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT + 500.månedlig)
                assertInntektsgrunnlag(a2, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT - 500.månedlig)
            }
        }
    }

    @Test
    fun `overskriver ikke skjønnsmessig fastsettelse om inntekt fra im utgjør mindre enn 1kr forskjell på årlig`() {
        val inntektVedNyIM = 372000.5.årlig
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSkjønnsmessigFastsettelse(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(a1, INNTEKT + 500.månedlig),
                OverstyrtArbeidsgiveropplysning(a2, INNTEKT - 500.månedlig)
            )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntektVedNyIM)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, inntektVedNyIM)
            }
        }
    }

    @Test
    fun `endring i refusjon skal ikke endre omregnet årsinntekt`() {
        (a1 og a2).nyeVedtak(januar)

        håndterSkjønnsmessigFastsettelse(
            1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(a1, 19000.0.månedlig),
            OverstyrtArbeidsgiveropplysning(a2, 21000.0.månedlig)
        )
        )

        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        (a1 og a2).forlengVedtak(februar)

        a1 {
            val im = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig, refusjon = Refusjon(20000.månedlig, opphørsdato = 31.januar))

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, 20_000.månedlig, forventetFastsattÅrsinntekt = 19_000.månedlig)
                assertInntektsgrunnlag(a2, 20_000.månedlig, forventetFastsattÅrsinntekt = 21_000.månedlig)
            }
        }
    }

    @Test
    fun `korrigere inntekten på noe som allerede har blitt skjønnsmessig fastsatt`() {
        nyttVedtak(januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT * 2)
            }
        }
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 3)))
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT * 3)
            }
        }
    }

    @Test
    fun `skjønnsmessig fastsatt inntekt skal ikke ha avviksvurdering`() {
        nyttVedtak(januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT * 2)
            }
        }
    }

    @Test
    fun `alle inntektene må skjønnsfastsettes ved overstyring`() {
        (a1 og a2).nyeVedtak(januar)
        a1 {
            assertThrows<IllegalStateException> {
                håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
            }
        }
    }

    @Test
    fun `Ved deaktivering av arbeidsgivere må eventuell skjønnsmessig fastsettelse rulles tilbake`() {
        (a1 og a2).nyeVedtak(januar, inntekt = 20_000.månedlig)
        a1 {
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = 40_000.månedlig),
                OverstyrtArbeidsgiveropplysning(orgnummer = a2, inntekt = 40_000.månedlig),
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, 2) {
                assertInntektsgrunnlag(a1, 20_000.månedlig, forventetFastsattÅrsinntekt = 40_000.månedlig)
                assertInntektsgrunnlag(a2, 20_000.månedlig, forventetFastsattÅrsinntekt = 40_000.månedlig)
            }
        }
        a1 {
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "foo"))
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, 2) {
                assertInntektsgrunnlag(a1, 20_000.månedlig, forventetFastsattÅrsinntekt = 20_000.månedlig)
                assertInntektsgrunnlag(a2, 20_000.månedlig, forventetFastsattÅrsinntekt = 20_000.månedlig, deaktivert = true)
            }
            assertVarsler(1.vedtaksperiode, RV_UT_23)
        }
        a2 {
            assertVarsler(1.vedtaksperiode, RV_UT_23)
        }
    }


    @Test
    fun `saksbehandler-inntekt overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT * 2, forventetKorrigertInntekt = INNTEKT * 2)
            }
        }
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT * 2, forventetKorrigertInntekt = INNTEKT * 2)
            }
        }
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med samme beløp`() {
        a1 {
            tilGodkjenning(januar)
            val inntekt = INNTEKT
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = inntekt)))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            nullstillTilstandsendringer()
            val im = håndterInntektsmelding(listOf(1.januar til 16.januar), inntekt)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `korrigert IM etter skjønnsfastsettelse på flere AG`() {
        (a1 og a2 og a3).nyeVedtak(januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 3)))
        a3 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, 20000.månedlig)
                assertInntektsgrunnlag(a2, 20000.månedlig)
                assertInntektsgrunnlag(a3, 20000.månedlig, INNTEKT * 3, forventetKorrigertInntekt = INNTEKT * 3)
            }
        }
        håndterSkjønnsmessigFastsettelse(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a2, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 2)
            )
        )
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, 20000.månedlig, forventetFastsattÅrsinntekt = INNTEKT * 2)
                assertInntektsgrunnlag(a2, 20000.månedlig, forventetFastsattÅrsinntekt = INNTEKT * 2)
                assertInntektsgrunnlag(a3, 20000.månedlig, INNTEKT * 3, forventetFastsattÅrsinntekt = INNTEKT * 2, forventetKorrigertInntekt = INNTEKT * 3)
            }
        }

        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 20000.månedlig)
                assertInntektsgrunnlag(a3, 20000.månedlig, INNTEKT * 3, forventetKorrigertInntekt = INNTEKT * 3)
            }
        }
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med ulikt beløp`() {
        nyttVedtak(januar)
        håndterSkjønnsmessigFastsettelse(
            1.januar,
            listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2))
        )
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 3)
        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 3)
            }
        }
    }

    @Test
    fun `skjønnsmessig fastsatt - men så skulle det være etter hovedregel`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)

            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 2, forventetFastsattÅrsinntekt = INNTEKT * 2)
            }
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT)))

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 2, INNTEKT, forventetKorrigertInntekt = INNTEKT)
            }
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Tidligere perioder revurderes mens nyere skjønnsmessig fastsettes`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(mars, a1)
            håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(
                listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag, 100))
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `Overstyre refusjon etter skjønnsmessig fastsatt -- etter utbetalt`() {
        val inntektsmeldingInntekt = INNTEKT
        val skjønnsfastsattInntekt = INNTEKT * 2

        a1 {
            // Normal behandling med Inntektsmelding
            håndterSøknad(januar)
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntektsmeldingInntekt, refusjon = Refusjon(inntektsmeldingInntekt, null, emptyList()))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, inntektsmeldingInntekt)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, inntektsmeldingInntekt, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))

            // Saksbehandler skjønnsmessig fastsetter
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsfastsattInntekt)))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, inntektsmeldingInntekt, forventetFastsattÅrsinntekt = skjønnsfastsattInntekt)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, inntektsmeldingInntekt, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))

            // Saksbehandler endrer kun refusjon, men beholder inntekt
            val overstyrInntektOgRefusjonId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = inntektsmeldingInntekt, refusjonsopplysninger = listOf(Triple(1.januar, null, skjønnsfastsattInntekt)))), hendelseId = overstyrInntektOgRefusjonId)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, inntektsmeldingInntekt, forventetFastsattÅrsinntekt = skjønnsfastsattInntekt)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, skjønnsfastsattInntekt, overstyrInntektOgRefusjonId.saksbehandler), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `Overstyre refusjon og inntekt etter skjønnsmessig fastsatt -- inntekten er det samme som skjønnsfastsatt`() {
        val inntektsmeldingInntekt = INNTEKT
        val skjønnsfastsattInntekt = INNTEKT * 2

        a1 {
            // Normal behandling med Inntektsmelding
            håndterSøknad(januar)
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntektsmeldingInntekt, refusjon = Refusjon(inntektsmeldingInntekt, null, emptyList()))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, inntektsmeldingInntekt, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))

            // Saksbehandler skjønnsmessig fastsetter
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsfastsattInntekt)))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetFastsattÅrsinntekt = skjønnsfastsattInntekt)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, inntektsmeldingInntekt, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))

            // Saksbehandler endrer refusjon og inntekt til INNTEKT * 2
            val overstyrInntektOgRefusjonId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsfastsattInntekt, refusjonsopplysninger = listOf(Triple(1.januar, null, skjønnsfastsattInntekt)))), hendelseId = overstyrInntektOgRefusjonId)
            assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, skjønnsfastsattInntekt, forventetKorrigertInntekt = skjønnsfastsattInntekt)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, skjønnsfastsattInntekt, overstyrInntektOgRefusjonId.saksbehandler), inspektør.refusjon(1.vedtaksperiode))
        }
    }
}
