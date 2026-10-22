package no.nav.helse.spleis.e2e.selvstendig

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SelvstendigTilstanderTilGeneriskeTest : AbstractDslTest() {
    @Test
    fun `påminnelse i selvstendig avventer infotrygdhistorikk går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        byttTilstand(AVVENTER_VILKÅRSPRØVING, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `reberegning i selvstendig avventer infotrygdhistorikk går til generisk avventer infotrygdhistorikk`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }
        byttTilstand(AVVENTER_VILKÅRSPRØVING, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `selvstendig avventer blokkerende periode går til generisk flyt når perioden foran er ferdig`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterForlengelsessøknadSelvstendig(februar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        byttTilstand(AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
        selvstendig {
            håndterPåminnelse(2.vedtaksperiode, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
            assertSisteTilstand(2.vedtaksperiode, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelserSelvstendig(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(
                2.vedtaksperiode,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
            )
        }
    }

    @Test
    fun `reberegning i selvstendig avventer vilkårsprøving går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }
        byttTilstand(AVVENTER_VILKÅRSPRØVING, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `løsning på vilkårsgrunnlag i selvstendig avventer vilkårsprøving går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }
        byttTilstand(AVVENTER_VILKÅRSPRØVING, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
        selvstendig {
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `reberegning i selvstendig avventer historikk går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_HISTORIKK, SELVSTENDIG_AVVENTER_HISTORIKK)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `løsning på ytelser i selvstendig avventer historikk går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_HISTORIKK, SELVSTENDIG_AVVENTER_HISTORIKK)
        selvstendig {
            håndterYtelserSelvstendig(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `reberegning i selvstendig avventer simulering går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_SIMULERING, SELVSTENDIG_AVVENTER_SIMULERING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_SIMULERING, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `løsning på simulering i selvstendig avventer simulering går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_SIMULERING, SELVSTENDIG_AVVENTER_SIMULERING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_SIMULERING)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `reberegning i selvstendig avventer godkjenning går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_GODKJENNING, SELVSTENDIG_AVVENTER_GODKJENNING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `godkjenning i selvstendig avventer godkjenning går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        byttTilstand(AVVENTER_GODKJENNING, SELVSTENDIG_AVVENTER_GODKJENNING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `reberegning i selvstendig til utbetaling går til generisk flyt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        byttTilstand(TIL_UTBETALING, SELVSTENDIG_TIL_UTBETALING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, flagg = setOf("ønskerReberegning"))
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `utbetalt i selvstendig til utbetaling går til generisk avsluttet`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        byttTilstand(TIL_UTBETALING, SELVSTENDIG_TIL_UTBETALING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `reberegning i selvstendig avsluttet går til generisk revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        byttTilstand(AVSLUTTET, SELVSTENDIG_AVSLUTTET)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, flagg = setOf("ønskerReberegning"))
            håndterYtelserSelvstendig(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        }
    }

    @Test
    fun `reberegning i selvstendig avventer revurdering går til generisk revurdering`() {
        reberegningIRevurderingstilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING)
    }

    @Test
    fun `reberegning i selvstendig avventer vilkårsprøving revurdering går til generisk revurdering`() {
        reberegningIRevurderingstilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `reberegning i selvstendig avventer historikk revurdering går til generisk revurdering`() {
        reberegningIRevurderingstilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `reberegning i selvstendig avventer simulering revurdering går til generisk revurdering`() {
        reberegningIRevurderingstilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `reberegning i selvstendig avventer godkjenning revurdering går til generisk revurdering`() {
        reberegningIRevurderingstilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING)
    }

    @Test
    fun `reberegning i selvstendig avventer revurdering til utbetaling går til generisk revurdering til utbetaling`() {
        selvstendigperiodeIRevurderingTilUtbetaling()
        byttTilstand(AVVENTER_REVURDERING_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING, flagg = setOf("ønskerReberegning"))
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `utbetalt i selvstendig avventer revurdering til utbetaling går til generisk revurdering`() {
        selvstendigperiodeIRevurderingTilUtbetaling()
        byttTilstand(AVVENTER_REVURDERING_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `løsning på ytelser i selvstendig avventer historikk revurdering går til generisk revurdering`() {
        avsluttetSelvstendigperiodeIRevurdering()
        byttTilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
        selvstendig {
            håndterYtelserSelvstendig(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        }
    }

    @Test
    fun `gjenopptakelse i selvstendig avventer revurdering går til generisk revurdering`() {
        avsluttetSelvstendigperiodeIRevurdering()
        byttTilstand(AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING)
        selvstendig {
            håndterAvbruttSøknad(mars)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    private fun reberegningIRevurderingstilstand(
        fra: TilstandType,
        gammel: TilstandType,
    ) {
        avsluttetSelvstendigperiodeIRevurdering()
        byttTilstand(fra, gammel)
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, gammel, flagg = setOf("ønskerReberegning"))
            assertTilstander(1.vedtaksperiode, gammel, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    private fun avsluttetSelvstendigperiodeIRevurdering() {
        avsluttetSelvstendigperiode()
        selvstendig {
            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET, flagg = setOf("ønskerReberegning"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    private fun selvstendigperiodeIRevurderingTilUtbetaling() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING, flagg = setOf("ønskerReberegning"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING)
        }
    }

    private fun avsluttetSelvstendigperiode() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    private fun byttTilstand(
        fra: TilstandType,
        til: TilstandType,
    ) {
        val serialisertPerson = dto().tilPersonData().tilSerialisertPerson()
        val fraTilstand = "\"tilstand\":\"${fra.name}\""
        assertTrue(fraTilstand in serialisertPerson.json) { "Fant ikke $fraTilstand i serialisert person" }
        val json = serialisertPerson.json.replace(fraTilstand, "\"tilstand\":\"${til.name}\"")
        medJSONPersonTekst(json, serialisertPerson.skjemaVersjon)
    }
}
