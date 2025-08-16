package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.YTELSE_FRA_OFFENTLIGE
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_3
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerOpptjening
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningE2ETest : AbstractDslTest() {

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
        }
        assertHarArbeidsforhold(1.januar, a1)
        assertHarIkkeArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer flere arbeidsforhold brukt til opptjening`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsler(listOf(Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
        }
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening om tilstøtende`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, 20.desember(2017), null),
                    Triple(a2, LocalDate.EPOCH, 19.desember(2017))
                )
            )
        }
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening ved overlapp`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, 1.desember(2017), null),
                    Triple(a2, LocalDate.EPOCH, 24.desember(2017))
                )
            )
        }
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `opptjening er ikke oppfylt siden det ikke er nok opptjeningsdager`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Triple(a1, 31.desember(2017), null)
                )
            )
            håndterYtelser(1.vedtaksperiode)

            assertAntallOpptjeningsdager(1)
            assertErIkkeOppfylt()
            assertVarsel(RV_OV_1, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `tar ikke med inntekter fra A-Ordningen dersom arbeidsforholdet kun er brukt til opptjening og ikke gjelder under skjæringstidspunktet`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars))
            håndterSøknad(1.januar til 15.mars)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }

        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                månedligeInntekter = mapOf(
                    desember(2017) to listOf(a1 to INNTEKT, a2 to INNTEKT),
                    november(2017) to listOf(a1 to INNTEKT),
                    oktober(2017) to listOf(a1 to INNTEKT),
                ),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.desember(2017), 31.desember(2017))
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
        }
    }

    @Test
    fun `Har ikke pensjonsgivende inntekt måneden før skjæringstidspunkt`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntekterForOpptjeningsvurdering = listOf(a1 to INGEN)
            )

            assertVarsel(RV_OV_3, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)

            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller)
        }
    }

    @Test
    fun `opptjening fra offentlig ytelse - arbeidsforhold opphørt før opptjeningstiden`() {
        setupOpptjeningFraOffentligYtelse(ansattTom = 31.januar)
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(1.vedtaksperiode, RV_OV_1)
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[8.mai]) {
                assertTrue(this is AvvistDag)
                erAvvistMed(ManglerOpptjening)
            }
        }
    }

    @Test
    fun `opptjening fra offentlig ytelse - arbeidsforhold aktivt under opptjeningstiden`() {
        setupOpptjeningFraOffentligYtelse(ansattTom = 31.mars)
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[8.mai]) {
                assertTrue(this is NavDag)
            }
        }
    }

    private fun setupOpptjeningFraOffentligYtelse(ansattTom: LocalDate) {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
        }
        a2 {
            håndterSøknad(22.april til 22.mai)
            håndterArbeidsgiveropplysninger(listOf(22.april til 7.mai))
            håndterVilkårsgrunnlag(
                arbeidsforhold = listOf(
                    Arbeidsforhold(orgnummer = a1, ansattFom = 1.januar, ansattTom = ansattTom, type = ORDINÆRT),
                    Arbeidsforhold(orgnummer = a2, ansattFom = 1.april, ansattTom = null, type = ORDINÆRT),
                ),
                inntekterForOpptjeningsvurdering = InntekterForOpptjeningsvurdering(
                    listOf(ArbeidsgiverInntekt(a1, inntekter = listOf(MånedligInntekt(mars(2018), INNTEKT, YTELSE_FRA_OFFENTLIGE, "ja", "beskrivelse"))))
                ),
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(a2, INNTEKT, 22.april)
            )
            håndterYtelser(1.vedtaksperiode)
        }
    }
}
