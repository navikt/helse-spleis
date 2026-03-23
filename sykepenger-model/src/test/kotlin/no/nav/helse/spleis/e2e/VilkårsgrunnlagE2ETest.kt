package no.nav.helse.spleis.e2e

import OpenInSpanner
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Disabled
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

            håndterAnnullering(2.vedtaksperiode)
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
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_IM_8, Varselkode.RV_VV_1, Varselkode.RV_OV_1), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Forkaster vilkårsgrunnlag når periode annulleres`() {
        a1 {
            nyttVedtak(januar)
            assertVilkårsgrunnlagFraSpleisFor(1.januar)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertIngenVilkårsgrunnlagFraSpleis()
        }
    }

    @Disabled("Som best jeg klarte gjenskaper dette hendelsesforløp i prod, men det blir en annen tilstand i spannerish enn prod-caset?")
    @OpenInSpanner
    @Test
    fun `Rare ting skjer med korte perioder, out of order søknader, og inntektsmeldinger fra feil arbeidsgiver`() {
        val sfo = a2
        val skole = a1
        sfo {
            håndterSøknad(29.september(2025) til 26.oktober(2025))
            håndterSøknad(27.oktober(2025) til 11.november(2025))
            håndterSøknad(12.november(2025) til 27.november(2025))
        }
        skole {
            håndterSøknad(28.november(2025) til 17.desember(2025))
            håndterSøknad(18.desember(2025) til 5.januar(2026))
        }
        sfo {
            håndterSøknad(5.januar(2026) til 20.januar(2026))
            håndterSøknad(21.januar(2026) til 5.februar(2026))
            håndterInntektsmelding(listOf(29.september(2025) til 14.oktober(2025)))
        }
        skole {
            /*
            denne AGPen treffer jo ikke noen av perioden til skolen, som jeg skulle ønske at var mistenkelig,
            men skole og sfo er underenhet av samme hovedenhet, så egentlig så skal de jo dele AGP, men
            vi har vel ikke støtte for sånt?
             */
            håndterInntektsmelding(listOf(29.september(2025) til 14.oktober(2025)))
            håndterSøknad(6.februar(2026) til 25.februar(2026))
        }
        sfo {
            håndterSøknad(6.februar(2026) til 25.februar(2026))
        }
        skole {
            håndterSøknad(27.oktober(2025) til 11.november(2025))
            håndterSøknad(12.november(2025) til 27.november(2025))
        }
        sfo {
            håndterSøknad(28.november(2025) til 16.desember(2025))
            håndterSøknad(17.desember(2025) til 4.januar(2026))
        }
        skole {
            håndterSøknad(6.januar(2026) til 5.februar(2026))
            håndterSøknad(6.februar(2026) til 25.februar(2026))
        }
        sfo {
            håndterSøknad(26.februar(2026) til 17.mars(2026))
        }
        skole {
            håndterSøknad(26.februar(2026) til 17.mars(2026))
        }
    }
}
