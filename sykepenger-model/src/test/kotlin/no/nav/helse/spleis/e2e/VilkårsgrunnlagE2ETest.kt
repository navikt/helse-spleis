package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagE2ETest : AbstractDslTest() {

    @Test
    fun `skjæringstidspunkt måneden før inntektsmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(26.januar, 8.februar))
            håndterSøknad(26.januar til 8.februar)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar))
            håndterSøknad(6.februar til 28.februar)
            håndterInntektsmelding(listOf(21.januar til 21.januar, 6.februar til 20.februar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `negativt omregnet årsinntekt for ghost-arbeidsgiver`() {
        a1 {

            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to (-500).månedlig),
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Forkaster ikke etterfølgende perioder dersom vilkårsprøving feiler pga minimum inntekt på første periode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
            håndterSøknad(1.januar til 17.januar)

            val arbeidsgiverperioder = listOf(1.januar til 16.januar)

            håndterInntektsmelding(
                arbeidsgiverperioder = arbeidsgiverperioder,
                beregnetInntekt = 1000.månedlig,
                refusjon = Inntektsmelding.Refusjon(1000.månedlig, null, emptyList()),
            )

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_1, 1.vedtaksperiode.filter())

            håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar))
            håndterSøknad(18.januar til 20.januar)

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
            )

            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE
            )
        }
    }

    @Test
    fun `Forkaster ikke vilkårsgrunnlag om det er en periode i AUU med samme skjæringstidspunkt som den som blir annullert`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyPeriode(17.januar til 31.januar)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertVilkårsgrunnlagFraSpleisFor(1.januar)

            håndterAnnullering(inspektør.sisteUtbetaling().utbetalingId)
            håndterUtbetalt()
            assertVilkårsgrunnlagFraSpleisFor(1.januar)
        }
    }

    @Test
    fun `nytt og eneste arbeidsforhold på skjæringstidspunkt`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening"
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INGEN),
                arbeidsforhold = listOf(Triple(a1, 1.januar, null))
            )
            assertVarsler(listOf(Varselkode.RV_IM_8, Varselkode.RV_VV_1, Varselkode.RV_OV_1), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Forkaster vilkårsgrunnlag når periode annulleres`() {
        a1 {
            nyttVedtak(januar)
            assertVilkårsgrunnlagFraSpleisFor(1.januar)
            håndterAnnullering(inspektør.sisteUtbetaling().utbetalingId)
            assertIngenVilkårsgrunnlagFraSpleis()
        }
    }
}
