package no.nav.helse.spleis.e2e

import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Søknad
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvstendigTest : AbstractDslTest() {

    @Test
    fun `selvstendigsøknad med færre inntekter enn 3 år gir varsel`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                )
            )
            assertVarsel(Varselkode.RV_IV_12, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendigsøknad kastes ut frem til vi støtter det`() = Toggle.SelvstendigNæringsdrivende.disable {
        selvstendig {
            håndterSøknad(januar)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt under 6G og uten forskring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig),
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(460589.årlig)
            }
            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(januar, linje.periode)
                    assertEquals(1417, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt over 6G og uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig),
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(selvstendig, 715713.årlig)
            }
            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(januar, linje.periode)
                    assertEquals(1729, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        }
    }

    @Test
    fun `To selvstendigsøknader`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(januar)
            håndterSøknad(mars)

            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        }
    }
}
