package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Selvstendigsøknad med flere typer pensjonsgivende inntekter`
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_46
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_START
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SelvstendigTest : AbstractDslTest() {

    @Test
    fun `Legger ikke på varsel for potensiell selvstendigghost når søknaden kommer mer enn 90 dager etter`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            nyttVedtak(mai)
        }
    }

    @Test
    fun `Legger på varsel for potensiell selvstendigghost hvis arbeidstakersøknad kommer inom 90 dager`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterSøknad(april)

            assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Kaster ut søknader som ikke overlapper men har kort gap`() {
        a1 {
            nyttVedtak(januar)
        }

        selvstendig {
            håndterFørstegangssøknadSelvstendig(2.februar til 28.februar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode, listOf(a1 to INNTEKT))
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_IV_13, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }


    @Test
    fun `Kaster ut overlappenden vedtaksperiode på tvers av yrkesaktivitetstype`() {
        a1 {
            håndterSøknad(1.januar til 14.januar)
            håndterSøknad(15.januar til 31.januar)
        }

        selvstendig {
            håndterFørstegangssøknadSelvstendig(14.januar til 31.januar)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `selvstendig løper videre til vilkårsprøving selv om det er en overlappende søknad som arbeidstaker`() {
        a1 {
            håndterSykmelding(januar)
        }

        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }

        assertForventetFeil(
            forklaring = "Selvstendig venter ikke på tur!",
            nå = {
                selvstendig {
                    assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
                }
            },
            ønsket = {
                selvstendig {
                    assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
                }

                a1 {
                    håndterSøknad(januar)
                    assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                }

                selvstendig {
                    assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
                }
            }
        )
    }

    @Test
    fun `revurdere flere fattet vedtak`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterForlengelsessøknadSelvstendig(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET)
            assertTilstand(2.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `revurdering mens avventer historikk revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })

            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 100) })

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering mens avventer simulering revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
            håndterYtelser(1.vedtaksperiode)
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 90) })

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering mens avventer godkjenning revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 90) })

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering mens avventer utbetaling revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 90) })

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
        }
    }

    @Test
    fun `påminnelse av utbetaling mens avventer utbetaling revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, sykdomsgrad = 80.prosent)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 90) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 100) })
            håndterUtbetalt(status = Oppdragstatus.OVERFØRT)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
            assertBehov(Aktivitet.Behov.Behovtype.Utbetaling) {
                håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING)
            }

            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertTilstander(1.vedtaksperiode, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering mens perioder allerede venter på revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterForlengelsessøknadSelvstendig(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterForlengelsessøknadSelvstendig(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING)

            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((31.januar.somPeriode()).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 100) })

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, SELVSTENDIG_AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `foreldet søknad`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, sendtTilNAVEllerArbeidsgiver = LocalDate.of(2018, 5, 1).atStartOfDay())
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_SØ_2), 1.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
        }
    }

    @Test
    fun `annullere selvstendig i en pågående revurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()


            håndterPåminnelse(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, flagg = setOf("ønskerReberegning"))
            håndterYtelser(1.vedtaksperiode)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `annullere selvstendig`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tar inn fisker, men forkaster perioden`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.FISKER)
            assertInfo("Har ikke støtte for søknadstypen FISKER", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tar inn annet, men forkaster perioden`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.ANNET)
            assertInfo("Har ikke støtte for søknadstypen ANNET", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Kaster ut selvstendigperiode når det finnes ghosts`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
    fun `inget varsel om flere pensjonsgivende inntekter i en forlengelse`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, 1.årlig, 2.årlig, 3.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true)
                )
            )

            assertVarsel(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)


            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterFørstegangssøknadSelvstendig(
                februar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, 1.årlig, 2.årlig, 3.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true)
                )
            )

            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `Varsel om flere pensjonsgivende inntekter i en forlengelse når begge søknader kommer før vi vilkårsvurderer den første`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, 1.årlig, 2.årlig, 3.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true)
                )
            )

            håndterFørstegangssøknadSelvstendig(
                februar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, 1.årlig, 2.årlig, 3.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, 2.årlig, 3.årlig, 4.årlig, erFerdigLignet = true)
                )
            )

            assertVarsel(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)


            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
        }
    }



    @ParameterizedTest
    @CsvSource(
        value = [
            // format: år, næringsinntekt, lønnsinntekt, lønnsinntektBarePensjonsdel, næringsinntektFraFiskeFangstEllerFamiliebarnehage for hvert av de tre årene, og forventet varsel
            "2017, 450000,  0,          0,      0, 2016, 450000,    0,      0, 0, 2015, 450000, 0,       0, 0,        false",
            "2017, 0,       450000,     0,      0, 2016, 0,         450000, 0, 0, 2015, 0,      450000,  0, 0,        false",
            "2017, 225000,  225000,     0,      0, 2016, 225000,    225000, 0, 0, 2015, 225000, 225000,  0, 0,        true",
            "2017, 450000,  0,          0,      0, 2016, 450000,    0,      0, 0, 2015, 0,      450000,  0, 0,        true",
            "2017, 225000,  0,          225000, 0, 2016, 450000,    0,      0, 0, 2015, 450000, 0,       0, 0,        true",
            "2017, 450000,  0,          0,      0, 2016, 450000,    0,      0, 0, 2015, 0,      0,       0, 450000,   true",
        ]
    )
    fun `beregner korrekt utbetaling for selvstendig med både nærings- og lønnsinntekt under 6G og uten forskring`(
        år1: Int, næringsinntekt1: Int, lønnsinntekt1: Int, lønnsinntektBarePensjonsdel1: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage1: Int,
        år2: Int, næringsinntekt2: Int, lønnsinntekt2: Int, lønnsinntektBarePensjonsdel2: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage2: Int,
        år3: Int, næringsinntekt3: Int, lønnsinntekt3: Int, lønnsinntektBarePensjonsdel3: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage3: Int,
        forventetVarsel: Boolean
    ) {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(år1), næringsinntekt1.årlig, lønnsinntekt1.årlig, lønnsinntektBarePensjonsdel1.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage1.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(år2), næringsinntekt2.årlig, lønnsinntekt2.årlig, lønnsinntektBarePensjonsdel2.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage2.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(år3), næringsinntekt3.årlig, lønnsinntekt3.årlig, lønnsinntektBarePensjonsdel3.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage3.årlig, erFerdigLignet = true)
                )
            )
            if (forventetVarsel) assertVarsel(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`, 1.vedtaksperiode.filter())
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
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            // format: år, næringsinntekt, lønnsinntekt, lønnsinntektBarePensjonsdel, næringsinntektFraFiskeFangstEllerFamiliebarnehage for hvert av de tre årene, og forventet varsel
            "2017, 1000000, 0,          0,      0, 2016, 1000000,   0,       0, 0, 2015, 1000000,   0,       0, 0,        false",
            "2017, 0,       1000000,    0,      0, 2016, 0,         1000000, 0, 0, 2015, 0,         1000000, 0, 0,        false",
            "2017, 500000,  500000,     0,      0, 2016, 500000,    500000,  0, 0, 2015, 500000,    500000,  0, 0,        true",
            "2017, 1000000, 0,          0,      0, 2016, 1000000,   0,       0, 0, 2015, 0,         1000000, 0, 0,        true",
            "2017, 500000,  0,          500000, 0, 2016, 1000000,   0,       0, 0, 2015, 1000000,   0,       0, 0,        true",
            "2017, 1000000, 0,          0,      0, 2016, 1000000,   0,       0, 0, 2015, 0,         0,       0, 1000000,  true",
        ]
    )
    fun `beregner korrekt utbetaling for selvstendig med både nærings- og lønnsinntekt over 6G og uten forskring`(
        år1: Int, næringsinntekt1: Int, lønnsinntekt1: Int, lønnsinntektBarePensjonsdel1: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage1: Int,
        år2: Int, næringsinntekt2: Int, lønnsinntekt2: Int, lønnsinntektBarePensjonsdel2: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage2: Int,
        år3: Int, næringsinntekt3: Int, lønnsinntekt3: Int, lønnsinntektBarePensjonsdel3: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage3: Int,
        forventetVarsel: Boolean
    ) {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
                januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(år1), næringsinntekt1.årlig, lønnsinntekt1.årlig, lønnsinntektBarePensjonsdel1.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage1.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(år2), næringsinntekt2.årlig, lønnsinntekt2.årlig, lønnsinntektBarePensjonsdel2.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage2.årlig, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(år3), næringsinntekt3.årlig, lønnsinntekt3.årlig, lønnsinntektBarePensjonsdel3.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage3.årlig, erFerdigLignet = true)
                )
            )
            if (forventetVarsel) assertVarsel(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(1023532.årlig)
                assertBeregningsgrunnlag(715713.årlig)
                assertSykepengegrunnlag(561804.årlig)
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
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
        }
    }

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), 31.oktober(2017), Arbeidsforholdtype.FRILANSER),
                )
            )
            assertVarsler(listOf(Varselkode.RV_IV_3), 1.vedtaksperiode.filter())
            assertTilstander(
                1.vedtaksperiode,
                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                SELVSTENDIG_AVVENTER_HISTORIKK
            )
        }
    }

    @Test
    fun `Verifiserer sykdomstidslinje for selvstendig`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toString())

        }
    }

    @Test
    fun `Overstyrer tidslinje i halen i avventer godkjenning`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
    fun `Overstyrer tidslinje i halen til annen ytelse i avventer godkjenning`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
    fun `Overstyrer hele perioden til annen ytelse`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
    fun `ventetid fra søknad lagres på behandlingen`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

        }
    }

    @Test
    fun `perioden kastes ut når det er fravær før sykmelding`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, fraværFørSykmelding = true)
            assertFunksjonellFeil(RV_SØ_46, 1.vedtaksperiode.filter())
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når spørsmål om fravær før sykmelding ikke stilles`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, fraværFørSykmelding = null)
            assertFunksjonellFeil(Varselkode.RV_OV_4, 1.vedtaksperiode.filter())
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `selvstendigsøknad med færre inntekter enn 3 år kastes ut`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
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
    fun `beregner korrekt utbetaling for selvstendig med inntekt under 6G og uten forskring`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

        }
    }

    @Test
    fun `beregner korrekt utbetaling for selvstendig med inntekt over 6G og uten forsikring`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(
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
                assertSelvstendigInntektsgrunnlag(1023532.årlig)
                assertBeregningsgrunnlag(715713.årlig)
                assertSykepengegrunnlag(561804.årlig)
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
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

        }
    }

    @Test
    fun `To selvstendigsøknader`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterFørstegangssøknadSelvstendig(mars)

            assertTilstander(1.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, SELVSTENDIG_START, SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.mars, listOf(1.mars til 16.mars))

        }
    }

    @Test
    fun `Overstyrer tidslinje etter fattet vedtak`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
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
            assertTilstander(
                1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING,
                SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET
            )
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
            assertEquals(setOf(80), inspektør.sykdomstidslinje.inspektør.grader.values.toSet())

        }
    }

    @Test
    fun `korrigert søknad etter fattet vedtak`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            håndterFørstegangssøknadSelvstendig(januar, sykdomsgrad = 80.prosent)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING,
                SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET
            )
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
            assertEquals(setOf(80), inspektør.sykdomstidslinje.inspektør.grader.values.toSet())

        }
    }

    @Test
    fun `inntektsendringer etter fattet vedtak`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()

            håndterInntektsendringer(1.januar)

            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 20.januar til 31.januar, 1000.daglig)))
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, forventetPersonbeløp = 1417, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, forventetPersonbeløp = 617, subset = 20.januar til 31.januar)
            assertTilstander(
                1.vedtaksperiode, SELVSTENDIG_AVSLUTTET, SELVSTENDIG_AVVENTER_REVURDERING, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING,
                SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING, SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, SELVSTENDIG_TIL_UTBETALING, SELVSTENDIG_AVSLUTTET
            )
            assertEquals(Utbetalingtype.REVURDERING, inspektør.utbetalinger(1.vedtaksperiode)[1].type)
        }
    }

    @Test
    fun `kombinert arbeidstaker og selvstendig, men bare innenfor tid uten nav-ansvar`() {
        selvstendig {
            håndterSykmelding(1.januar til 16.januar)
        }
        a1 {
            håndterSykmelding(1.januar til 16.januar)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(1.januar til 16.januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
        }
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
        selvstendig {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt avviklet bedrift`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, harOppgittAvvikling = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_47, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen er ny i arbeidslivet`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, harOppgittNyIArbeidslivet = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_48, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen har varig inntektsendring`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, harOppgittVarigEndring = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_49, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `varsel når bruker har oppgitt at hen har opprettholdt inntekt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, harOppgittOpprettholdtInntekt = true)
            assertVarsel(Varselkode.RV_SØ_51, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `kaster ut når bruker har oppgitt at hen har opphold i utlandet`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, harOppgittOppholdIUtlandet = true)
            assertFunksjonellFeil(Varselkode.RV_SØ_52, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `foreslår utbetaling på 80 prosent dekning i ventetid ved denne type forsikring`() = Toggle.SelvstendigForsikring.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode,
                selvstendigForsikring = SelvstendigForsikring(
                    virkningsdato = 10.oktober(2017),
                    opphørsdato = null,
                    type = SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn,
                    premiegrunnlag = 450000.årlig
                )
            )

            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            utbetalingstidslinje.subset(1.januar til 16.januar).forEach { assertUtbetalingsdag(it, Utbetalingsdag.Ventetidsdag::class, 100) }

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(1.januar til 31.januar, linje.periode)
                    assertEquals(1417, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            // Sjekk at forsikringen (dager nav overtar) er lagret på behandlingsendringen
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_AN_6)

        }
    }

    @Test
    fun `foreslår utbetaling på 100 prosent fra dag sytten`() = Toggle.SelvstendigForsikring.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode,
                selvstendigForsikring = SelvstendigForsikring(
                    virkningsdato = 10.oktober(2017),
                    opphørsdato = null,
                    type = SelvstendigForsikring.Forsikringstype.HundreProsentFraDagSytten,
                    premiegrunnlag = 450000.årlig
                )
            )

            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            utbetalingstidslinje.subset(1.januar til 16.januar).forEach { assertUtbetalingsdag(it, Utbetalingsdag.Ventetidsdag::class, 100) }

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1771, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            assertVarsler(1.vedtaksperiode, Varselkode.RV_AN_6)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
        }
    }

    @Test
    fun `foreslår utbetaling på 100 prosent fra dag 1`() = Toggle.SelvstendigForsikring.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode,
                selvstendigForsikring = SelvstendigForsikring(
                    virkningsdato = 10.oktober(2017),
                    opphørsdato = null,
                    type = SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn,
                    premiegrunnlag = 450000.årlig
                )
            )

            val utbetalingstidslinje = inspektør.utbetalinger(1.vedtaksperiode).single().utbetalingstidslinje
            utbetalingstidslinje.subset(1.januar til 16.januar).forEach { assertUtbetalingsdag(it, Utbetalingsdag.Ventetidsdag::class, 100) }

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(1.januar til 31.januar, linje.periode)
                    assertEquals(1771, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            assertVarsler(1.vedtaksperiode, Varselkode.RV_AN_6)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
        }
    }

    @Test
    fun `Kaster ut periode med selvstendig forsikring når toggle er av`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode,
                selvstendigForsikring = SelvstendigForsikring(
                    virkningsdato = 10.oktober(2017),
                    opphørsdato = null,
                    type = SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn,
                    premiegrunnlag = 450000.årlig
                )
            )
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                SELVSTENDIG_AVVENTER_HISTORIKK,
                TIL_INFOTRYGD,
                varselkode = Varselkode.RV_AN_6
            )
        }
    }

    @Test
    fun `Medlding til nav dager flytter skjæringstidspunkt`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(3.januar til 31.januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 2.januar).map { ManuellOverskrivingDag(it, Dagtype.MeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, forventetVenteperiode = listOf(1.januar til 16.januar))
        }
    }

    @Test
    fun `Melding til Nav dager med opphold lager ny ventetid`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(6.januar til 31.januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 2.januar).map { ManuellOverskrivingDag(it, Dagtype.MeldingTilNavdag) })
            håndterOverstyrTidslinje((4.januar til 5.januar).map { ManuellOverskrivingDag(it, Dagtype.MeldingTilNavdag) })
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 4.januar, forventetVenteperiode = listOf(4.januar til 19.januar))
        }
    }

    @Test
    fun `tilkommet inntekt - tilkommet på en annen arbeidsgiver`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode, inntekterForBeregning = listOf(
                InntekterForBeregning.Inntektsperiode(
                    inntektskilde = a1,
                    periode = 1.januar til 31.januar,
                    beløp = 1000.daglig
                )
            )
            )
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, forventetArbeidsgiverbeløp = 0, forventetArbeidsgiverRefusjonsbeløp = 0, forventetPersonbeløp = 0, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, forventetArbeidsgiverbeløp = 0, forventetArbeidsgiverRefusjonsbeløp = 0, forventetPersonbeløp = 617, subset = 17.januar til 31.januar)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(460589.0.årlig)
                assertBeregningsgrunnlag(460589.0.årlig)
            }
        }
    }

    @Test
    fun `tilkommet inntekt - tilkommet på en selvstendig`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            håndterYtelserSelvstendig(
                1.vedtaksperiode, inntekterForBeregning = listOf(
                InntekterForBeregning.Inntektsperiode(
                    inntektskilde = "SELVSTENDIG",
                    periode = 1.januar til 31.januar,
                    beløp = 1000.daglig
                )
            )
            )
            håndterSimulering(1.vedtaksperiode)

            assertUtbetalingsbeløp(1.vedtaksperiode, forventetArbeidsgiverbeløp = 0, forventetArbeidsgiverRefusjonsbeløp = 0, forventetPersonbeløp = 0, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, forventetArbeidsgiverbeløp = 0, forventetArbeidsgiverRefusjonsbeløp = 0, forventetPersonbeløp = 617, subset = 17.januar til 31.januar)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 0) {
                assertSelvstendigInntektsgrunnlag(460589.0.årlig)
                assertBeregningsgrunnlag(460589.0.årlig)
            }
        }
    }

}
