package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderKorrigertSoknad::class)
internal class RevurderKorrigertSøknadFlereArbeidsgivereTest : AbstractDslTest() {
    @Test
    fun `Korrigerende søknad hos en arbeidsgiver - setter i gang revurdering`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Arbeid(17.januar, 18.januar),
                Sykdom(24.januar, 25.januar, 50.prosent)
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertEquals(21, inspektør.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, inspektør.sykdomstidslinje.inspektør.dagteller[Arbeidsdag::class])
            assertEquals(8, inspektør.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar] is Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag)
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar] is Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad som strekker seg forbi vedtaksperioden - setter ikke i gang revurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 15.februar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.februar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = listOf(a1, a2).lagStandardSammenligningsgrunnlag(20000.månedlig, 1.januar)
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
            håndterSøknad(
                Sykdom(1.januar, 15.februar, 100.prosent),
                Arbeid(17.januar, 18.januar),
                Sykdom(24.januar, 25.januar, 50.prosent),
            )
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(23, inspektør.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertNull(inspektør.sykdomstidslinje.inspektør.dagteller[Arbeidsdag::class])
            assertEquals(15, inspektør.sykdomstidslinje.inspektør.dagteller[Dag.UkjentDag::class]) // TODO: ok?
            assertEquals(8, inspektør.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }
}
