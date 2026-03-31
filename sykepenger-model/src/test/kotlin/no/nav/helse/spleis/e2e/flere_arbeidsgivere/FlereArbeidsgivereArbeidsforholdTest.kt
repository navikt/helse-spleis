package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.ArbeidstakerOpptjeningView
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereArbeidsforholdTest : AbstractDslTest() {

    @Test
    fun `Filtrerer ut irrelevante arbeidsforhold per arbeidsgiver`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 20000.månedlig),
            )

            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(1.mars)
            assertNotNull(vilkårsgrunnlag)

            assertEquals(1, (vilkårsgrunnlag.inspektør.opptjening as ArbeidstakerOpptjeningView).arbeidsforhold.single { it.orgnummer == a1 }.ansattPerioder.size)
            assertEquals(1, (vilkårsgrunnlag.inspektør.opptjening as ArbeidstakerOpptjeningView).arbeidsforhold.single { it.orgnummer == a2 }.ansattPerioder.size)
        }
    }

    @Test
    fun `Vanlige forlengelser av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(1.april, 30.april))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Flere aktive arbeidsforhold, men kun 1 med inntekt, skal ikke få warning for flere arbeidsgivere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juli(2021), 31.juli(2021)))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juli(2021), 31.juli(2021), 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.juli(2021) til 16.juli(2021)),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 30000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, 2.januar(2020), null),
                    Triple(a2, 1.mai(2019), null)
                ),
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Syk for a1, slutter i a1, syk for a2, a1 finnes ikke i Aa-reg lenger - ingen warning for manglende arbeidsforhold`() {
        /*
        * Siden vi ikke vet om arbeidsforhold for tidligere utbetalte perioder må vi passe på at ikke lar de periodene føre til advarsel på nye helt uavhengie vedtaksperioder
        * Sjekker kun arbeidsforhold for gjelende skjæringstidspunkt, derfor vil ikke mangel av arbeidsforhold for a1 skape problemer
        * */
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars(2017), 31.mars(2017)))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars(2017), 31.mars(2017), 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars(2017) til 16.mars(2017)),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 30000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(
                692.0,
                inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje[19.mars].økonomi.inspektør.arbeidsgiverbeløp?.daglig
            )
            val a2Linje = inspektør.utbetaling(0).arbeidsgiverOppdrag.last()
            assertEquals(17.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(692, a2Linje.beløp)
        }

        a1 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
        a2 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
    }

    @Test
    fun `lagrer kun arbeidsforhold som gjelder under skjæringstidspunkt`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 10000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 10000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, 1.januar),
                    Triple(a1, 1.januar, null), // Skal gjelde
                    Triple(a1, 28.februar, 1.mars), // Skal gjelde
                    Triple(a1, 1.mars, 31.mars), // Skal ikke gjelde fordi ansettelsetidspunktet er på skjæringstidspunktet
                    Triple(a1, 1.februar, 28.februar),
                    Triple(a1, 2.mars, 31.mars) // Gjelder ikke etter endring
                ),
            )
            val grunnlagsdata = inspektør.vilkårsgrunnlag(1.mars)!!
            assertEquals(4, (grunnlagsdata.inspektør.opptjening as ArbeidstakerOpptjeningView).arbeidsforhold.single { it.orgnummer == a1 }.ansattPerioder.size)
        }
    }

    @Test
    fun `opphold i arbeidsforhold skal ikke behandles som ghost`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 11400.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 11400.månedlig, a2 to 45000.månedlig),
            )
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSykmelding(Sykmeldingsperiode(2.februar, 20.februar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.februar, 20.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(2.februar til 17.februar),
                beregnetInntekt = 45000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to 11400.månedlig, a2 to 45000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, 31.januar),
                    Triple(a1, 1.mars, null),
                    Triple(a2, LocalDate.EPOCH, null)
                ),
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = 11400.månedlig,
                vedtaksperiodeId = 2.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                2.vedtaksperiode,
                skatteinntekter = listOf(a1 to 11400.månedlig, a2 to 45000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, 31.januar),
                    Triple(a1, 1.mars, null),
                    Triple(a2, LocalDate.EPOCH, null)
                ),
            )
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_8, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            val utbetaling = inspektør.utbetaling(0)
            val linje = utbetaling.arbeidsgiverOppdrag.linjerUtenOpphør().single()
            assertEquals(100, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje[20.februar].økonomi.brukTotalGrad { totalGrad -> totalGrad })
            assertEquals(
                2077,
                linje.beløp
            ) // Ikke cappet på 6G, siden personen ikke jobber hos a1 ved dette skjæringstidspunktet
            assertEquals(18.februar, linje.fom)
            assertEquals(20.februar, linje.tom)
        }
    }

    @Test
    fun `Vedtaksperioder med flere arbeidsforhold fra Aa-reg skal ha inntektskilde FLERE_ARBEIDSGIVERE`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to INNTEKT),
            )
            håndterYtelser(1.vedtaksperiode)

            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `Forlengelser skal ikke få warning på at arbeidsgiver ikke finnes i Aa-reg`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = emptyList(),
                arbeidsforhold = listOf(
                    Triple(a2, 1.januar(2017), null)
                ),
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)

            assertVarsel(RV_VV_1, 1.vedtaksperiode.filter())
        }
    }
}
