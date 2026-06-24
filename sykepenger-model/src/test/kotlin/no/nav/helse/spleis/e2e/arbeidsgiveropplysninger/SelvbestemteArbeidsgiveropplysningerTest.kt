package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.assertForventetFeil
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvbestemteArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `mottar selvbestemt inntektsmelding når vi ikke trenger en`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSelvbestemtArbeidsgiveropplysninger(1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(LovligFravaer),
                OppgittInntekt(INNTEKT * 1.25)
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_8)
        }
    }

    @Test
    fun `mottar selvbestemt inntektsmelding som korrigerer eksisterende`() {
        a1 {
            nyttVedtak(januar)
            håndterSelvbestemtArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT * 1.25))
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_4)
        }
    }

    @Test
    fun `selvbestemt inntektsmelding med begrunnelseForIkkeUtbetaltIAGP som treffer en AUU`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                IkkeUtbetaltArbeidsgiverperiode(begrunnelse = Arbeidsgiveropplysning.Begrunnelse.ManglerOpptjening)
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, RV_IM_8)
            assertForventetFeil(
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
                    assertEquals(emptyList<Periode>(), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
                    assertEquals(listOf(1.januar til 16.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
                }
            )
        }
    }

    @Test
    fun `selvbestemt inntektsmelding opplyser om tidligere AGP som gjør at en AUU egentlig skal gi utbetaling`() {
        a1 {
            håndterSøknad(5.januar til 20.januar)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            håndterSelvbestemtArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
            )
            assertVarsler(1.vedtaksperiode, RV_AO_3, Varselkode.RV_IM_24)
            assertForventetFeil(
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
                    assertEquals(listOf(5.januar til 20.januar), inspektør.venteperiode(1.vedtaksperiode))
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
                    assertEquals(listOf(1.januar til 16.januar), inspektør.venteperiode(1.vedtaksperiode))
                }
            )
        }
    }

}
