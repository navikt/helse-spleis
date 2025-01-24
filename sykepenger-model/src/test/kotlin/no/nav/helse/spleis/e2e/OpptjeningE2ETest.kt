package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.UtbetalingInntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_3
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
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

            val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
            val sykepengegrunnlagInspektør = vilkårsgrunnlag.inntektsgrunnlag.inspektør

            assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
            assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
            assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
            assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
            assertInntektsgrunnlag(1.januar, a1, INNTEKT)
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

            assertEquals(0, inspektør.utbetaling(0).utbetalingstidslinje.inspektør.avvistDagTeller)
        }
    }
}
