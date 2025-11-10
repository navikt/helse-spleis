package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrInntektTest : AbstractDslTest() {

    @Test
    fun `skal kunne overstyre en inntekt i et enkelt case`() {
        a1 {
            // Hva gjør vi egentlig med overstyring? Skal man kunne sette opp inntekten uten å ha mottatt en ny inntektsmelding med nye refusjonsopplysninger?
            val fom = 1.januar(2021)
            val overstyrtInntekt = 32000.månedlig
            tilGodkjenning(fom til 31.januar(2021), 100.prosent)

            håndterInntektsmelding(
                listOf(fom til fom.plusDays(15)),
                beregnetInntekt = overstyrtInntekt,
                refusjon = Refusjon(overstyrtInntekt, null, emptyList())
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()

            assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING)

            assertInntektsgrunnlag(1.januar(2021), forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, overstyrtInntekt)
            }
        }
    }

    @Test
    fun `overstyre ghostinntekt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar)
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.desember(2017), null)
                )
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()

            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(a2, 500.daglig, emptyList()))
            )
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT)
                assertInntektsgrunnlag(a2, INGEN, 500.daglig, forventetKorrigertInntekt = 500.daglig, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `skal ikke hente registerdata for vilkårsprøving på nytt ved overstyring av inntekt`() {
        a1 {
            tilGodkjenning(januar)
            nullstillTilstandsendringer()
            håndterOverstyrInntekt(inntekt = 19000.månedlig, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
        }
    }

    @Test
    fun `Ved overstyring av inntekt til under krav til minste sykepengegrunnlag skal vi lage en utbetaling uten utbetaling`() {
        val OverMinstegrense = 50000.årlig
        val UnderMinstegrense = 46000.årlig

        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = OverMinstegrense
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterOverstyrInntekt(inntekt = UnderMinstegrense, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_1, 1.vedtaksperiode.filter())

            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(0, inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
            assertTrue(inspektør.utbetaling(1).erAvsluttet)
            assertTrue(inspektør.utbetaling(0).erForkastet)

        }
    }
}
