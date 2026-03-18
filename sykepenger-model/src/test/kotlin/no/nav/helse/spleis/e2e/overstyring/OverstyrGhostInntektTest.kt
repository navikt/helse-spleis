package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OverstyrGhostInntektTest : AbstractDslTest() {
    @Test
    fun `Overstyrer ghost-inntekt -- happy case`() {

        a1 {
            tilOverstyring()
        }
        a2 {
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 500.månedlig)

        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, 500.månedlig, forventetKorrigertInntekt = 500.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }

        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `Ved overstyring av inntekt til under krav til minste sykepengegrunnlag skal vi lage en utbetaling uten utbetaling`() {
        a1 {
            tilOverstyring()
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = INGEN)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(a1, 3750.månedlig),
                    OverstyrtArbeidsgiveropplysning(a2, INGEN)
                )
            )

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_VV_2, Varselkode.RV_SV_1), 1.vedtaksperiode.filter())
            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(0, inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
            Assertions.assertTrue(inspektør.utbetaling(1).erAvsluttet)
            Assertions.assertTrue(inspektør.utbetaling(0).erForkastet)
        }
    }

    @Test
    fun `Overstyr ghost-inntekt -- ghost har ingen inntekt fra før av`() {
        a1 {
            tilOverstyring()
        }
        a2 {
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 500.månedlig)
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, 500.månedlig, forventetKorrigertInntekt = 500.månedlig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }

        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())

        }
    }

    private fun TestPerson.TestArbeidsgiver.tilOverstyring(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            vedtaksperiodeId = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
    }
}
