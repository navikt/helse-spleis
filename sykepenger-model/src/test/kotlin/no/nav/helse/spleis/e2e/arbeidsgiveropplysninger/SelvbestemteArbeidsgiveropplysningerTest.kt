package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.LovligFravaer
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AO_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvbestemteArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `Selvbestemt Arbeidsgiveropplysninger med reduksjon av AGP som er sendt på andre AUU biter også på den første`() {
        a1 {

            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 10.januar)

            håndterSelvbestemtArbeidsgiveropplysninger(vedtaksperiodeId = 2.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer")
            assertEquals(listOf(1.januar til 5.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
            assertVarsel(RV_AO_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Selvbestemt Arbeidsgiveropplysninger med ikke utbetalt AGP som er sendt på andre AUU biter også på den første`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 10.januar)

            håndterSelvbestemtArbeidsgiveropplysninger(vedtaksperiodeId = 2.vedtaksperiode, arbeidsgiverperioder = emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer")
            assertEquals(listOf(1.januar til 5.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
            assertVarsel(RV_AO_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Selvbestemt Arbeidsgiveropplysninger med delvis ikke utbetalt AGP som er sendt på andre AUU biter også på den første`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 10.januar)

            håndterSelvbestemtArbeidsgiveropplysninger(vedtaksperiodeId = 2.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 4.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer")
            assertEquals(listOf(5.januar til 5.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            assertEquals(listOf(6.januar til 10.januar), inspektør.dagerNavOvertarAnsvar(2.vedtaksperiode))

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
            assertVarsel(RV_AO_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Selvbestemte Arbeidsgiveropplysninger med redusert AGP biter på ren AUU med hele AGP i forkant`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)

            håndterSøknad(17.januar til 31.januar)

            håndterSelvbestemtArbeidsgiveropplysninger(vedtaksperiodeId = 2.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer")
            assertEquals(listOf(1.januar til 16.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            assertEquals(emptyList<Periode>(), inspektør.dagerNavOvertarAnsvar(2.vedtaksperiode))

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(RV_AO_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `mottar selvbestemte arbeidsgiveropplysninger når vi ikke trenger en`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSelvbestemtArbeidsgiveropplysninger(1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(LovligFravaer),
                OppgittInntekt(INNTEKT * 1.25),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = 0.månedlig, endringer = emptyList())
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_8)
        }
    }

    @Test
    fun `selvbestemt inntektsmelding som kvitterer ut egenmeldingsdager`() {
        a1 {
            håndterSøknad(5.januar til 31.januar, egenmeldinger = listOf(1.januar til 4.januar))
            //simulerer at det har gått 3 måneder og vi ikke har fått inntektsmelding
            håndterPåminnelse(1.vedtaksperiode, tilstand = TilstandType.AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 5.januar, listOf(1.januar til 16.januar), listOf(1.januar til 4.januar))
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = INNTEKT, endringer = emptyList())
            )
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar), emptyList())
            assertVarsler(1.vedtaksperiode, RV_AO_3, Varselkode.RV_IV_10)
        }
    }

    @Test
    fun `mottar selvbestemte arbeidsgiveropplysninger hvor arbeidgiverperioden ikke er sortert kronologisk`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(
                    listOf(
                        5.januar til 18.januar,
                        2.januar til 3.januar
                    )
                ),
                OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = INNTEKT, endringer = emptyList())
            )
            assertEquals(listOf(
                2.januar til 3.januar,
                5.januar til 16.januar
            ), inspektør.venteperiode(1.vedtaksperiode))

            assertVarsel(RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `mottar selvbestemte arbeidsgiveropplysninger som korrigerer eksisterende`() {
        a1 {
            nyttVedtak(januar)
            håndterSelvbestemtArbeidsgiveropplysninger(1.vedtaksperiode,
                OppgittInntekt(INNTEKT * 1.25),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = 0.månedlig, endringer = emptyList())
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_4)
        }
    }

    @Test
    fun `selvbestemte arbeidsgiveropplysninger med begrunnelseForIkkeUtbetaltIAGP som treffer en AUU`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                IkkeUtbetaltArbeidsgiverperiode(begrunnelse = Arbeidsgiveropplysning.Begrunnelse.ManglerOpptjening),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = 0.månedlig, endringer = emptyList())
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_8)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
            assertEquals(listOf(1.januar til 16.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
        }
    }

    @Test
    fun `selvbestemte arbeidsgiveropplysninger opplyser om tidligere AGP som gjør at en AUU egentlig skal gi utbetaling`() {
        a1 {
            håndterSøknad(5.januar til 20.januar)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittRefusjon(beløp = 0.månedlig, endringer = emptyList()),
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
            assertEquals(listOf(1.januar til 16.januar), inspektør.venteperiode(1.vedtaksperiode))
        }
    }
}
