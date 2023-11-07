package no.nav.helse.spleis.e2e.overgang_ny_ag

import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

internal class OvergangNyArbeidsgiverTest : AbstractDslTest() {

    @Test
    fun `overgang til ny arbeidsgiver - innenfor agp - reduksjon oppgitt`() {
        // Inntektsmelding-signal corner case
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent))
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.februar, begrunnelseForReduksjonEllerIkkeUtbetalt = "TidligereVirksomhet")
            assertFunksjonellFeil(Varselkode.RV_SV_2)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `overgang til ny arbeidsgiver - utenfor agp`() {
        val inntektA1 = 50000.månedlig
        val inntektA2 = 30000.månedlig
        val forventetSykepengegrunnlag = `6G`.beløp(1.januar)

        a1 {
            nyttVedtak(1.januar, 31.januar, beregnetInntekt = inntektA1)
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
            assertForventetFeil(
                forklaring = "må støtte tilkommen inntekt først",
                nå = {
                    assertFunksjonellFeil(Varselkode.RV_SV_2, 1.vedtaksperiode.filter())
                    assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
                },
                ønsket = {
                    håndterInntektsmelding(emptyList(), inntektA2, førsteFraværsdag = 1.februar, begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeOpptjening")
                    håndterYtelser(1.vedtaksperiode)
                    håndterSimulering(1.vedtaksperiode)
                    assertVarsel(Varselkode.RV_SV_2, 1.vedtaksperiode.filter())
                    checkNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode)).inspektør.sykepengegrunnlag.inspektør.also { sykepengegrunnlagInspektør ->
                        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                        assertEquals(forventetSykepengegrunnlag, sykepengegrunnlagInspektør.sykepengegrunnlag)
                        assertEquals(inntektA1, sykepengegrunnlagInspektør.beregningsgrunnlag)
                    }
                    inspektør.utbetaling(0).inspektør.also { utbetalingInspektør ->
                        utbetalingInspektør.utbetalingstidslinje.also { utbetalingstidslinje ->
                            utbetalingstidslinje[1.februar].also { dagen ->
                                val forventetTotalgrad = 37.5
                                val forventetUtbetaling = (forventetSykepengegrunnlag * forventetTotalgrad).reflection { _, _, _, dagligInt -> dagligInt }
                                assertInstanceOf(Utbetalingsdag.ArbeidsgiverperiodedagNav::class.java, dagen)
                                assertEquals(inntektA2, dagen.økonomi.inspektør.aktuellDagsinntekt)
                                assertEquals(forventetTotalgrad.toInt(), dagen.økonomi.inspektør.totalGrad)
                                assertEquals(INGEN, dagen.økonomi.inspektør.arbeidsgiverbeløp)
                                assertEquals(forventetUtbetaling, dagen.økonomi.inspektør.personbeløp)
                            }
                        }
                    }
                    assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
                }
            )
        }
    }
}