package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class InntektsmeldingMedAvvikE2ETest: AbstractDslTest() {

    @Test
    fun `Avviksvarsel på første periode som treffes av korrigerende inntektsmelding`() {
        a1 {
            nyttVedtak(1.januar, 31.januar, beregnetInntekt = 20000.månedlig)
            nyPeriode(1.februar til 28.februar)

            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 9000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
            assertIngenVarsel(RV_IV_2, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Avviksvarsel legges kun på perioden inntektsmeldingen treffer`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyPeriode(17.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterInntektsmelding(listOf(25.januar til 4.februar), beregnetInntekt = 9000.månedlig)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertVarsel(RV_IV_2, 2.vedtaksperiode.filter())
            assertIngenVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Avviksvarsel legges kun på perioden inntektsmeldingen treffer med flere arbeidsgivere`() {
        a1 {
            nyPeriode(1.januar til 20.januar)
        }

        a2 {
            nyPeriode(21.januar til 31.januar)
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }

        a1 {
            nyPeriode(21.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = listOf(a1, a2).lagStandardSammenligningsgrunnlag(20000.månedlig, 1.januar),
                inntektsvurderingForSykepengegrunnlag = listOf(a1, a2).lagStandardSykepengegrunnlag(20000.månedlig, 1.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            nyPeriode(1.februar til 28.februar)
            nullstillTilstandsendringer()
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a2 {
            nyPeriode(1.februar til 28.februar)
            nullstillTilstandsendringer()
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        a1 {
            håndterUtbetalt()
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 9000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
            assertIngenVarsel(RV_IV_2, 2.vedtaksperiode.filter())
            assertIngenVarsel(RV_IV_2, 3.vedtaksperiode.filter())
        }

        a2 {
            assertIngenVarsel(RV_IV_2, 1.vedtaksperiode.filter())
            assertIngenVarsel(RV_IV_2, 2.vedtaksperiode.filter())
        }
    }
}