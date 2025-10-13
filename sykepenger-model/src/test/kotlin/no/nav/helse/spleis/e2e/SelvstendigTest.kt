package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
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
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

internal class SelvstendigTest : AbstractDslTest() {

    @Test
    fun `tar inn søknad uten ventetid, men forkaster perioden`() {
        selvstendig {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
                pensjonsgivendeInntekter = emptyList()
            )
            assertInfo("Søknaden har ikke ventetid", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tar inn fisker, men forkaster perioden`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.FISKER)
            assertInfo("Har ikke støtte for søknadstypen FISKER", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tar inn jordbruker, men forkaster perioden`() = Toggle.Jordbruker.disable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            assertInfo("Har ikke støtte for søknadstypen JORDBRUKER", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `jordbruker har 100 prosent dekningsgrad og egen klassekode`() = Toggle.Jordbruker.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1771, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk, linje.klassekode)
                }
            }
        }
    }

    @Test
    fun `tar inn annet, men forkaster perioden`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.ANNET)
            assertInfo("Har ikke støtte for søknadstypen ANNET", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Kaster ut selvstendigperiode når det finnes ghosts`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), type = Arbeidsforholdtype.ORDINÆRT))
            )

            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_IV_13, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Kaster ut søknader når det også er oppgitt lønnsinntekter`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                pensjonsgivendeInntekter = listOf(
                        Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INNTEKT, INGEN, INGEN, erFerdigLignet = true),
                        Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                        Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
                )
            )
            assertFunksjonellFeil(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene sendes til infotrygd`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), 31.oktober(2017), Arbeidsforholdtype.FRILANSER),
                )
            )
            assertFunksjonellFeil(Varselkode.RV_IV_3.varseltekst, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                TIL_INFOTRYGD
            )
        }
    }

    @Test
    fun `Verifiserer sykdomstidslinje for selvstendig`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toString())

        }
    }

    @Test
    fun `Overstyrer tidslinje i halen i avventer godkjenning`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
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
            håndterSøknadSelvstendig(januar)
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
            håndterSøknadSelvstendig(januar)
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
            håndterSøknadSelvstendig(januar)

            assertEquals(1.januar til 16.januar, inspektør.ventetid(1.vedtaksperiode))

        }
    }

    @Test
    fun `opptjeningVurdertOk lagres på behandlingen når det ikke er fravær før sykmelding`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, fraværFørSykmelding = false)

            assertTrue(inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.single().endringer.last().forberedendeVilkårsgrunnlag!!.erOpptjeningVurdertOk)
        }
    }

    @Test
    fun `perioden kastes ut når det er fravær før sykmelding`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, fraværFørSykmelding = true)

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonelleFeil()
        }
    }

    @Test
    fun `lager ikke forberedende vilkårsgrunnlag når spørsmål om fravær før sykmelding ikke stilles`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, fraværFørSykmelding = false)
            håndterSøknadSelvstendig(februar)

            assertNull(inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.single().endringer.last().forberedendeVilkårsgrunnlag)
        }
    }

    @Test
    fun `selvstendigsøknad med færre inntekter enn 3 år kastes ut`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                pensjonsgivendeInntekter = listOf(
                        Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                        Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
                )
            )
            assertFunksjonelleFeil(1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `selvstendigsøknad kastes ut frem til vi støtter det`() = Toggle.SelvstendigNæringsdrivende.disable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)

        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt under 6G og uten forskring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
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

        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt over 6G og uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                pensjonsgivendeInntekter = listOf(
                        Søknad.PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                        Søknad.PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                        Søknad.PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
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

        }
    }

    @Test
    fun `To selvstendigsøknader`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterSøknadSelvstendig(mars, ventetid = 1.mars til 16.mars)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))

        }
    }

    @Test
    fun `Overstyrer tidslinje etter fattet vedtak`() {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((1.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET)
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
            assertEquals(setOf(80), inspektør.sykdomstidslinje.inspektør.grader.values.toSet())

        }
    }

    @Test
    fun `korrigert søknad etter fattet vedtak`() {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            håndterSøknadSelvstendig(januar, sykdomsgrad = 80.prosent)

            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET)
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
            assertEquals(setOf(100, 80), inspektør.sykdomstidslinje.inspektør.grader.values.toSet())

        }
    }

    @Test
    fun `inntektsendringer etter fattet vedtak`() {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            håndterInntektsendringer(1.januar)

            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 20.januar, 31.januar, 1000.daglig)))
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, forventetPersonbeløp = 1417, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, forventetPersonbeløp = 617, subset = 20.januar til 31.januar)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING, SELVSTENDIG_AVVENTER_GODKJENNING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET)
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
        }
    }

    @Test
    fun `kombinert arbeidstaker og selvstendig`() {
        a1 {
            håndterSøknad(januar)
        }

        selvstendig {
            håndterSøknadSelvstendig(januar)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 {
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)), OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()))
            håndterVilkårsgrunnlag(
                vedtaksperiodeId = 1.vedtaksperiode,
                skatteinntekter = listOf(this.orgnummer to INNTEKT),
                arbeidsforhold = listOf(Triple(a1, LocalDate.EPOCH, null))
            )

            assertFunksjonellFeil(Varselkode.RV_IV_13, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
        selvstendig {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt avviklet bedrift`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, harOppgittAvvikling = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_47, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen er ny i arbeidslivet`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, harOppgittNyIArbeidslivet = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_48, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen har varig inntektsendring`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, harOppgittVarigEndring = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_49, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen har ventetidsforsikring`() {
        selvstendig {
            håndterSøknadSelvstendig(januar, harOppgittÅHaForsikring = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_50, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }
}
