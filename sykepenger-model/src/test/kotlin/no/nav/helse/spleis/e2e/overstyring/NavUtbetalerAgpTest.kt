package no.nav.helse.spleis.e2e.overstyring

import java.util.UUID
import kotlin.reflect.KClass
import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.den
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.person.BehandlingView.TilstandView.UBEREGNET_OMGJØRING
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AO_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.assertInstanceOf
import no.nav.helse.til
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavUtbetalerAgpTest : AbstractDslTest() {

    @Test
    fun `AI fjerner gammel IM - Foreslår sykedag NAV ved en hullete arbedisgiverperiode og begrunnelse for reduksjon satt`() {
        a1 {
            håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
            håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(14.april til 14.april, 18.april til 2.mai)
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())

            nullstillTilstandsendringer()
            val inntektsmeldingId = håndterArbeidsgiveropplysninger(
                listOf(14.april til 14.april, 18.april til 2.mai),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer",
            )

            assertVarsler(listOf(), 1.vedtaksperiode.filter())
            assertVarsler(listOf(), 2.vedtaksperiode.filter())

            assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            assertTrue(observatør.inntektsmeldingHåndtert.any { it.first == inntektsmeldingId })
            assertFalse(observatør.inntektsmeldingIkkeHåndtert.contains(inntektsmeldingId))
            assertVarsel(Varselkode.RV_IM_8, 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - begrunnelse satt med første fraværsdag og hullete arbeidsgiverperiode kan behandles`() {
        a1 {
            håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
            håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(14.april til 14.april, 18.april til 2.mai)
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            nullstillTilstandsendringer()
            håndterArbeidsgiveropplysninger(
                listOf(14.april til 14.april, 18.april til 2.mai),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer"
            )

            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_IM_8, 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - begrunnelse for reduksjon påvirker ikke tidligere arbeidsgiverperiode når første fraværsdag er opplyst`() {
        a1 {
            nyPeriode(1.januar til 4.januar, a1)
            nyPeriode(5.januar til 10.januar, a1)

            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel"
            )

            assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsel(Varselkode.RV_AO_3, 2.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_8, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - hullete AGP sammen med begrunnelse for reduksjon`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent), søknadId = søknadId)
            val søknad = MeldingsreferanseId(søknadId)
            val im = MeldingsreferanseId(
                håndterArbeidsgiveropplysninger(
                    listOf(1.januar til 5.januar, 10.januar til 20.januar),
                    refusjon = Refusjon(INGEN, null),
                    begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer",
                )
            )
            assertEquals(listOf(1.januar til 5.januar, 10.januar til 20.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertVarsler(listOf(RV_IM_8, RV_IV_11), 1.vedtaksperiode.filter())
            assertEquals(
                setOf(
                    Dokumentsporing.søknad(søknad),
                    Dokumentsporing.inntektsmeldingDager(im),
                    Dokumentsporing.inntektsmeldingRefusjon(im),
                    Dokumentsporing.inntektsmeldingInntekt(im),
                ), inspektør.hendelser(1.vedtaksperiode).toSet()
            )
            assertEquals(im.id to 1.vedtaksperiode, observatør.inntektsmeldingHåndtert.single())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `AI fjerner gammel IM - ingen oppgitt agp og første fraværsdag i helg`() {
        a1 {
            nyPeriode(lørdag den 6.januar til 20.januar, a1)
            nullstillTilstandsendringer()
            håndterSelvbestemtArbeidsgiveropplysninger(
                emptyList(),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer"
            )
            assertEquals("HH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertEquals(listOf(6.januar til 20.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - ingen oppgitt agp og første fraværsdag i helg -- første fraværsdag er siste dag i perioden`() {
        a1 {
            nyPeriode(1.januar til lørdag den 6.januar, a1)
            nullstillTilstandsendringer()
            håndterSelvbestemtArbeidsgiveropplysninger(
                emptyList(),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer"
            )
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertEquals(listOf(1.januar til 6.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertEquals(UBEREGNET_OMGJØRING, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().tilstand)
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Overstyrer agp til sykedagNav - ingen refusjon`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Refusjon(INGEN, null, emptyList())
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.sisteUtbetaling().also { overstyringen ->
                assertEquals(1, overstyringen.personOppdrag.size)
                assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
                overstyringen.personOppdrag[0].inspektør.also { linje ->
                    assertEquals(januar, linje.fom til linje.tom)
                    assertEquals(1431, linje.beløp)
                }
            }
        }
    }

    @Test
    fun `AI fjerner gammel IM - im medfører at nav skal utbetale arbeidsgiverperioden`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
            nullstillTilstandsendringer()
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Refusjon(INGEN, null, emptyList()),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ArbeidOpphoert",
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Overstyrer sykedagNav tilbake til vanlig agp`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Refusjon(INGEN, null, emptyList()),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
            )
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            val dagerFør = inspektør.sykdomstidslinje.inspektør.dager
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.Sykedag, 100) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            val dagerEtter = inspektør.sykdomstidslinje.inspektør.dager
            (1.januar til 16.januar).forEach {
                if (!it.erHelg()) assertTrue(dagerFør.getValue(it).kommerFra("Søknad")) { "$it kommer ikke fra Søknad" }
                assertTrue(dagerEtter.getValue(it).kommerFra(OverstyrTidslinje::class)) { "$it kommer ikke fra OverstyrTidslinje" }
            }

            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.sisteUtbetaling().also { overstyringen ->
                assertEquals(1, overstyringen.personOppdrag.size)
                assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
                overstyringen.personOppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(1431, linje.beløp)
                }
            }
        }
    }

    @Test
    fun `Overstyrer egenmeldingsdager til SykedagNav`() {
        a1 {
            håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Refusjon(INGEN, null, emptyList())
            )
            assertVarsel(RV_AO_3, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.sisteUtbetaling().also { overstyringen ->
                assertEquals(1, overstyringen.personOppdrag.size)
                assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
                overstyringen.personOppdrag[0].inspektør.also { linje ->
                    assertEquals(januar, linje.fom til linje.tom)
                    assertEquals(1431, linje.beløp)
                }
            }
        }
    }

    @Test
    fun `Overstyrer agp til sykedagNav - refusjon`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.sisteUtbetaling().also { overstyringen ->
                assertEquals(0, overstyringen.personOppdrag.size)
                assertEquals(1, overstyringen.arbeidsgiverOppdrag.size)
                overstyringen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(januar, linje.fom til linje.tom)
                    assertEquals(1431, linje.beløp)
                }
            }
        }
    }

    @Test
    fun `AI fjerner gammel IM - arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av manglende opptjening`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(),
                refusjon = Refusjon(INGEN, null, emptyList()),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
            )
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertEquals(listOf(1.januar til 10.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertInstanceOf<Sykedag>(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar])
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            val utbetaling = inspektør.utbetaling(0)
            assertTrue(utbetaling.arbeidsgiverOppdrag.isEmpty())
            assertEquals(1, utbetaling.personOppdrag.size)
            assertEquals(1.januar til 10.januar, utbetaling.personOppdrag[0].periode)
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av ferie eller avspasering`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering",
            )
            assertVarsel(RV_IM_25, 1.vedtaksperiode.filter())
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertInstanceOf<Sykedag>(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar])
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - kort periode etter ferie uten sykdom`() {
        a1 {
            nyttVedtak(juni)
            håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
            nullstillTilstandsendringer()
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(1.juni til 16.juni),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
            )
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_AO_3, 2.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_25, 2.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - kort periode etter ferie uten sykdom med arbeidsgiverperioden spredt litt utover`() {
        a1 {
            nyttVedtak(
                juni, arbeidsgiverperiode = listOf(
                1.juni til 5.juni,
                8.juni til 18.juni
            )
            )
            håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
            nullstillTilstandsendringer()
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(1.juni til 5.juni, 8.juni til 18.juni),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
            )
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_AO_3, 2.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_25, 2.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_3, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Inntektsmelding med begrunnelseForReduksjonEllerIkkeUtbetalt og hullete agp`() {
        a1 {
            håndterSøknad(Sykdom(14.januar, 20.januar, 100.prosent))
            håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
            håndterSøknad(Sykdom(8.februar, 11.februar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterArbeidsgiveropplysninger(
                listOf(2.januar til 4.januar, 14.januar til 26.januar)
            )
            assertEquals(2.januar til 20.januar, inspektør.periode(1.vedtaksperiode))
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals("UUUARR AAAAARH SSSSSHH SSSSS?? ??????? ???SSHH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

            nullstillTilstandsendringer()
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(2.januar til 4.januar, 14.januar til 26.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel"
            )
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).dagerNavOvertarAnsvar)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            assertVarsel(Varselkode.RV_IM_4, 3.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_8, 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - avviser dager nav skal utbetale i arbeidsgiverperioden om sykdomsgrad er for lav`() {
        a1 {
            nyPeriode(2.januar til 17.januar, a1)
            håndterSelvbestemtArbeidsgiveropplysninger(
                listOf(2.januar til 17.januar),
                beregnetInntekt = 4000.månedlig,
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer",
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_VV_4, Varselkode.RV_IM_8, Varselkode.RV_VV_2, Varselkode.RV_AO_3), 1.vedtaksperiode.filter())
            assertEquals(listOf(2.januar til 17.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertUtbetalingsdag(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[2.januar], expectedDagtype = Utbetalingsdag.AvvistDag::class, 11)
        }
    }

    @Test
    fun `AI fjerner gammel IM - eget varsel ved oppgitt begrunnelse FerieEllerAvspasering`() {
        a1 {
            nyttVedtak(juni)

            håndterSøknad(Sykdom(1.august, 31.august, 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.juni til 16.juni),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering",
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertVarsler(listOf(RV_IM_3, RV_IM_25), 2.vedtaksperiode.filter())

            håndterOverstyrTidslinje((1..31).map {
                ManuellOverskrivingDag(
                    it.juli,
                    Dagtype.ArbeidIkkeGjenopptattDag
                )
            } + listOf(
                ManuellOverskrivingDag(1.august, Dagtype.Sykedag, 50)
            ))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertEquals("SHH SSSSSHH SSSSSHH SSSSSHH SSSSSHJ JJJJJJJ JJJJJJJ JJJJJJJ JJJJJJJ JJSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        }
    }

    @Test
    fun `AI fjerner gammel IM - avviser dager nav skal utbetale i arbeidsgiverperioden om kravet til minsteinntekt ikke er innfridd`() {
        a1 {
            val underHalvG = Grunnbeløp.halvG.beløp(1.januar) - 1000.årlig
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = underHalvG,
                refusjon = Refusjon(INGEN, null),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer",
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_IM_8, RV_SV_1), 1.vedtaksperiode.filter())
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            (1.januar til 31.januar).filterNot { it.erHelg() }.forEach {
                assertTrue(inspektør.sykdomstidslinje[it] is Sykedag)
            }
            with(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør) {
                (januar).filterNot { it.erHelg() }.forEach {
                    assertEquals(listOf(Begrunnelse.MinimumInntekt), begrunnelse(it))
                }
            }
        }
    }

    @Test
    fun `IM med begrunnelse for reduksjon, men ikke AGP, lager utbetaling bare første fraværsdag`() {
        a1 {
            håndterSøknad(1.januar til 10.januar)

            håndterSelvbestemtArbeidsgiveropplysninger(
                arbeidsgiverperioder = emptyList(),
                beregnetInntekt = 9000.månedlig,
                refusjon = Refusjon(9000.månedlig, null),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening"
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertEquals(listOf(1.januar til 10.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))

            håndterSelvbestemtArbeidsgiveropplysninger(
                arbeidsgiverperioder = emptyList(),
                beregnetInntekt = INNTEKT,
                refusjon = Refusjon(INNTEKT, null),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
                vedtaksperiodeId = 1.vedtaksperiode
            )

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertVarsler(listOf(RV_IM_8, Varselkode.RV_IM_4, Varselkode.RV_AO_3), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Revurdering av AUU med påfølgende utbetalt periode, etter IM med begrunnelse for reduksjon`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 16.januar)
            håndterSøknad(17.januar til 31.januar)

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 3.vedtaksperiode
            )

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSelvbestemtArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 14.januar, 15.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeLoenn",
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)

            håndterYtelser(3.vedtaksperiode)


            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Revudering av AUU uten påfølgende utbetalt periode kastes ut på IM med ikke støttet begrunnelse for reduksjon`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
            håndterSøknad(6.januar til 16.januar)
            håndterSøknad(17.januar til 31.januar)

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 3.vedtaksperiode
            )

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)

            håndterKorrigerteArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 14.januar, 15.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeLoenn",
                beregnetInntekt = INNTEKT,
            )

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)

            assertVarsel(Varselkode.RV_IM_4, 3.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_8, 3.vedtaksperiode.filter())
        }
    }

    private inline fun <reified R : Utbetalingsdag> assertUtbetalingsdag(dag: Utbetalingsdag, expectedDagtype: KClass<R>, expectedTotalgrad: Int = 100) {
        dag.let {
            assertEquals(expectedDagtype, it::class)
            it.økonomi.brukTotalGrad { totalGrad -> assertEquals(expectedTotalgrad, totalGrad) }
        }
    }
}
