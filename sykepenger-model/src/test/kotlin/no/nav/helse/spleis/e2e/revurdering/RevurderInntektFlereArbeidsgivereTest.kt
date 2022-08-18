package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde.SAKSBEHANDLER
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurdereInntektMedFlereArbeidsgivere::class)
internal class RevurderInntektFlereArbeidsgivereTest: AbstractDslTest() {

    @Test
    fun `over 6G -- revurder inntekt ned på a1 når begge er i Avsluttet`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 32000.månedlig)
        nullstillTilstandsendringer()
        a1 { assertDag(17.januar, 1081.0.daglig, aktuellDagsinntekt = 32000.månedlig) }
        a2 { assertDag(17.januar, 1080.0.daglig, aktuellDagsinntekt = 32000.månedlig) }

        a1 {
            håndterOverstyrInntekt(1.januar, 31000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertDag(17.januar, 1063.0.daglig, aktuellDagsinntekt = 31000.månedlig)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertDag(17.januar, 1098.0.daglig, aktuellDagsinntekt = 32000.månedlig)
        }

    }
    @Test
    fun `over 6G -- revurder inntekt opp på a1 påvirker ikke utbetaling når refusjon er uendret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 32000.månedlig)
        a1 { assertDag(17.januar, 1081.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN) }
        a2 { assertDag(17.januar, 1080.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN) }

        a1 {
            håndterOverstyrInntekt(1.januar, 33000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertDag(17.januar, 1081.0.daglig, aktuellDagsinntekt = 33000.månedlig, personbeløp = INGEN)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertDag(17.januar, 1080.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN)
            assertIngenInfo("Arbeidsgiveren som initierte revurderingen har trukket tilbake penger på en eller flere andre arbeidsgivere", AktivitetsloggFilter.person())
        }
    }
    @Test
    fun `over 6G -- revurder inntekt opp på a1 påvirker utbetaling når refusjon er endret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 32000.månedlig)
        a1 { assertDag(17.januar, 1081.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN) }
        a2 { assertDag(17.januar, 1080.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN) }

        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 33000.månedlig)
            håndterOverstyrInntekt(1.januar, 33000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertDag(17.januar, 1097.0.daglig, aktuellDagsinntekt = 33000.månedlig, personbeløp = INGEN)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            // TODO: 🤔 Her er det ikke juridisk avklart om vi får lov til å trekke tilbake penger fra ag2: https://trello.com/c/6dWvZ50u 💸
            assertDag(17.januar, 1064.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN)
            assertInfo("Arbeidsgiveren som initierte revurderingen har trukket tilbake penger på en eller flere andre arbeidsgivere", AktivitetsloggFilter.person())
        }
    }

    @Test
    fun `Å flytte penger fra arbeidsgiveroppdrag til personoppdrag skal ikke logge at arbeidsgiveren har fått trukket penger`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 15000.månedlig)
        a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 15000.månedlig,
                refusjon = Refusjon(7500.månedlig, opphørsdato = null)
            )
        }
        a1 {
            håndterOverstyrInntekt(1.januar, 15000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertIngenInfo("Arbeidsgiveren som initierte revurderingen har trukket tilbake penger på en eller flere andre arbeidsgivere", AktivitetsloggFilter.person())
        }
    }
    @Test
    fun `under 6G -- revurder inntekt opp på a1 gir brukerutbetaling når refusjon er uendret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 15000.månedlig)
        (a1 og a2) { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }

        a1 {
            håndterOverstyrInntekt(1.januar, 16500.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 16500.månedlig, personbeløp = 70.daglig)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN)
            assertIngenInfo("Arbeidsgiveren som initierte revurderingen har trukket tilbake penger på en eller flere andre arbeidsgivere", AktivitetsloggFilter.person())
        }
    }
    @Test
    fun `under 6G -- revurder inntekt opp på a1 gir økt arbeidsgiverutbetaling når refusjon er endret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 15000.månedlig)
        (a1 og a2) { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }

        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 16500.månedlig)
            håndterOverstyrInntekt(1.januar, 16500.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertDag(17.januar, 762.0.daglig, aktuellDagsinntekt = 16500.månedlig, personbeløp = INGEN)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN)
            assertIngenInfo("Arbeidsgiveren som initierte revurderingen har trukket tilbake penger på en eller flere andre arbeidsgivere", AktivitetsloggFilter.person())
        }
    }

    @Test
    fun `3 arbeidsgivere -- justerer inntekten ned på a1`() {
        (a1 og a2 og a3).nyeVedtak(1.januar til 31.januar, inntekt = 32000.månedlig)
        nullstillTilstandsendringer()
        a1 { assertDag(17.januar, 721.0.daglig, aktuellDagsinntekt = 32000.månedlig) }
        (a2 og a3) { assertDag(17.januar, 720.0.daglig, aktuellDagsinntekt = 32000.månedlig) }

        a1 {
            håndterOverstyrInntekt(1.januar, 31000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertDag(17.januar, 705.0.daglig, aktuellDagsinntekt = 31000.månedlig)
        }
        (a2 og a3) {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertDag(17.januar, 728.0.daglig, aktuellDagsinntekt = 32000.månedlig)
        }
    }

    @Test
    fun `a1 er avsluttet og a2 er i AvventerGodkjenning -- revurderer a1`() {
        (a1 og a2) {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        (a1 og a2) { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                    inntekter = listOf(
                        sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, INNTEKT.repeat(12)),
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, INNTEKT.repeat(3))
                    ), arbeidsforhold = emptyList()
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrInntekt(1.januar, 32000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `a1 er avsluttet og a2 er til AvventerGodkjenningRevurdering -- revurderer a1`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 32000.månedlig)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrInntekt(1.januar, 31000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrInntekt(1.januar, 31000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `a1 er avsluttet og a2 er til godkjenning -- overstyrer a2`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                    inntekter = listOf(
                        sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, INNTEKT.repeat(12)),
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, INNTEKT.repeat(3))
                    ), arbeidsforhold = emptyList()
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrInntekt(1.januar, 32000.månedlig)
            assertForventetFeil(
                forklaring = "Feature ikke støttet enda: https://trello.com/c/aiYZO1VK",
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
                }
            )
        }
        a1 {
            assertForventetFeil(
                forklaring = "Feature ikke støttet enda: https://trello.com/c/aiYZO1VK",
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                }
            )
        }
    }

    @Test
    fun `revurderer forlengelse`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        (a1 og a2).forlengVedtak(1.februar til 28.februar)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrInntekt(1.januar, 32000.månedlig)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `revurder inntekt når a1 står i AvventerHistorikkRevurdering`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 23000.månedlig)
            håndterOverstyrInntekt(1.januar, 22000.månedlig)
            håndterOverstyrInntekt(1.januar, 23000.månedlig)
            håndterYtelser(1.vedtaksperiode)

            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING
            )
            assertDag(17.januar, 1062.daglig, aktuellDagsinntekt = 23000.månedlig)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }
    @Test
    fun `revurder inntekt når a1 står i AvventerSimuleringRevurdering`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 23000.månedlig)
            håndterOverstyrInntekt(1.januar, 22000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterOverstyrInntekt(1.januar, 23000.månedlig)
            håndterYtelser(1.vedtaksperiode)

            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING
            )
            assertDag(17.januar, 1062.daglig, aktuellDagsinntekt = 23000.månedlig)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }
    @Test
    fun `revurder inntekt når a1 står i AvventerGodkjenningRevurdering`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        nullstillTilstandsendringer()
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 23000.månedlig)
            håndterOverstyrInntekt(1.januar, 22000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrInntekt(1.januar, 23000.månedlig)
            håndterYtelser(1.vedtaksperiode)

            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING
            )
            assertDag(17.januar, 1062.daglig, aktuellDagsinntekt = 23000.månedlig)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurderer tidligere skjæringstidspunkt`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        (a1 og a2).nyeVedtak(1.mars til 31.mars)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrInntekt(1.januar, 19000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertDag(17.januar, 877.daglig, aktuellDagsinntekt = 19000.månedlig)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertDag(17.januar, 923.daglig, aktuellDagsinntekt = 20000.månedlig)
        }
        (a1 og a2) {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }
    @Test
    fun `kun den arbeidsgiveren som har fått overstyrt inntekt som faktisk lagrer inntekten`() {
        a2 {
            nyttVedtak(1.januar(2017), 31.januar(2017), 100.prosent) // gammelt vedtak
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                    inntekter = listOf(
                        sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                    ), arbeidsforhold = emptyList()
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 25000.månedlig)
            assertAntallInntektsopplysninger(1, SAKSBEHANDLER)
        }
        a2 {
            assertAntallInntektsopplysninger(0, SAKSBEHANDLER)
        }
    }

    @Test
    fun `alle perioder for alle arbeidsgivere med aktuelt skjæringstidspunkt skal ha hendelseIden`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        val hendelseId = UUID.randomUUID()
        a1 {
            håndterOverstyrInntekt(1.januar, 25000.månedlig, hendelseId)
            assertHarHendelseIder(1.vedtaksperiode, hendelseId)
        }
        a2 {
            assertHarIkkeHendelseIder(1.vedtaksperiode, hendelseId)
        }
    }

    private fun TestPerson.TestArbeidsgiver.assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN, aktuellDagsinntekt: Inntekt = INGEN) {
        inspektør.sisteUtbetalingUtbetalingstidslinje()[dato].let {
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
            assertEquals(aktuellDagsinntekt, it.økonomi.inspektør.aktuellDagsinntekt)
        }
    }
}
