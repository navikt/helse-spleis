package no.nav.helse.spleis.e2e

import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.selvstendig
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_START
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvstendigTest : AbstractDslTest() {

    @Test
    fun `Overstyrer tidslinje i avventer godkjenning`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig),
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            håndterOverstyrTidslinje((25.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)
            assertEquals("VVVVVVV VVVVVVV VVSSSHH SSSYYYY YYY", inspektør(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `venteperiode fra søknad lagres på behandlingen`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
            )

            assertEquals(1.januar til 16.januar, inspektør.venteperiode(1.vedtaksperiode))
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendigsøknad med færre inntekter enn 3 år kastes ut`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                )
            )
            assertFunksjonelleFeil(1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendigsøknad kastes ut frem til vi støtter det`() = Toggle.SelvstendigNæringsdrivende.disable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar)
            )
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt under 6G og uten forskring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(460589.årlig)
            }
            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            val venteperiodedager = utbetalingstidslinje.filterIsInstance<Utbetalingsdag.Venteperiodedag>()

            assertEquals(16, venteperiodedager.size)
            assertEquals(true, venteperiodedager.all { it.økonomi.utbetalingsgrad == 0.prosent && it.økonomi.sykdomsgrad == 100.prosent })
            assertEquals(11, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavDag>().size)
            assertEquals(4, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavHelgDag>().size)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1417, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING,
                SELVSTENDIG_AVVENTER_GODKJENNING,
                SELVSTENDIG_TIL_UTBETALING,
                SELVSTENDIG_AVSLUTTET
            )
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt over 6G og uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(715713.årlig)
            }

            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            assertEquals(16, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.Venteperiodedag>().size)
            assertEquals(11, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavDag>().size)
            assertEquals(4, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavHelgDag>().size)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1729, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING,
                SELVSTENDIG_AVVENTER_GODKJENNING,
                SELVSTENDIG_TIL_UTBETALING,
                SELVSTENDIG_AVSLUTTET
            )
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Subsumerer 8-34 ledd 1 for selvstendig uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)

            val antallSubsumsjoner = { subsumsjonInspektør: SubsumsjonInspektør ->
                subsumsjonInspektør.antallSubsumsjoner(
                    paragraf = Paragraf.PARAGRAF_8_34,
                    versjon = 1.januar(2019),
                    ledd = Ledd.LEDD_1,
                    punktum = null,
                    bokstav = null
                )
            }
            assertSubsumsjoner { assertEquals(1, antallSubsumsjoner(this)) }
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `To selvstendigsøknader`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar)
            )
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.mars til 16.mars)
            )

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SØ_45, 2.vedtaksperiode.filter())
        }
    }
}
