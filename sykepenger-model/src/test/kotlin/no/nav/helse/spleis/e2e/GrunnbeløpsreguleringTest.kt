package no.nav.helse.spleis.e2e

import java.time.Year
import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
            assertEquals(EventSubscription.SykefraværstilfelleIkkeFunnet(1.januar), observatør.sykefraværstilfelleIkkeFunnet.single())
        }
    }

    @Test
    fun `Selvstendig - Grunnbeløpsregulering på en utbetalt periode`() {
        tilGodkjenningSelvstendigMedFeilGrunnbeløp(
            pensjonsgivendeInntektÅr1 = 650_000.årlig,
            pensjonsgivendeInntektÅr2 = 650_000.årlig,
            pensjonsgivendeInntektÅr3 = 650_000.årlig,
            forventetSykepengegrunnlagFørGregulering = 561804.årlig,
            forventetSykepengegrunnlagEtterGregulering = 581298.årlig,
        )
        selvstendig {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterGrunnbeløpsregulering(1.mai)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
            assertEquals(581298.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(581298.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
        }
    }

    @Test
    fun `Selvstendig - Grunnbeløpsregulering på en utbetalt periode der spg er under 6G skal også medføre regulering ut fra gjeldende G`() {
        tilGodkjenningSelvstendigMedFeilGrunnbeløp(
            pensjonsgivendeInntektÅr1 = 450_000.årlig,
            pensjonsgivendeInntektÅr2 = 450_000.årlig,
            pensjonsgivendeInntektÅr3 = 450_000.årlig,
            forventetSykepengegrunnlagFørGregulering = 460589.0.årlig,
            forventetSykepengegrunnlagEtterGregulering = 476571.0.årlig
        )

        selvstendig {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterGrunnbeløpsregulering(1.mai)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
            assertEquals(581298.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(476571.0.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
        }
    }

    @Test
    fun `Selvstendig - Grunnbeløpsregulering på en utbetalt periode der pensjonsgivende inntekt varierer over og under 6G de tre årene, og er over 6G`() {
        tilGodkjenningSelvstendigMedFeilGrunnbeløp(
            pensjonsgivendeInntektÅr1 = 650_000.årlig,
            pensjonsgivendeInntektÅr2 = 800_000.årlig,
            pensjonsgivendeInntektÅr3 = 2_000_000.årlig,
            år = 2026,
            forventetSykepengegrunnlagFørGregulering = Grunnbeløp.`6G`.beløp(1.januar(2026)),
            forventetSykepengegrunnlagEtterGregulering = Grunnbeløp.`6G`.beløp(1.mai(2026))
        )

        selvstendig {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(857914.0.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.beregningsgrunnlag)
            håndterGrunnbeløpsregulering(1.mai(2026))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
            assertEquals(Grunnbeløp.`6G`.beløp(1.mai(2026)), inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(Grunnbeløp.`6G`.beløp(1.mai(2026)), inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
            assertEquals(900026.0.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.beregningsgrunnlag)
        }
    }

    @Test
    fun `Selvstendig - Grunnbeløpsregulering på en utbetalt periode der pensjonsgivende inntekt varierer over og under 6G de tre årene, og er under 6G`() {
        tilGodkjenningSelvstendigMedFeilGrunnbeløp(
            pensjonsgivendeInntektÅr1 = 400_000.årlig,
            pensjonsgivendeInntektÅr2 = 800_000.årlig,
            pensjonsgivendeInntektÅr3 = 1_500_000.årlig,
            år = 2026,
            forventetSykepengegrunnlagFørGregulering = 760380.årlig,
            forventetSykepengegrunnlagEtterGregulering = 797704.årlig,
        )

        selvstendig {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(760380.0.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.beregningsgrunnlag)
            håndterGrunnbeløpsregulering(1.mai(2026))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING)
            assertTrue(observatør.utkastTilVedtakEventer.last().tags.contains("Grunnbeløpsregulering"))
            assertEquals(Grunnbeløp.`6G`.beløp(1.mai(2026)), inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(797704.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
            assertEquals(797704.årlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.beregningsgrunnlag)
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

    private fun tilGodkjenningSelvstendigMedFeilGrunnbeløp(
        pensjonsgivendeInntektÅr1: Inntekt,
        pensjonsgivendeInntektÅr2: Inntekt,
        pensjonsgivendeInntektÅr3: Inntekt,
        år: Int = 2018,
        forventetSykepengegrunnlagEtterGregulering: Inntekt,
        forventetSykepengegrunnlagFørGregulering: Inntekt,
        forventet6GEtterGregulering: Inntekt = Grunnbeløp.`6G`.beløp(1.mai(år)),
        forventet6GFørGregulering: Inntekt = Grunnbeløp.`6G`.beløp(1.januar(år)),
    ) {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
                1.mai(år) til 31.mai(år), pensjonsgivendeInntekter = listOf(
                Søknad.PensjonsgivendeInntekt(Year.of(år - 1), pensjonsgivendeInntektÅr3, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(år - 2), pensjonsgivendeInntektÅr2, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(år - 3), pensjonsgivendeInntektÅr1, INGEN, INGEN, INGEN, erFerdigLignet = true)
            ))
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            assertEquals(forventet6GEtterGregulering, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(forventetSykepengegrunnlagEtterGregulering, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
        }
        hackGrunnbeløp(fra = forventet6GEtterGregulering.årlig.toInt(), til = forventet6GFørGregulering.årlig.toInt()) // Hacker inn 2017-G
        selvstendig {
            assertEquals(forventet6GFørGregulering, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.`6G`)
            assertEquals(forventetSykepengegrunnlagFørGregulering, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.sykepengegrunnlag)
            håndterPåminnelse(1.vedtaksperiode, TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)
            nullstillTilstandsendringer()
        }
    }

    private fun hackGrunnbeløp(fra: Int, til: Int) {
        val serialisertPerson = testperson.dto().tilPersonData().tilSerialisertPerson()
        val json = serialisertPerson.json
            .replace("\"grunnbeløp\":$fra.0", "\"grunnbeløp\":$til.0")
            .replace("\"anvendtÅrligGrunnbeløp\":${fra.div(6)}.0", "\"anvendtÅrligGrunnbeløp\":${til.div(6)}.0")
        medJSONPersonTekst(json, serialisertPerson.skjemaVersjon)
    }
}
