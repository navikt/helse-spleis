package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertTrue

internal class NyArbeidsgiverUnderveisTest : AbstractDslTest() {

    @Test
    fun `Ny arbeidsgiver underveis`() {
        a1 {
            nyttVedtak(januar)
        }
        a2 {
            håndterSøknad(februar)
            håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar))
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(1.vedtaksperiode,Varselkode.TilkommenInntekt.`Søknad fra arbeidsgiver som ikke er i sykepengegrunnlaget`, Varselkode.RV_VV_4)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.let { avvisteDager ->
                assertEquals(8, avvisteDager.size)
                assertTrue(avvisteDager.all { it.begrunnelser.single() == Begrunnelse.MinimumSykdomsgrad })
            }
        }
    }

    @Test
    fun `ghost blir syk`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
            assertVarsler(listOf(Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterSøknad(februar)
        }
        a2 {
            håndterSøknad(februar)
            håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = 10000.månedlig, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertVarsler(listOf(Varselkode.RV_IM_8), 1.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `ghost blir deaktivert, deretter syk`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(1.vedtaksperiode, Varselkode.TilkommenInntekt.`Søknad fra arbeidsgiver som ikke er i sykepengegrunnlaget`)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a1 {
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, false, "test"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `Omgjøring av overlappende periode med nytt skjæringstidspunkt`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(30.januar, Dagtype.Arbeidsdag), ManuellOverskrivingDag(31.januar, Dagtype.Arbeidsdag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(Sykdom(31.januar, 15.februar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(31.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            håndterInntektsmelding(listOf(31.januar til 15.februar), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertDoesNotThrow { håndterYtelser(1.vedtaksperiode) }
        }
    }

    @Test
    fun `saksbehandler flytter arbeidsgiver på skjæringstidspunktet som tilkommen`() {
        a1 {
            håndterSøknad(januar)
        }
        a2 {
            håndterSøknad(Sykdom(10.januar, 31.januar, 50.prosent))
            håndterInntektsmelding(listOf(10.januar til 25.januar), 5000.månedlig)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.januar, null)
                )
            )
            assertVarsler(listOf(Varselkode.RV_VV_1), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, deaktivert = true, forklaring = "skal ikke inngå i sykepengegrunnlaget"))
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(
                InntekterForBeregning.Inntektsperiode(a2, 10.januar, tom = 31.januar, inntekt = 5000.månedlig)
            ))
            håndterSimulering(1.vedtaksperiode)

            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[1.januar].also { dag ->
                assertEquals(100, dag.økonomi.inspektør.totalGrad)
            }
            inspektør.utbetalingstidslinjer(1.vedtaksperiode)[10.januar].also { dag ->
                assertEquals(83, dag.økonomi.inspektør.totalGrad)
            }
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.also { vilkårsgrunnlagInspektør ->
                assertEquals(INNTEKT, vilkårsgrunnlagInspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
            }
        }
    }
}
