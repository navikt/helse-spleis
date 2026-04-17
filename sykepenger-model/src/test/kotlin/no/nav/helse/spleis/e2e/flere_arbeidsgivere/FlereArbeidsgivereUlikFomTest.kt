package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.a4
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereUlikFomTest : AbstractDslTest() {

    @Test
    fun `får varsel om ulik startdato selv om vi velger en inntekt i samme måned som skjæringstidspunktet`() {
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 {
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
            håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = INNTEKT * 1.1, førsteFraværsdag = 1.februar)
            håndterInntektsmelding(emptyList(), beregnetInntekt = INNTEKT * 1.2, førsteFraværsdag = 25.januar)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertInntektsgrunnlag(1.januar, 2) {
                assertInntektsgrunnlag(a1, forventetFaktaavklartInntekt = INNTEKT)
                assertInntektsgrunnlag(a2, forventetFaktaavklartInntekt = INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Gjenbruk av tidsnære opplysninger slår ikke til ved skatteinntekt i inntektsgrunnlaget`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(februar)
            håndterInntektsmelding(listOf(1.februar til 16.februar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        val refusjonFør = a2 { inspektør.refusjon(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }

        observatør.vedtaksperiodeVenter.clear()
        nullstillTilstandsendringer()

        a2 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.februar, Dagtype.Arbeidsdag)))
        }
        assertEquals("VILKÅRSPRØVING", observatør.vedtaksperiodeVenter.single().venterPå.venteårsak.hva)
        a2 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertEquals(refusjonFør, inspektør.refusjon(1.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertVarsler(listOf(Varselkode.RV_IV_7), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Går ikke videre til vilkårsprøving om vi mangler IM fra en arbeidsgiver`() {
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.januar, 28.februar, 100.prosent)) }
        a3 { håndterSøknad(Sykdom(1.februar, 31.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER) }
    }

    @Test
    fun `kort periode hos en ag2 forkaster utbetaling`() {
        a1 { nyPeriode(1.januar til 20.januar) }
        a2 { nyPeriode(5.januar til 20.januar) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(1, inspektør.utbetalinger(1.vedtaksperiode).size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertTrue(inspektør.utbetalinger(1.vedtaksperiode).isEmpty())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `ag2 forkaster ikke utbetaling tildelt av ag1`() {
        a1 { nyPeriode(1.januar til 20.januar) }
        a2 { nyPeriode(1.januar til 20.januar) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 2,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 2,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(1, inspektør.utbetalinger(1.vedtaksperiode).size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            val ag2Utbetalinger = inspektør.utbetalinger(1.vedtaksperiode)
            assertEquals(1, ag2Utbetalinger.size)
            assertEquals(Utbetalingstatus.IKKE_UTBETALT, ag2Utbetalinger[0].inspektør.tilstand)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `ag2 forkaster utbetaling tildelt av ag1 om det har skjedd endringer i mellomtiden`() {
        a1 { nyPeriode(1.januar til 20.januar) }
        a2 { nyPeriode(1.januar til 20.januar) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 2,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 2,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(2, inspektør.utbetalinger(1.vedtaksperiode).size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            val ag2Utbetalinger = inspektør.utbetalinger(1.vedtaksperiode)
            assertEquals(2, ag2Utbetalinger.size)
            assertEquals(Utbetalingstatus.FORKASTET, ag2Utbetalinger[0].inspektør.tilstand)
            assertEquals(Utbetalingstatus.IKKE_UTBETALT, ag2Utbetalinger[1].inspektør.tilstand)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `To førstegangsbehandlinger med ulik fom i forskjellige måneder - skal bruke skatteinntekter for arbeidsgiver med senest fom`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = INNTEKT
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to 20000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 { håndterYtelser(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 20000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `To førstegangsbehandlinger med lik fom - skal bruke inntektsmelding for begge arbeidsgivere`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 18000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 { håndterYtelser(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, 30000.månedlig)
                assertInntektsgrunnlag(a2, 18000.månedlig)
            }
        }
    }

    @Test
    fun `Bruker gjennomsnitt av skatteinntekter ved ulik fom i forskjellige måneder`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = INNTEKT
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 31000.månedlig, a2 to 21000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 { håndterYtelser(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 21000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `Ulik fom og ikke 6G-begrenset, utbetalinger beregnes riktig`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 10000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = 20000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 20000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(16.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(462, a1Linje.beløp)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(20000.månedlig.dagligInt, a2Linje.beløp)
        }
    }

    @Test
    fun `Ulik fom og 6G-begrenset, skal beregne utbetaling ut fra skatteinntekter for a2`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = 40000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val arbeidsgiverOppdrag = inspektør.sisteUtbetaling().arbeidsgiverOppdrag
            assertEquals(1, arbeidsgiverOppdrag.size)
            val a1Linje1 = arbeidsgiverOppdrag[0]
            assertEquals(16.mars, a1Linje1.fom)
            assertEquals(31.mars, a1Linje1.tom)
            assertEquals(997, a1Linje1.beløp)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(1163, a2Linje.beløp)
        }
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og siste arbeidsgiver er 50 prosent sykmeldt`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 50.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = 40000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
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
        }
        a1 {
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(16.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(997, a1Linje.beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(582, a2Linje.beløp)
        }
    }

    @Test
    fun `mursteinspølser og totalgrad`() {
        a1 { håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a1 { håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 10,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT / 10, a2 to INNTEKT)
            )
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
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 { assertEquals(2, inspektør.antallUtbetalinger) }
        a2 {
            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[31.januar].økonomi.inspektør.totalGrad)
            assertEquals(100, inspektør.utbetalingstidslinjer(2.vedtaksperiode)[1.februar].økonomi.inspektør.totalGrad)
        }
    }

    @Test
    fun `mursteinspølser og manglende inntektsmelding på a2`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        nullstillTilstandsendringer()

        a2 { håndterSøknad(Sykdom(5.januar, lørdag den 20.januar, 100.prosent)) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING) }
        nullstillTilstandsendringer()
        // Perioden som står i AvventerGodkjenningRevurdering får ikke noe signal om overstyring igangsatt ettersom disse periodene er _etter_ 1.vedtaksperiode
        // Allikevel kan den ikke beregnes på nytt, fordi mursteinssituasjonen som nå har oppstått gjør at vi mangler refusjonsopplysninger på 22.januar
        a2 { håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent)) }
        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING) }
        a2 { assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING) }
        a1 {
            håndterSøknad(Sykdom(21.januar, 5.februar, 100.prosent))
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        }

        // Om f.eks. saksbehandler overstyrer perioden som står i AvventerGodkjenningRevurdering blir den sittende fast i AvventerRevurdering
        // Ettersom vi nå må ha inntektsmelding fra a2 for refusjonsopplysninger 22.januar
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1)))

        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING) }
        val vedtaksperiodeIdA1 = a1 { 1.vedtaksperiode }
        val venterPå = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == vedtaksperiodeIdA1 }.venterPå
        assertEquals("BEREGNING", venterPå.venteårsak.hva)
        assertEquals(a1, venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
        assertEquals(vedtaksperiodeIdA1, venterPå.vedtaksperiodeId)
    }

    @Test
    fun `Førstegangsbehandling med ulik fom og første arbeidsgiver er 50 prosent sykmeldt`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 50.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(5.mars til 20.mars),
                førsteFraværsdag = 5.mars,
                beregnetInntekt = 40000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
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
        }
        a1 {
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(16.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(499, a1Linje.beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(21.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(1163, a2Linje.beløp)
        }
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars)) }
        a2 { håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(3.januar, 28.februar)) }
        a3 { håndterSøknad(Sykdom(3.januar, 28.februar, 100.prosent)) }
        a4 { håndterSykmelding(Sykmeldingsperiode(4.januar, 15.februar)) }
        a4 { håndterSøknad(Sykdom(4.januar, 15.februar, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(2.januar til 17.januar),
                beregnetInntekt = 32000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(3.januar til 18.januar),
                beregnetInntekt = 33000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a4 {
            håndterArbeidsgiveropplysninger(
                listOf(4.januar til 19.januar),
                beregnetInntekt = 34000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4)
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
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a4 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(31.januar, a1Linje.tom)
            assertEquals(515, a1Linje.beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(18.januar, a2Linje.fom)
            assertEquals(15.mars, a2Linje.tom)
            assertEquals(532, a2Linje.beløp)
        }
        a3 {
            assertEquals(1, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.size)
            val a3Linje1 = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(19.januar, a3Linje1.fom)
            assertEquals(28.februar, a3Linje1.tom)
            assertEquals(549, a3Linje1.beløp)
        }
        a4 {
            val a4Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(20.januar, a4Linje.fom)
            assertEquals(15.februar, a4Linje.tom)
            assertEquals(565, a4Linje.beløp)
        }
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars)) }
        a1 { håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars)) }
        a2 { håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars)) }
        a3 { håndterSøknad(Sykdom(3.januar, 15.mars, 100.prosent)) }
        a4 { håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars)) }
        a4 { håndterSøknad(Sykdom(4.januar, 15.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(2.januar til 17.januar),
                beregnetInntekt = 32000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(3.januar til 18.januar),
                beregnetInntekt = 33000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a4 {
            håndterArbeidsgiveropplysninger(
                listOf(4.januar til 19.januar),
                beregnetInntekt = 34000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4)
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
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a4 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(515, a1Linje.beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(18.januar, a2Linje.fom)
            assertEquals(15.mars, a2Linje.tom)
            assertEquals(532, a2Linje.beløp)
        }
        a3 {
            val a3Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(19.januar, a3Linje.fom)
            assertEquals(15.mars, a3Linje.tom)
            assertEquals(549, a3Linje.beløp)
        }
        a4 {
            val a4Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(20.januar, a4Linje.fom)
            assertEquals(15.mars, a4Linje.tom)
            assertEquals(565, a4Linje.beløp)
        }
    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt, nå med gradert sykmelding!`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars)) }
        a1 { håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars)) }
        a2 { håndterSøknad(Sykdom(2.januar, 15.mars, 100.prosent)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars)) }
        a3 { håndterSøknad(Sykdom(3.januar, 15.mars, 50.prosent)) }
        a4 { håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars)) }
        a4 { håndterSøknad(Sykdom(4.januar, 15.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(2.januar til 17.januar),
                beregnetInntekt = 32000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(3.januar til 18.januar),
                beregnetInntekt = 33000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a4 {
            håndterArbeidsgiveropplysninger(
                listOf(4.januar til 19.januar),
                beregnetInntekt = 34000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4)
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
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a4 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            val a1Linjer = inspektør.sisteUtbetaling().arbeidsgiverOppdrag
            assertEquals(1, a1Linjer.size)
            assertEquals(17.januar, a1Linjer[0].fom)
            assertEquals(15.mars, a1Linjer[0].tom)
            assertEquals(515, a1Linjer[0].beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(18.januar, a2Linje.fom)
            assertEquals(15.mars, a2Linje.tom)
            assertEquals(532, a2Linje.beløp)
        }
        a3 {
            val arbeidsgiver3Oppdrag = inspektør.sisteUtbetaling().arbeidsgiverOppdrag
            assertEquals(1, arbeidsgiver3Oppdrag.size)
            val a3Linje = arbeidsgiver3Oppdrag.single()
            assertEquals(19.januar, a3Linje.fom)
            assertEquals(15.mars, a3Linje.tom)
            assertEquals(274, a3Linje.beløp)
        }
        a4 {
            val a4Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(20.januar, a4Linje.fom)
            assertEquals(15.mars, a4Linje.tom)
            assertEquals(565, a4Linje.beløp)
        }
    }

    @Test
    fun `Wow! Her var det mye greier - ulik fom, lik tom, forskjellig gradering for alle arbeidsgivere`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars)) }
        a1 { håndterSøknad(Sykdom(1.januar, 15.mars, 22.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars)) }
        a2 { håndterSøknad(Sykdom(2.januar, 15.mars, 69.prosent)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars)) }
        a3 { håndterSøknad(Sykdom(3.januar, 15.mars, 42.prosent)) }
        a4 { håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars)) }
        a4 { håndterSøknad(Sykdom(4.januar, 15.mars, 37.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(2.januar til 17.januar),
                beregnetInntekt = 32000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(3.januar til 18.januar),
                beregnetInntekt = 33000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a4 {
            håndterArbeidsgiveropplysninger(
                listOf(4.januar til 19.januar),
                beregnetInntekt = 34000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3, a4)
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
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a4 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(113, a1Linje.beløp)
        }
        a2 {
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(18.januar, a2Linje.fom)
            assertEquals(15.mars, a2Linje.tom)
            assertEquals(367, a2Linje.beløp)
        }
        a3 {
            val a3Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(19.januar, a3Linje.fom)
            assertEquals(15.mars, a3Linje.tom)
            assertEquals(230, a3Linje.beløp)
        }
        a4 {
            val a4Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.single()
            assertEquals(20.januar, a4Linje.fom)
            assertEquals(15.mars, a4Linje.tom)
            assertEquals(209, a4Linje.beløp)
        }
    }

    @Test
    fun `Flere arbeidsgivere med ulik fom - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(4.mars, 31.mars)) }
        a2 { håndterSøknad(Sykdom(4.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                beregnetInntekt = 10000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(4.mars til 19.mars),
                førsteFraværsdag = 4.mars,
                beregnetInntekt = 19000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, 10000.månedlig)
                assertInntektsgrunnlag(a2, 19000.månedlig)
            }
        }
    }

    @Test
    fun `Flere arbeidsgivere med lik fom - skal ikke få warning om flere arbeidsforhold med ulikt sykefravær`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a2 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 19000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
        a2 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april)) }
        a2 { håndterSøknad(Sykdom(20.mars, 25.april, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(20.mars til 4.april),
                førsteFraværsdag = 20.mars,
                beregnetInntekt = 40000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.last()
            assertEquals(16.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(997, a1Linje.beløp)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.last()
            assertEquals(5.april, a2Linje.fom)
            assertEquals(25.april, a2Linje.tom)
            assertEquals(1163, a2Linje.beløp)
        }
    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG, nå med gradert sykmelding!`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 31.mars, 50.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april)) }
        a2 { håndterSøknad(Sykdom(20.mars, 25.april, 70.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                førsteFraværsdag = 28.februar,
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(20.mars til 4.april),
                førsteFraværsdag = 20.mars,
                beregnetInntekt = 40000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 35000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a1Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.last()
            assertEquals(16.mars, a1Linje.fom)
            assertEquals(31.mars, a1Linje.tom)
            assertEquals(499, a1Linje.beløp)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val a2Linje = inspektør.sisteUtbetaling().arbeidsgiverOppdrag.last()
            assertEquals(5.april, a2Linje.fom)
            assertEquals(25.april, a2Linje.tom)
            assertEquals(814, a2Linje.beløp)
        }
    }

    @Test
    fun `skjæringstidspunkt i samme måned betyr at begge arbeidsgivere bruker inntekt fra inntektsmelding`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars)) }
        a1 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(5.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 31000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(5.mars til 20.mars),
                beregnetInntekt = 21000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 21000.månedlig)
            }
        }
    }

    @Test
    fun `skjæringstidspunkt i forskjellige måneder betyr at senere arbeidsgiver bruker skatteinntekt`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(28.februar, 30.mars)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.mars, 30.mars)) }
        a1 { håndterSøknad(Sykdom(28.februar, 30.mars, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.mars, 30.mars, 100.prosent)) }
        a1 {
            håndterInntektsmelding(
                listOf(28.februar til 15.mars),
                beregnetInntekt = 31000.månedlig
            )
        }
        a2 {
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 21000.månedlig
            )
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig, a2 to 20000.månedlig)
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            assertInntektsgrunnlag(28.februar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 20000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `To arbeidsgivere med ulik fom i samme måned - med en tidligere periode i samme måned - andre vedtaksperiode velger IM for egen første fraværsdag`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar)) }
        a1 { håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
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
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 21.januar
            )
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar))
            håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 22.januar,
                beregnetInntekt = 32000.månedlig
            )
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertInntektsgrunnlag(21.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 32000.månedlig)
            }
        }
    }

    @Test
    fun `alle arbeidsgivere burde hoppe inn i AVVENTER_BLOKKERENDE_PERIODE dersom de har samme skjæringstidspunkt men ikke overlapper`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar)) }
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a3 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }

    @Test
    fun `søknad for ghost etter utbetalt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        listOf(a1).forlengVedtak(februar)

        nullstillTilstandsendringer()
        a2 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }

        a1 { assertTilstander(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING) }
        a1 { assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING) }

        nullstillTilstandsendringer()
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }

        a1 { assertTilstander(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 { assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING) }

        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { assertTilstander(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET) }
        a1 { assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET) }

        a1 {
            val revurderingen = inspektør.sisteUtbetaling()
            assertEquals(1, revurderingen.arbeidsgiverOppdrag.size)
            assertEquals(0, revurderingen.personOppdrag.size)
            revurderingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(1.februar til 28.februar, linje.fom til linje.tom)
                assertEquals(1080, linje.beløp)
            }
        }
        a2 {
            val førstegangsutbetalingen = inspektør.sisteUtbetaling()
            assertEquals(1, førstegangsutbetalingen.arbeidsgiverOppdrag.size)
            assertEquals(0, førstegangsutbetalingen.personOppdrag.size)
            førstegangsutbetalingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(17.februar til 28.februar, linje.fom til linje.tom)
                assertEquals(1080, linje.beløp)
            }
        }
    }

    @Test
    fun `søknad for ghost etter utbetalt som delvis overlapper med to perioder hos a1`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        listOf(a1).forlengVedtak(februar)

        nullstillTilstandsendringer()
        a2 { håndterSøknad(Sykdom(20.januar, 28.februar, 100.prosent)) }

        a1 { assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING) }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING) }
        a1 { assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING) }

        nullstillTilstandsendringer()
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(20.januar til 4.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }

        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 { assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING) }

        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a1 { assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET) }
        a1 { assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET) }

        a1 {
            val revurderingen = inspektør.sisteUtbetaling()
            assertEquals(1, revurderingen.arbeidsgiverOppdrag.size)
            assertEquals(0, revurderingen.personOppdrag.size)
            revurderingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(1.februar til 28.februar, linje.fom til linje.tom)
                assertEquals(1080, linje.beløp)
            }
        }
        a2 {
            val førstegangsutbetalingen = inspektør.sisteUtbetaling()
            assertEquals(1, førstegangsutbetalingen.arbeidsgiverOppdrag.size)
            assertEquals(0, førstegangsutbetalingen.personOppdrag.size)
            førstegangsutbetalingen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(5.februar til 28.februar, linje.fom til linje.tom)
                assertEquals(1080, linje.beløp)
            }
        }
    }

    @Test
    fun `skjæringstidspunktet er i måneden før ag1`() {
        a1 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(20.januar, 31.januar, 100.prosent)) }
        a1 {
            håndterInntektsmelding(listOf(1.februar til 16.februar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(20.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `skjæringstidspunktet er i måneden før ag1 og ag2 - nyoppstartet arbeidsforhold ag2 - bare inntekt 3 mnd før`() {
        a1 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a3 { håndterSøknad(Sykdom(31.januar, 14.februar, 100.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.februar til 16.februar)) }
        a2 { håndterInntektsmelding(listOf(1.februar til 16.februar)) }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.november(2017), null),
                    Triple(a3, 1.oktober(2017), null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(31.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
                assertInntektsgrunnlag(a3, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `avviser dag med totalgrad under 20 prosent`() {
        val inntektA1 = 51775.månedlig
        val inntektA2 = 10911.månedlig
        a1 { nyPeriode(1.mai(2023) til 30.mai(2023)) }
        a2 { nyPeriode(1.mai(2023) til 31.mai(2023)) }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mai(2023) til 16.mai(2023)),
                beregnetInntekt = inntektA1,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mai(2023) til 16.mai(2023)),
                beregnetInntekt = inntektA2,
                refusjon = Refusjon(INGEN, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
        }
        a2 {
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            assertEquals(0.daglig, utbetalingstidslinje[31.mai(2023)].økonomi.inspektør.personbeløp)
            with(utbetalingstidslinje.inspektør) {
                assertEquals(listOf(Begrunnelse.MinimumSykdomsgrad), begrunnelse(31.mai(2023)))
            }
            assertEquals(17, utbetalingstidslinje[31.mai(2023)].økonomi.inspektør.totalGrad)
        }
    }
}
