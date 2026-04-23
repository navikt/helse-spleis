package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Venteårsak.Companion.VILKÅRSPRØVING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.VedtaksperiodeVenterTest.Companion.assertVenter
import no.nav.helse.spleis.e2e.enesteGodkjenningsbehovSomFølgeAv
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereGhostTest : AbstractDslTest() {

    @Test
    fun `bruker avbryter søknad for én arbeidsgiver`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a2 {
            håndterAvbruttSøknad(januar)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `Søknad og IM fra ghost etter kort gap til A1 - så kommer søknad fra A1 som tetter gapet - da gjenbruker vi tidsnære opplysninger`() {
        val ghost = a2
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
        ghost.tilGodkjenning(10.februar til 28.februar)
        ghost {
            assertEquals(10.februar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)
        }
        nullstillTilstandsendringer()
        a1 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        }
        ghost {
            assertEquals(1.januar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE)
        }
        a1 {
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `blir syk fra ghost man har vært syk fra tidligere, og inntektsmeldingen kommer først`() {
        listOf(a1, a2).nyeVedtak(1.januar(2017) til 31.januar(2017))

        a1 {
            håndterSøknad(Sykdom(1.januar, fredag den 26.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterInntektsmelding(
                listOf(mandag den 29.januar til 13.februar)
            )
        }
        // IM replayes, og ettersom 27. og 28 blir friskedager pga. IM beregnes skjæringstidspunktet til 29.januar. Når A1 sin søknad kommer dekker den "hullet" med sykdom slik at skjæringstidspunktet blir 1.januar
        observatør.vedtaksperiodeVenter.clear()
        a2 {
            håndterSøknad(Sykdom(lørdag den 27.januar, 20.februar, 100.prosent))
            observatør.assertVenter(2.vedtaksperiode, venterPåHva = VILKÅRSPRØVING)
            assertEquals(29.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
        }
        a1 {
            håndterSøknad(Sykdom(lørdag den 27.januar, 20.februar, 100.prosent))
        }
        a2 {
            assertEquals(1.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertTrue(inspektør.refusjon(1.vedtaksperiode).isNotEmpty())
            assertTrue(inspektør.refusjon(2.vedtaksperiode).isNotEmpty())
        }
        a1 {
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `Ghost som sender IM hvor de opplyser om ikke fravær - men blir syk fra ghost etterpå allikevel`() {
        val ghost = a2

        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 31000.månedlig
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertBeregningsgrunnlag(INNTEKT * 2)
                assertSykepengegrunnlag(561804.årlig)
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        // Inntektsmelding fra Ghost vi egentlig ikke trenger, men de sender den allikevel og opplyser om IkkeFravaer...
        // denne vinner over skatt i inntektsturnering
        val ghostIM = ghost {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = 33000.månedlig,
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFravaer"
            ).let { MeldingsreferanseId(it) }
        }
        a1 {
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertBeregningsgrunnlag(INNTEKT * 2)
                assertSykepengegrunnlag(561804.årlig)
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        // Så kjem søknaden på ghosten læll
        val ghostSøknad = UUID.randomUUID()
        ghost {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), søknadId = ghostSøknad)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        ghost {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertBeregningsgrunnlag(INNTEKT + 31_000.månedlig)
                assertSykepengegrunnlag(561804.årlig)
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 31_000.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        ghost {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        ghost {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, 33000.månedlig, ghostIM.id)
            assertEquals(setOf(Dokumentsporing.søknad(MeldingsreferanseId(ghostSøknad))), inspektør.hendelser(1.vedtaksperiode))
        }
    }

    @Test
    fun `blir syk fra ghost`()  {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
        nullstillTilstandsendringer()
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())
        }
        a2 {
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
        nullstillTilstandsendringer()
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `blir syk fra ghost og inntektsmeldingen kommer først`() {
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
        nullstillTilstandsendringer()
        a2 {
            håndterInntektsmelding(
                listOf(1.februar til 16.februar)
            )
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `blir syk fra ghost annen måned enn skjæringstidspunkt etter at saksbehandler har overstyrt inntekten etter 8-28, 3 ledd bokstav b -- Ghost sender klassisk inntektsmelding`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(
                1.vedtaksperiode,
                a1, a2
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a2, INNTEKT * 1.1)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.februar til 16.februar),
                beregnetInntekt = INNTEKT
            )
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `blir syk fra ghost annen måned enn skjæringstidspunkt etter at saksbehandler har overstyrt inntekten etter 8-28, 3 ledd bokstav b -- Ghost svarer på etterspurte arbeidsgiveropplysninger`()  {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(
                1.vedtaksperiode,
                a1, a2
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a2, INNTEKT * 1.1)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.1, forventetKorrigertInntekt = INNTEKT * 1.1, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `Korrigerende refusjonsopplysninger på arbeidsgiver med skatteinntekt i sykepengegrunnlaget`()  {
        utbetalPeriodeMedGhost()
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        }
        val inntektsmelding = a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            assertBeløpstidslinje(Beløpstidslinje.fra(februar, INNTEKT, inntektsmelding.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
        val korrigerendeInntektsmelding = a2 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.februar til 16.februar),
                førsteFraværsdag = 20.februar
            )
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            assertBeløpstidslinje(
                Beløpstidslinje.fra(1.februar til 19.februar, INNTEKT, inntektsmelding.arbeidsgiver) + Beløpstidslinje.fra(20.februar til 28.februar, INNTEKT, korrigerendeInntektsmelding.arbeidsgiver),
                inspektør.refusjon(1.vedtaksperiode)
            )
        }
    }

    @Test
    fun `ghost n stuff`() {
        utbetalPeriodeMedGhost()

        a1 {
            val a1Linje = inspektør.utbetaling(0).arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(1080, a1Linje.beløp)
        }
    }

    @Test
    fun `ny ghost etter tidligere ghostperiode`() {
        utbetalPeriodeMedGhost()

        a1 {
            håndterSykmelding(Sykmeldingsperiode(26.mars, 10.april))
            håndterSøknad(Sykdom(26.mars, 10.april, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 26.mars,
                refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList())
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            val førsteOppdrag = inspektør.utbetaling(0).arbeidsgiverOppdrag
            val a1Linje = førsteOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(1080, a1Linje.beløp)

            val andreOppdrag = inspektør.utbetaling(1).arbeidsgiverOppdrag
            val a1Linje2 = andreOppdrag.single()
            assertEquals(26.mars, a1Linje2.fom)
            assertEquals(10.april, a1Linje2.tom)
            assertEquals(1080, a1Linje2.beløp)
        }
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som starter etter skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars))
            håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 2.januar, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            val a1Linje = inspektør.utbetaling(0).arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(1431, a1Linje.beløp)
        }
    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som slutter før skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars))
            håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                månedligeInntekter = mapOf(
                    oktober(2017) to listOf(a1 to INNTEKT),
                    november(2017) to listOf(a1 to INNTEKT),
                    desember(2017) to listOf(a1 to INNTEKT, a2 to INNTEKT)
                ),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.desember(2017), 31.desember(2017))
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            val a1Linje = inspektør.utbetaling(0).arbeidsgiverOppdrag.single()
            assertEquals(17.januar, a1Linje.fom)
            assertEquals(15.mars, a1Linje.tom)
            assertEquals(1431, a1Linje.beløp)
        }
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom personen startet i jobb mer enn 2 måneder før skjæringstidspunktet og ikke har inntekt de 2 siste månedene`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 31.desember(2017), null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
        }
    }

    @Test
    fun `Tar med arbeidsforhold dersom personen startet i jobb mindre enn 2 måneder før skjæringstidspunktet, selvom det mangler inntekt de 2 siste månedene`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 2.januar, null)
                )
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom siste inntekt var 3 måneder før skjæringstidspunkt`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                månedligeInntekter = mapOf(
                    desember(2017) to listOf(a1 to INNTEKT, a2 to INNTEKT),
                    januar(2018) to listOf(a1 to INNTEKT),
                    februar(2018) to listOf(a1 to INNTEKT),
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.mars, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
        }
    }

    @Test
    fun `bruker har fyllt inn andre inntektskilder i søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(
                Sykdom(1.mars, 31.mars, 100.prosent),
                andreInntektskilder = true
            )
            assertFunksjonellFeil(RV_SØ_10, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Forlengelse av en ghostsak skal ikke få warning - stoler på avgjørelsen som ble tatt i førstegangsbehandlingen`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `tar med arbeidsforhold i vilkårsgrunnlag som startet innen 2 mnd før skjæringstidspunkt, selvom vi ikke har inntekt`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.november(2017), null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, 30_000.månedlig)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `overstyrer inntekt dersom det ikke er rapportert inn inntekt enda`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsforhold(1.januar, OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "Jeg, en saksbehandler, overstyrte pga 8-15"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, 30_000.månedlig)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
            }
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `skal ikke gå til AvventerHistorikk uten IM fra alle arbeidsgivere om vi ikke overlapper med første vedtaksperiode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
            håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar))
        }
        a1 {
            håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(18.januar til 2.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar))
        }
        a1 {
            håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.februar til 16.februar)
            )
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar))
        }
        a1 {
            håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent))
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM (uferdig)`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        }
        a1 {
            håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar))
        }
        a1 {
            håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }

    @Test
    fun `deaktivert arbeidsforhold blir med i vilkårsgrunnlag`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.desember(2017), null)
                )
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            assertOrgnummereMedRelevanteArbeidsforholdFraGodkjenningsbehov(1.vedtaksperiode, listOf(a1, a2)) {
                håndterSimulering(1.vedtaksperiode)
            }
            val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
            håndterOverstyrArbeidsforhold(
                skjæringstidspunkt,
                OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                    a2,
                    true,
                    "forklaring"
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertOrgnummereMedRelevanteArbeidsforholdFraGodkjenningsbehov(1.vedtaksperiode, listOf(a1)) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertBeregningsgrunnlag(372000.årlig)
                assertSykepengegrunnlag(372000.årlig)
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen, deaktivert = true)
            }
        }
    }

    @Test
    fun `arbeidsgiver går fra å være ghost mens første arbeidsgiver står til godkjenning -- Ghost sender klassisk inntektsmelding`() {
        utbetalPeriodeMedGhost(tilGodkjenning = true)

        nyPeriode(16.mars til 31.mars, a1) // Forlengelse på a1
        nyPeriode(16.mars til 15.april, a2) // Går fra Ghost -> ikke ghost

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        // IM for tidligere ghost a2 sparker igang revurdering på a1
        a2 {
            håndterInntektsmelding(
                listOf(16.mars til 31.mars),
                førsteFraværsdag = 16.mars,
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList())
            )
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            // Her står saken nå (*NÅ*)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        // Vi har fortsatt ikke beregnet hva utbetalingen for a2 kommer til å bli, men saksbehandleren lurer på det allerede *NÅ*
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `arbeidsgiver går fra å være ghost mens første arbeidsgiver står til godkjenning -- Ghost svarer på etterspurte arbeidsgiveropplysninger`()  {
        utbetalPeriodeMedGhost(tilGodkjenning = true)

        nyPeriode(16.mars til 31.mars, a1) // Forlengelse på a1
        nyPeriode(16.mars til 15.april, a2) // Går fra Ghost -> ikke ghost

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        // IM for tidligere ghost a2 sparker _ikke_ igang revurdering på a1 fordi vi beholder inntektene som de var i sykepengegrunnlaget
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(16.mars til 31.mars),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `deaktivere arbeidsgiver med kort søknad`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 15.januar, 50.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertEquals(75, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[1.januar].økonomi.inspektør.totalGrad)
            håndterOverstyrArbeidsforhold(1.januar, OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    private fun utbetalPeriodeMedGhost(tilGodkjenning: Boolean = false) {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars))
            håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            if (tilGodkjenning) return@a1
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    private fun assertOrgnummereMedRelevanteArbeidsforholdFraGodkjenningsbehov(vedtaksperiodeId: UUID, expected: List<String>, block: () -> Unit) {
        val actual= enesteGodkjenningsbehovSomFølgeAv({vedtaksperiodeId}, block).event.orgnummereMedRelevanteArbeidsforhold.toList()
        assertEquals(expected, actual)
    }
}
