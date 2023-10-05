package no.nav.helse.serde.api.v2.buildere

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.september
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GenerasjonDTO
import no.nav.helse.serde.api.dto.Inntekt
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.Periodetilstand
import no.nav.helse.serde.api.dto.Periodetilstand.Annullert
import no.nav.helse.serde.api.dto.Periodetilstand.ForberederGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.IngenUtbetaling
import no.nav.helse.serde.api.dto.Periodetilstand.ManglerInformasjon
import no.nav.helse.serde.api.dto.Periodetilstand.RevurderingFeilet
import no.nav.helse.serde.api.dto.Periodetilstand.TilAnnullering
import no.nav.helse.serde.api.dto.Periodetilstand.TilGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.TilUtbetaling
import no.nav.helse.serde.api.dto.Periodetilstand.Utbetalt
import no.nav.helse.serde.api.dto.Periodetilstand.UtbetaltVenterPåAnnenPeriode
import no.nav.helse.serde.api.dto.Periodetilstand.VenterPåAnnenPeriode
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType.FORELDET_SYKEDAG
import no.nav.helse.serde.api.dto.Tidslinjeperiode
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype.FORLENGELSE
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.UberegnetVilkårsprøvdPeriode
import no.nav.helse.serde.api.dto.Utbetalingstatus
import no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
import no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
import no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
import no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.serde.api.dto.Utbetalingtype.ANNULLERING
import no.nav.helse.serde.api.dto.Utbetalingtype.REVURDERING
import no.nav.helse.serde.api.dto.Utbetalingtype.UTBETALING
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.GenerasjonerBuilder
import no.nav.helse.serde.api.speil.builders.VilkårsgrunnlagBuilder
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forkastAlle
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.søknadDTOer
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class GenerasjonerBuilderTest : AbstractEndToEndTest() {

    @Test
    fun `Manglende generasjon når det kommer IM som endrer AGP ved å endre dager i forkant av perioden`() {
        håndterSøknad(Sykdom(7.august, 20.august, 100.prosent))
        håndterSøknad(Sykdom(21.august, 1.september, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(24.juli til 25.juli, 7.august til 20.august))
        assertEquals("UUAARR AAAAARR ASSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        // 21 & 22.August utbetalingsdager

        håndterInntektsmelding(arbeidsgiverperioder = listOf(7.august til 22.august))
        assertEquals("AAAARR AAAAARR ASSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        // 21 & 22.August agp -- denne blir ikke en generasjon

        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(24.juli, Dagtype.Egenmeldingsdag),
            ManuellOverskrivingDag(25.juli, Dagtype.Egenmeldingsdag)
        ))

        håndterYtelser(2.vedtaksperiode)
        // 21 & 22.August utbetalingsdager

        generasjoner {
            assertForventetFeil(
                forklaring= """
                    Det blir ingen ny generasjon ved korrigert IM fordi det eneste som endrer seg
                    er utbetalingsdagtypen for 21 og 22. august. Vi lager kun ny generajon om sykdomstidslinjetypen endres,
                    men den er SYK både før og etter 🤔
                    Vilkårsgrunnlaget endrer seg heller ikke, ettersom det er samme inntekt i den korrigerende inntektsmeldingen
                """,
                nå = { assertEquals(2, size) },
                ønsket = { assertEquals(3, size) }
            )
        }
    }

    @Test
    fun `avvik i inntekt slik at dager avslås pga minsteinntekt`() {
        val beregnetInntekt = halvG.beløp(1.januar)
        nyttVedtak(1.januar, 31.januar, beregnetInntekt = beregnetInntekt)
        forlengVedtak(1.februar,  28.februar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt - 1.daglig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, size)
                beregnetPeriode(0) avType REVURDERING fra 1.februar til 28.februar medTilstand TilGodkjenning
                beregnetPeriode(1) avType REVURDERING fra 1.januar til 31.januar medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(2, size)
                beregnetPeriode(0) avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(1) avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `revurdere skjæringstidspunktet flere ganger før forlengelsene`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT - 1.daglig)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT - 2.daglig)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, size)
                uberegnetVilkårsprøvdPeriode(0) fra 1.mars til 31.mars medTilstand UtbetaltVenterPåAnnenPeriode
                uberegnetVilkårsprøvdPeriode(1) fra 1.februar til 28.februar medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(2) avType REVURDERING fra 1.januar til 31.januar medTilstand TilGodkjenning
            }
            1.generasjon {
                // fordi de to andre ikke ble utbetalt før det startet ny revurdering
                assertEquals(1, size)
                beregnetPeriode(0) avType REVURDERING fra 1.januar til 31.januar medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(3, size)
                beregnetPeriode(0) avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(1) avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(2) avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `revurdere skjæringstidspunktet flere ganger`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT - 1.daglig)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT - 2.daglig)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                uberegnetVilkårsprøvdPeriode(1) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(2) medTilstand TilGodkjenning
            }
            1.generasjon {
                assertEquals(3, size)
                beregnetPeriode(0) avType REVURDERING medTilstand Utbetalt
                beregnetPeriode(1) avType REVURDERING medTilstand Utbetalt
                beregnetPeriode(2) avType REVURDERING medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(3, size)
                beregnetPeriode(0) avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(1) avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(2) avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `happy case`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        generasjoner(a1) {
            assertEquals(1, this.size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt medPeriodetype FORLENGELSE avType UTBETALING fra 1.februar til 28.februar medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt medPeriodetype FØRSTEGANGSBEHANDLING avType UTBETALING fra 1.januar til 31.januar medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
        generasjoner(a2) {
            assertEquals(1, this.size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt medPeriodetype FORLENGELSE avType UTBETALING fra 1.februar til 28.februar medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt medPeriodetype FØRSTEGANGSBEHANDLING avType UTBETALING fra 1.januar til 31.januar medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `periodetype ved enkel revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT - 50.0.månedlig)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra 1.februar til 28.februar medTilstand UtbetaltVenterPåAnnenPeriode medPeriodetype FORLENGELSE
                beregnetPeriode(1) fra 1.januar til 31.januar medTilstand TilGodkjenning medPeriodetype FØRSTEGANGSBEHANDLING
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) fra 1.februar til 28.februar medTilstand Utbetalt medPeriodetype FORLENGELSE
                beregnetPeriode(1) fra 1.januar til 31.januar medTilstand Utbetalt medPeriodetype FØRSTEGANGSBEHANDLING
            }
        }
    }

    @Test
    fun `person med foreldet dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juni)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) fra 1.februar til 28.februar medTilstand ForberederGodkjenning medPeriodetype FORLENGELSE
                beregnetPeriode(1) harTidslinje (1.januar til 31.januar to FORELDET_SYKEDAG) medTilstand IngenUtbetaling medPeriodetype FØRSTEGANGSBEHANDLING
            }
        }
    }

    @Test
    fun `annullerer feilet revurdering`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje()
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)
        håndterAnnullerUtbetaling()

        generasjoner {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Overført avType ANNULLERING medTilstand TilAnnullering
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er IkkeGodkjent avType REVURDERING medTilstand RevurderingFeilet
            }
            2.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `arbeidsgivere uten vedtaksperioder filtreres bort`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        forkastAlle(hendelselogg)
        generasjoner {
            assertEquals(0, size)
        }
    }

    @Test
    fun `Akkumulerer inntekter fra a-orningen pr måned`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom), orgnummer = a1)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            beregnetInntekt = 1000.månedlig,
            refusjon = Inntektsmelding.Refusjon(
                beløp = 1000.månedlig,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            ),
            orgnummer = a1
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }, arbeidsforhold = emptyList()),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                Arbeidsforhold(a2, LocalDate.EPOCH, type = Arbeidsforholdtype.ORDINÆRT)
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false)

        generasjoner(a1) {
            0.generasjon {
                val periode = beregnetPeriode(0)
                val vilkårsgrunnlag = periode.vilkårsgrunnlag()
                val omregnetÅrsinntekt = vilkårsgrunnlag.inntekter.first { it.organisasjonsnummer == a2 }.omregnetÅrsinntekt
                assertEquals(3, omregnetÅrsinntekt?.inntekterFraAOrdningen?.size)
                assertTrue(omregnetÅrsinntekt?.inntekterFraAOrdningen?.all { it.sum == 1000.0 } ?: false)
            }
        }
    }

    @Test
    fun `tar med vilkårsgrunnlag med ikke-rapportert inntekt`() {
        // A2 må være først i listen for at buggen skal intreffe
        nyttVedtak(1.januar(2017), 31.januar(2017), 100.prosent, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                    }
                },
                emptyList()
            ),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Arbeidsforhold(a2, 1.desember(2017), null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        generasjoner(a1) {
            0.generasjon {
                val vilkårsgrunnlag = beregnetPeriode(0).vilkårsgrunnlag()
                val inntektsgrunnlag = vilkårsgrunnlag.inntekter.firstOrNull { it.organisasjonsnummer == a2 }
                assertEquals(
                    Inntekt(
                        Inntektkilde.IkkeRapportert,
                        0.0,
                        0.0,
                        null
                    ),
                    inntektsgrunnlag?.omregnetÅrsinntekt
                )
            }
        }
    }


    @Test
    fun `happy case med periode til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Ubetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand TilGodkjenning
            }
        }
    }

    @Test
    fun `happy case med to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `happy case med to perioder med gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (2.februar til 28.februar) medAntallDager 27 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `periode blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `periode blir revurdert og utbetalt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `forlengelse blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand ForberederGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder - første blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder - revurdering til godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand TilGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger, deretter blir andre revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(4, size)

            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand ForberederGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }

            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }

            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }

            3.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder med gap - siste blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (2.februar til 28.februar) medAntallDager 27 forkastet false medTilstand ForberederGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (2.februar til 28.februar) medAntallDager 27 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `én periode som blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Overført avType ANNULLERING fra (1.januar til 31.januar) medAntallDager 0 forkastet true medTilstand TilAnnullering
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder som blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterAnnullerUtbetaling()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Overført avType ANNULLERING fra (1.februar til 28.februar) medAntallDager 0 forkastet true medTilstand TilAnnullering
                beregnetPeriode(1) er Overført avType ANNULLERING fra (1.januar til 31.januar) medAntallDager 0 forkastet true medTilstand TilAnnullering
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder som blir annullert - deretter nye perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        nyttVedtak(1.april, 30.april)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.april til 30.april) medAntallDager 30 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Annullert avType ANNULLERING fra (1.februar til 28.februar) medAntallDager 0 forkastet true medTilstand Annullert
                beregnetPeriode(2) er Utbetalingstatus.Annullert avType ANNULLERING fra (1.januar til 31.januar) medAntallDager 0 forkastet true medTilstand Annullert
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to arbeidsgiverperioder - siste blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        håndterAnnullerUtbetaling()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Overført avType ANNULLERING fra (1.mars til 31.mars) medAntallDager 0 forkastet true medTilstand TilAnnullering
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        forlengVedtak(1.mars, 31.mars)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false  medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse som så blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje((1.mars til 31.mars).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.mars til 31.mars) medAntallDager 31 forkastet false medTilstand IngenUtbetaling
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `korte perioder - arbeidsgiversøknader`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))

        generasjoner {
            0.generasjon {
                uberegnetPeriode(0) fra (1.januar til 15.januar) medAntallDager 15 forkastet false medTilstand IngenUtbetaling
            }
        }
    }

    @Test
    fun `kort periode med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType UTBETALING fra (16.januar til 15.februar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
                uberegnetVilkårsprøvdPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false medTilstand IngenUtbetaling
            }
        }
    }

    @Test
    fun `kort periode med forlengelse og revurdering av siste periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje((13.februar til 14.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (16.januar til 15.februar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
                uberegnetVilkårsprøvdPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (16.januar til 15.februar) medAntallDager 31 forkastet false medTilstand Utbetalt
                uberegnetVilkårsprøvdPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false medTilstand IngenUtbetaling
            }
        }
    }

    @Test
    fun `to perioder etter hverandre, nyeste er i venter-tilstand`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 21.februar))
        håndterSøknad(Sykdom(1.februar, 21.februar, 100.prosent))

        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand TilGodkjenning
            }
        }
    }

    @Test
    fun `to førstegangsbehandlinger, nyeste er i venter-tilstand`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetPeriode(0) medTilstand ManglerInformasjon
                beregnetPeriode(1) er Ubetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand TilGodkjenning
            }
        }
    }

    @Test
    fun `tidligere generasjoner skal ikke inneholde perioder som venter eller venter på informasjon`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        generasjoner {
            0.generasjon {
                assertEquals(2, this.perioder.size)
                uberegnetPeriode(0) medTilstand ManglerInformasjon
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(1, this.perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `periode som har tilstand TIL_INFOTRYGD sendes ikke med til Speil`() {
        val fyller18November2018 = "02110075045".somPersonidentifikator()
        createTestPerson(fyller18November2018, 2.november(2000))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), fnr = fyller18November2018)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), fnr = fyller18November2018)

        assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        generasjoner {
            assertEquals(0, size)
        }
    }

    @Test
    fun `ventende periode etter førstegangsbehandling`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand ForberederGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `ventende periode etter revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medTilstand ForberederGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING fra (1.januar til 31.januar) medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(1, size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `ventende perioder med revurdert tidligere periode`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `periode uten utbetaling - kun ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                uberegnetPeriode(0) fra (1.januar til 31.januar) medAntallDager 31 forkastet false medTilstand IngenUtbetaling
            }
        }
    }

    @Test
    fun `får riktig aldersvilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0).assertAldersvilkår(true, 26)
                beregnetPeriode(1).assertAldersvilkår(true, 25)
            }
        }
    }

    @Test
    fun `får riktig sykepengedager-vilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
                beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            }
        }
    }

    @Test
    fun `får riktig vilkår per periode ved revurdering av siste periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0).assertAldersvilkår(true, 26)
                beregnetPeriode(1).assertAldersvilkår(true, 25)
                beregnetPeriode(0).assertSykepengedagerVilkår(29, 219, 1.januar(2019), 1.januar, true)
                beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
                beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            }
        }
    }

    @Test
    fun `får riktig vilkår per periode ved revurdering av første periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            0.generasjon {
                uberegnetVilkårsprøvdPeriode(0) medTilstand  UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1).assertAldersvilkår(true, 25)
                // Revurdering av tidligere periode medfører at alle perioder berørt av revurderingen deler den samme utbetalingen, og derfor ender opp med samme
                // gjenstående dager, forbrukte dager og maksdato. Kan muligens skrives om i modellen slik at disse tallene kan fiskes ut fra utbetalingen gitt en
                // periode
                beregnetPeriode(1).assertSykepengedagerVilkår(9, 239, 1.januar(2019), 1.januar, true)
            }
            1.generasjon {
                beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
                beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            }
        }
    }

    @Test
    fun `ta med personoppdrag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(0.månedlig, null)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        generasjoner {
            0.generasjon {
                assertEquals(1, perioder.size)
                assertEquals(0, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.arbeidsgiverbeløp)
                assertEquals(1431, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.personbeløp)
                assertEquals(0, beregnetPeriode(0).utbetaling.arbeidsgiverNettoBeløp)
                assertEquals(15741, beregnetPeriode(0).utbetaling.personNettoBeløp)
            }
        }
    }

    @Test
    fun `ag2 venter på ag1 mens ag1 er til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)

        generasjoner(a1) {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand TilGodkjenning
            }
        }
        generasjoner(a2) {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand VenterPåAnnenPeriode
            }
        }
    }

    @Test
    fun `periode med bare ferie`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), Ferie(1.februar, 20.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er GodkjentUtenUtbetaling medTilstand IngenUtbetaling
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `behandlingstyper i normal forlengelsesflyt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetPeriode(0) medTilstand ManglerInformasjon
                uberegnetPeriode(1) medTilstand ManglerInformasjon
            }
        }

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetPeriode(0) medTilstand VenterPåAnnenPeriode
                uberegnetPeriode(1) medTilstand ForberederGodkjenning
            }
        }

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                uberegnetVilkårsprøvdPeriode(1) medTilstand ForberederGodkjenning
            }
        }

        håndterYtelser(1.vedtaksperiode)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand ForberederGodkjenning
            }
        }

        håndterSimulering(1.vedtaksperiode)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand TilGodkjenning
            }
        }

        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand TilUtbetaling
            }
        }

        håndterUtbetalt()
        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand ForberederGodkjenning
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `Annullering av revurdering feilet`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Annullert avType ANNULLERING medTilstand Annullert
            }

            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er IkkeGodkjent avType REVURDERING medTilstand RevurderingFeilet
            }

            2.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `revurdering av tidligere skjæringstidspunkt - opphører refusjon som treffer flere perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(a1, INNTEKT, "", null, listOf(Triple(1.januar, null, INGEN)))
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType REVURDERING medTilstand Utbetalt fra (1.februar til 28.februar)
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING medTilstand Utbetalt fra (1.januar til 31.januar)
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt fra (1.februar til 28.februar)
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt fra (1.januar til 31.januar)
            }
        }
    }

    @Test
    fun `revurdering av tidligere skjæringstidspunkt - nyere revurdering med ingen endringer`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mai, 31.mai)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er GodkjentUtenUtbetaling avType REVURDERING medTilstand Utbetalt fra (1.mai til 31.mai)
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType REVURDERING medTilstand Utbetalt fra (1.januar til 31.januar)
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt fra (1.mai til 31.mai)
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt fra (1.januar til 31.januar)
            }
        }
    }

    @Test
    fun `revurdering feilet med flere perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        generasjoner {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) er IkkeGodkjent avType REVURDERING medTilstand RevurderingFeilet
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `utbetaling feilet`() {
        tilGodkjenning(1.januar, 31.januar, grad = 100.prosent, førsteFraværsdag = 1.januar)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(status = Oppdragstatus.FEIL)

        assertTilstand(1.vedtaksperiode, TIL_UTBETALING)
        generasjoner {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Overført avType UTBETALING medTilstand TilUtbetaling
            }
        }
    }

    @Test
    fun `overlappende periode flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)

        nyPeriode(1.februar til 28.februar, a1)
        nyPeriode(1.februar til 28.februar, a2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        generasjoner(a1) {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand TilGodkjenning
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
            }
        }
        generasjoner(a2) {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand UtbetaltVenterPåAnnenPeriode
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `lage generasjoner når a2 er i Avventer historikk revurdering og har blitt tildelt utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
        generasjoner(a1) {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
            }
        }
        generasjoner(a2) {
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand ForberederGodkjenning
            }
        }
    }

    @Test
    fun `revurdering av flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        generasjoner(a1) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand ForberederGodkjenning
            }
        }
        generasjoner(a2) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand UtbetaltVenterPåAnnenPeriode
            }
        }

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        generasjoner(a1) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand TilGodkjenning
            }
        }
        generasjoner(a2) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand UtbetaltVenterPåAnnenPeriode
            }
        }

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        generasjoner(a1) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
        generasjoner(a2) {
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand TilGodkjenning
            }
        }
    }

    @Test
    fun `flere revurderinger, deretter revurdering feilet`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser()
        håndterSimulering()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Dagtype.Feriedag)))
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand RevurderingFeilet
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `revurdering til kun ferie`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje((17.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Feriedag) })
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand IngenUtbetaling
            }

            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `annullering av revurdert periode i til godkjenning`() {
        nyttVedtak(1.mars, 31.mars)

        nyttVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `annullering etter utbetaling underkjent`() {
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengTilGodkjenning(1.mai, 31.mai)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, utbetalingGodkjent = false)

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `annullering av flere perioder`() {
        nyttVedtak(1.mars, 31.mars)

        nyttVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `annullering av enda flere perioder`() {
        nyttVedtak(1.mars, 31.mars)

        nyttVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.mai, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        nyttVedtak(1.juli, 31.juli)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(3.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Annullert
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Annullert
            }
            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `flere perioder der første blir annullert, deretter ny periode, deretter annullering igjen`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        nyttVedtak(1.mai, 31.mai)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(3.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Annullert
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt
            }
            2.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `flere perioder der første blir annullert, deretter ny periode, deretter annullering igjen 2`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        nyttVedtak(1.mai, 31.mai)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(3.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        generasjoner {
            assertEquals(4, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType REVURDERING
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType UTBETALING
            }
            2.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType UTBETALING
            }
            3.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }

        håndterAnnullerUtbetaling(fagsystemId = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.arbeidsgiverOppdrag.fagsystemId())
        håndterUtbetalt()

        generasjoner {
            assertEquals(5, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Annullert
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType REVURDERING
            }
            2.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Annullert
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType UTBETALING
            }
            3.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Annullert
                beregnetPeriode(2) medTilstand Utbetalt avType UTBETALING
            }
            4.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `revurdering av tidligere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                uberegnetVilkårsprøvdPeriode(1) medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }

        håndterYtelser(1.vedtaksperiode)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - før vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand VenterPåAnnenPeriode
                beregnetPeriode(1) avType UTBETALING medTilstand TilGodkjenning
                uberegnetVilkårsprøvdPeriode(2) medTilstand IngenUtbetaling
            }
        }

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertTilstander(3.vedtaksperiode,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    @Disabled("går igjennom denne med Maxi og Simen")
    fun `inntektsmelding for to korte perioder`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(24.januar, 30.januar))
        håndterSøknad(Sykdom(24.januar, 30.januar, 100.prosent))

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 24.januar)
        assertEquals("UUUUGG UUUUSHH SSSSSH? ??SSSHH SS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals(2.januar til 17.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertEquals(2.januar til 17.januar, inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        val arbeidsgiverperioden = inspektør.arbeidsgiverperioden(2.vedtaksperiode)!!
        assertFalse(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(17.januar.somPeriode()))
        assertTrue(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(18.januar.somPeriode()))

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) avType UTBETALING medTilstand TilGodkjenning
                uberegnetPeriode(1) medTilstand IngenUtbetaling
            }
        }

        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 2.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) avType UTBETALING medTilstand ForberederGodkjenning
                beregnetPeriode(1) avType UTBETALING medTilstand Utbetalt
            }
        }

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling kan forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, TIL_INFOTRYGD)

        generasjoner {
            assertEquals(0, size)
        }
    }

    @Test
    fun `avvist utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        generasjoner {
            assertEquals(0, size)
        }
    }

    @Test
    fun `Utbetalt periode i AvventerRevurdering skal mappes til UtbetaltVenterPåAnnenPeriode`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) medTilstand ForberederGodkjenning
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) medTilstand Utbetalt
                beregnetPeriode(1) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `out of order med gap`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)
        tilGodkjenning(1.januar, 31.januar, a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.mars til 31.mars) medTilstand UtbetaltVenterPåAnnenPeriode
                beregnetPeriode(1) er Ubetalt avType UTBETALING fra (1.januar til 31.januar) medTilstand TilGodkjenning
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
            }
        }

        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.mars til 31.mars) medTilstand TilGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 31.januar) medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `omgjøre kort periode til at nav utbetaler`() {
        nyPeriode(4.januar til 20.januar)
        håndterInntektsmelding(listOf(4.januar til 19.januar))

        håndterOverstyrTidslinje(4.januar.til(19.januar).map { ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100) })
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (4.januar til 20.januar) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `omgjøring av eldre kort periode`() {
        nyPeriode(5.januar til 19.januar, orgnummer = a1)
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        generasjoner {
            assertEquals(1, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
                uberegnetPeriode(1) fra (5.januar til 19.januar) medTilstand IngenUtbetaling
            }
        }

        håndterOverstyrTidslinje((1.januar til 4.januar).map {
            ManuellOverskrivingDag(it, Dagtype.Sykedag, 100)
        }, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.mars til 31.mars) medTilstand TilGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.januar til 19.januar) medTilstand Utbetalt
            }
            1.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `out of order som er innenfor agp så utbetales`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)
        nyPeriode(1.januar til 15.januar, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(2, perioder.size)
                uberegnetVilkårsprøvdPeriode(0) fra (1.mars til 31.mars) medTilstand ForberederGodkjenning
                uberegnetPeriode(1) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
                uberegnetPeriode(1) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
        }

        nyPeriode(16.januar til 31.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(3.vedtaksperiode, orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.mars til 31.mars) medTilstand TilGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (16.januar til 31.januar) medTilstand Utbetalt
                uberegnetVilkårsprøvdPeriode(2) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
                beregnetPeriode(1) fra (16.januar til 31.januar) medTilstand Utbetalt
                uberegnetVilkårsprøvdPeriode(2) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
            2.generasjon {
                assertEquals(1, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.mars til 31.mars) medTilstand Utbetalt
            }
        }
    }

    @Test
    fun `out of order som er innenfor agp - så nyere perioder`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)
        nyPeriode(1.januar til 15.januar, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        forlengVedtak(1.april, 10.april, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.april til 10.april) medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.mars til 31.mars) medTilstand Utbetalt
                uberegnetPeriode(2) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
        }

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(10.april, Dagtype.Feriedag)))
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)

        generasjoner {
            assertEquals(3, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Ubetalt avType REVURDERING fra (1.april til 10.april) medTilstand TilGodkjenning
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.mars til 31.mars) medTilstand Utbetalt
                uberegnetPeriode(2) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.april til 10.april) medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType REVURDERING fra (1.mars til 31.mars) medTilstand Utbetalt
                uberegnetPeriode(2) fra (1.januar til 15.januar) medTilstand IngenUtbetaling
            }
        }
    }

    @Test
    fun `tidligere periode med arbeid får samme arbeidsgiverperiode som nyere periode`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Arbeid(1.februar, 28.februar))

        nyttVedtak(2.mars, 31.mars)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        generasjoner {
            assertEquals(2, size)
            0.generasjon {
                assertEquals(3, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType REVURDERING fra (2.mars til 31.mars) medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType UTBETALING fra (1.februar til 28.februar) medTilstand Utbetalt
                beregnetPeriode(2) er GodkjentUtenUtbetaling avType UTBETALING fra (1.januar til 31.januar) medTilstand IngenUtbetaling
            }
            1.generasjon {
                assertEquals(2, perioder.size)
                beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType UTBETALING fra (2.mars til 31.mars) medTilstand Utbetalt
                beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType UTBETALING fra (1.januar til 31.januar) medTilstand IngenUtbetaling
            }
        }
    }

    private fun BeregnetPeriode.assertAldersvilkår(expectedOppfylt: Boolean, expectedAlderSisteSykedag: Int) {
        assertEquals(expectedOppfylt, periodevilkår.alder.oppfylt)
        assertEquals(expectedAlderSisteSykedag, periodevilkår.alder.alderSisteSykedag)
    }

    private fun BeregnetPeriode.assertSykepengedagerVilkår(
        expectedForbrukteSykedager: Int,
        expectedGjenståendeSykedager: Int,
        expectedMaksdato: LocalDate,
        expectedSkjæringstidspunkt: LocalDate,
        expectedOppfylt: Boolean
    ) {
        assertEquals(expectedForbrukteSykedager, periodevilkår.sykepengedager.forbrukteSykedager)
        assertEquals(expectedGjenståendeSykedager, periodevilkår.sykepengedager.gjenståendeDager)
        assertEquals(expectedMaksdato, periodevilkår.sykepengedager.maksdato)
        assertEquals(expectedSkjæringstidspunkt, periodevilkår.sykepengedager.skjæringstidspunkt)
        assertEquals(expectedOppfylt, periodevilkår.sykepengedager.oppfylt)
    }

    private class Arbeidsgivergenerasjoner(
        private val orgnummer: String,
        private val vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>,
        private val generasjoner: List<GenerasjonDTO>
    ) {
        val size = generasjoner.size

        fun Int.generasjon(assertBlock: GenerasjonDTO.() -> Unit) {
            require(this >= 0) { "Kan ikke være et negativt tall!" }
            generasjoner[this].run(assertBlock)
        }

        fun GenerasjonDTO.beregnetPeriode(index: Int): BeregnetPeriode {
            val periode = this.perioder[index]
            require(periode is BeregnetPeriode) { "Perioden ${periode::class.simpleName} er ikke en beregnet periode!" }
            return periode
        }

        fun GenerasjonDTO.uberegnetPeriode(index: Int): UberegnetPeriode {
            val periode = this.perioder[index]
            require(periode is UberegnetPeriode) { "Perioden ${periode::class.simpleName} er ikke en uberegnet periode!" }
            return periode
        }

        fun GenerasjonDTO.uberegnetVilkårsprøvdPeriode(index: Int): UberegnetVilkårsprøvdPeriode {
            val periode = this.perioder[index]
            require(periode is UberegnetVilkårsprøvdPeriode) { "Perioden ${periode::class.simpleName} er ikke en uberegnet vilkårsprøvd periode!" }
            return periode
        }

        infix fun <T : Tidslinjeperiode> T.medAntallDager(antall: Int): T {
            assertEquals(antall, sammenslåttTidslinje.size)
            return this
        }
        infix fun <T : Tidslinjeperiode> T.harTidslinje(dager: Pair<Periode, SykdomstidslinjedagType>): T {
            val (periode, dagtype) = dager
            val periodeUtenHelg = periode.filterNot { it.erHelg() }
            val tidslinjedager = this.sammenslåttTidslinje.filter { it.dagen in periodeUtenHelg }
            assertEquals(periodeUtenHelg.toList().size, tidslinjedager.size)
            assertTrue(tidslinjedager.all { it.sykdomstidslinjedagtype == dagtype })
            return this
        }

        fun BeregnetPeriode.vilkårsgrunnlag(): Vilkårsgrunnlag {
            return requireNotNull(vilkårsgrunnlag[this.vilkårsgrunnlagId]) { "Forventet å finne vilkårsgrunnlag for periode" }
        }

        infix fun <T : Tidslinjeperiode> T.forkastet(forkastet: Boolean): T {
            assertEquals(forkastet, this.erForkastet)
            return this
        }

        infix fun BeregnetPeriode.er(utbetalingstilstand: Utbetalingstatus): BeregnetPeriode {
            assertEquals(utbetalingstilstand, this.utbetaling.status)
            return this
        }

        infix fun BeregnetPeriode.avType(type: Utbetalingtype): BeregnetPeriode {
            assertEquals(type, this.utbetaling.type)
            return this
        }

        infix fun <T : Tidslinjeperiode> T.medTilstand(tilstand: Periodetilstand): T {
            assertEquals(tilstand, this.periodetilstand)
            return this
        }

        infix fun <T : Tidslinjeperiode> T.medPeriodetype(tidslinjeperiodetype: Tidslinjeperiodetype): T {
            assertEquals(tidslinjeperiodetype, this.periodetype)
            return this
        }

        infix fun <T : Tidslinjeperiode> T.fra(periode: Periode): T {
            assertEquals(periode.start, this.fom)
            assertEquals(periode.endInclusive, this.tom)
            return this
        }
        infix fun <T : Tidslinjeperiode> T.fra(fom: LocalDate): T {
            assertEquals(fom, this.fom)
            return this
        }
        infix fun <T : Tidslinjeperiode> T.til(tom: LocalDate): T {
            assertEquals(tom, this.tom)
            return this
        }
    }

    private fun generasjoner(organisasjonsnummer: String = ORGNUMMER, block: Arbeidsgivergenerasjoner.() -> Unit = {}) {
        val vilkårsgrunnlagHistorikkBuilderResult = VilkårsgrunnlagBuilder(person.inspektør.vilkårsgrunnlagHistorikk).build()
        val generasjonerBuilder = GenerasjonerBuilder(
            organisasjonsnummer,
            søknadDTOer,
            UNG_PERSON_FØDSELSDATO.alder,
            person.arbeidsgiver(organisasjonsnummer),
            vilkårsgrunnlagHistorikkBuilderResult
        )
        val generasjoner = generasjonerBuilder.build()
        val vilkårsgrunnlag = vilkårsgrunnlagHistorikkBuilderResult.toDTO()
        block(Arbeidsgivergenerasjoner(organisasjonsnummer, vilkårsgrunnlag, generasjoner))
    }
}
