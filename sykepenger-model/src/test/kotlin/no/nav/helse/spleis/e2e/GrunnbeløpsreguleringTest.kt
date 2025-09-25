package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GrunnbeløpsreguleringTest : AbstractDslTest() {

    @Test
    fun `Grunnbeløpsregulering med allerede riktig G-beløp`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertEquals(561804.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            nullstillTilstandsendringer()
            inspektør.vilkårsgrunnlagHistorikkInnslag()
            håndterGrunnbeløpsregulering(1.januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInfo("Grunnbeløpet i sykepengegrunnlaget 2018-01-01 er allerede korrekt.", 1.vedtaksperiode.filter())
            assertEquals(561804.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertEquals(0, observatør.sykefraværstilfelleIkkeFunnet.size)
        }
    }

    @Test
    fun `sier ifra om det blir forsøkt grunnbeløpsregulert på sykefraværstilfelle som ikke finnes`() {
        a1 {
            håndterGrunnbeløpsregulering(1.januar)
            assertEquals(PersonObserver.SykefraværstilfelleIkkeFunnet(1.januar), observatør.sykefraværstilfelleIkkeFunnet.single())
        }
    }

    @Test
    fun `Grunnbeløpsregulering på en utbetalt periode`() {
        tilGodkjenningMedFeilGrunnbeløp()
        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterGrunnbeløpsregulering(1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
        }
    }

    @Test
    fun `Grunnbeløpsregulering på en periode som står til godkjenning`() {
        tilGodkjenningMedFeilGrunnbeløp()
        a1 {
            håndterGrunnbeløpsregulering(1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
        }
    }

    private fun tilGodkjenningMedFeilGrunnbeløp() {
        val riktig6G = 561804
        val feil6G = 555456
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 3,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertEquals(riktig6G.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
        }
        hackGrunnbeløp(fra = riktig6G, til = feil6G) // Hacker inn 2017-G
        a1 {
            assertEquals(feil6G.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            nullstillTilstandsendringer()
        }
    }

    private fun hackGrunnbeløp(fra: Int, til: Int) {
        val json = testperson.dto().tilPersonData().tilSerialisertPerson().json.replace("\"grunnbeløp\":$fra.0", "\"grunnbeløp\":$til.0")
        medJSONPersonTekst(json)
    }
}
