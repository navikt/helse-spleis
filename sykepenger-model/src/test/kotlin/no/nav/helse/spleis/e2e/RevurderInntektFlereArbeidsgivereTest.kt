package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde.SAKSBEHANDLER
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
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
            // TODO: 🤔 Her er det ikke juridisk avklart om vi får lov til å trekke tilbake penger fra ag2 💸
            assertDag(17.januar, 1064.0.daglig, aktuellDagsinntekt = 32000.månedlig, personbeløp = INGEN)
        }
    }
    @Test
    fun `under 6G -- revurder inntekt opp på a1 gir brukerutbetaling når refusjon er uendret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 15000.månedlig)
        a1 { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }
        a2 { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }

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
        }
    }
    @Test
    fun `under 6G -- revurder inntekt opp på a1 gir økt arbeidsgiverutbetaling når refusjon er endret`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar, inntekt = 15000.månedlig)
        a1 { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }
        a2 { assertDag(17.januar, 692.0.daglig, aktuellDagsinntekt = 15000.månedlig, personbeløp = INGEN) }

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
        }
    }

    @Test
    fun `3 arbeidsgivere`() {
        (a1 og a2 og a3).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterOverstyrInntekt(1.januar, 25000.månedlig)
        }
        // TODO
    }

    @Test
    fun `a1 er avsluttet og a2 er til godkjenning -- revurderer a1`() {
        // TODO
    }

    @Test
    fun `a1 er avsluttet og a2 er til godkjenning -- overstyrer a2`() {
        // TODO
    }

    @Test
    fun `revurderer forlengelse`() {
        // TODO
    }

    @Test
    fun `revurder inntekt i AvventerHistorikkRevurdering`() {
        // TODO
    }
    @Test
    fun `revurder inntekt i AvventerSimuleringRevurdering`() {
        // TODO
    }
    @Test
    fun `revurder inntekt i AvventerGjennomførtRevurdering`() {
        // TODO
    }

    @Test
    fun `revurderer tidligere skjæringstidspunkt`() {
        // TODO
    }

    @Test
    fun `noe med 25 warning`() {
        // TODO
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

    private fun TestPerson.TestArbeidsgiver.assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = Inntekt.INGEN, aktuellDagsinntekt: Inntekt = Inntekt.INGEN) {
        inspektør.sisteUtbetalingUtbetalingstidslinje()[dato].let {
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
            assertEquals(aktuellDagsinntekt, it.økonomi.inspektør.aktuellDagsinntekt)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.utbetalinger.last().inspektør.nettobeløp)
    }
}
