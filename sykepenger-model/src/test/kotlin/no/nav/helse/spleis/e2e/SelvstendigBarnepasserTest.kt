package no.nav.helse.spleis.e2e

import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
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
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvstendigBarnepasserTest : AbstractDslTest() {

    @Test
    fun `Verifiserer sykdomstidslinje for selvstendig`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toString())

        }
    }

    @Test
    fun `Overstyrer tidslinje i halen i avventer godkjenning`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            håndterOverstyrTidslinje((1.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            assertEquals(setOf(80), inspektør.sykdomstidslinje.inspektør.grader.values.toSet())

        }
    }

    @Test
    fun `Overstyrer tidslinje i halen til annen ytelse i avventer godkjenning`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            håndterOverstyrTidslinje((25.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            assertEquals("VVVVVVV VVVVVVV VVNNNHH NNNXXXX XXX", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())

        }
    }

    @Test
    fun `Overstyrer hele perioden til annen ytelse`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            håndterOverstyrTidslinje((1.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            assertEquals("XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXX", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())

        }
    }

    @Test
    fun `ventetid fra søknad lagres på behandlingen`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)

            assertEquals(1.januar til 16.januar, inspektør.ventetid(1.vedtaksperiode))

        }
    }

    @Test
    fun `selvstendigsøknad med færre inntekter enn 3 år kastes ut`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN)
                )
            )
            assertFunksjonelleFeil(1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)

        }
    }

    @Test
    fun `selvstendigsøknad kastes ut frem til vi støtter det`() = Toggle.SelvstendigNæringsdrivende.disable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)

        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt under 6G og uten forskring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(460589.årlig)
            }
            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            val ventetidsdager = utbetalingstidslinje.filterIsInstance<Utbetalingsdag.Ventetidsdag>()

            assertEquals(16, ventetidsdager.size)
            assertEquals(true, ventetidsdager.all { it.økonomi.utbetalingsgrad == 0.prosent && it.økonomi.sykdomsgrad == 100.prosent })
            assertEquals(11, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavDag>().size)
            assertEquals(4, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavHelgDag>().size)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1417, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig, linje.klassekode)
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

        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt over 6G og uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER,
                pensjonsgivendeInntekter = listOf( // TODO Inntekt for barnepasser er ikke vanlig næringsinntekt, men i næringsinntektFraFiskeFangstEllerFamiliebarnehage
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig, INGEN, INGEN, INGEN)
                )
            )
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(715713.årlig)
            }

            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            assertEquals(16, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.Ventetidsdag>().size)
            assertEquals(11, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavDag>().size)
            assertEquals(4, utbetalingstidslinje.filterIsInstance<Utbetalingsdag.NavHelgDag>().size)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1729, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig, linje.klassekode)
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

        }
    }

    @Test
    fun `To selvstendigsøknader`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)
            håndterSøknadSelvstendig(mars, 1.mars til 16.mars, arbeidssituasjon = Søknad.Arbeidssituasjon.BARNEPASSER)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))

        }
    }
}
