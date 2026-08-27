package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest : AbstractDslTest() {

    @Test
    fun `AI fjerner gammel IM - Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(2.januar til 17.januar))
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Korrigerende inntektsmelding som strekker agp tilbake skal ha varsel`() {
        a1 {
            nyttVedtak(2.januar til 31.januar)

            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 2.januar, listOf(2.januar til 17.januar))
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
            assertTrue(utbetalingstidslinje.subset(1.januar til 16.januar).all {
                it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
            })
            assertEquals(0.månedlig, utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp)

            assertEquals(2.januar til 31.januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode)
            assertEquals("SSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertEquals("SSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Korrigerende inntektsmelding med lik agp skal ikke ha varsel`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.januar til 16.januar))

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
            assertTrue(utbetalingstidslinje.subset(1.januar til 16.januar).all {
                it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
            })
            assertEquals(1431.daglig, utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Korrigerende inntektsmelding som strekker agp fremover`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(2.januar til 17.januar), vedtaksperiodeId = 1.vedtaksperiode)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
            assertEquals(Utbetalingsdag.ArbeidsgiverperiodeDag::class, utbetalingstidslinje[1.januar]::class)
            assertEquals(0.daglig, utbetalingstidslinje[1.januar].økonomi.inspektør.arbeidsgiverbeløp)
            assertTrue(utbetalingstidslinje.subset(1.januar til 16.januar).all {
                it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
            })
            assertEquals(1431.daglig, utbetalingstidslinje[18.januar].økonomi.inspektør.arbeidsgiverbeløp)

            assertEquals(januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Antall dager mellom opplyst agp og gammel agp er mer enn 10`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Antall dager mellom opplyst agp og gammel agp er mindre enn 10`() {
        a1 {
            nyttVedtak(10.januar til 31.januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertEquals("SSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertOverstyringIgangsatt("NY_PERIODE, REFUSJONSOPPLYSNINGER, NY_PERIODE, NY_PERIODE, REFUSJONSOPPLYSNINGER")

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Antall dager mellom opplyst agp og gammel agp er mindre enn 10 - siste periode er til utbetaling`() {
        a1 {
            nyttVedtak(10.januar til 31.januar)
            forlengVedtak(februar)
            val vpMars = nyPeriode(mars)
            håndterYtelser(vpMars)
            håndterSimulering(vpMars)
            håndterUtbetalingsgodkjenning(vpMars)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertEquals("SSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `her er det et gap mellom første og andre vedtaksperiode og mindre enn 10 dager mellom agps`() {
        a1 {
            nyttVedtak(10.januar til 29.januar)
            nyttVedtak(februar, arbeidsgiverperiode = emptyList())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar)
            )

            assertVarsler(listOf(RV_IM_4, RV_IM_24), 2.vedtaksperiode.filter())
            assertEquals("SSSHH SSSSSHH SSSSSHH S??SSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `AI fjerner gammel IM - her er det et gap mellom første og andre vedtaksperiode og mer enn 10 dager mellom agps, men mindre enn 16 dager mellom periodene`() {
        a1 {
            nyttVedtak(10.januar til 29.januar)
            nyttVedtak(10.februar til 26.februar, arbeidsgiverperiode = emptyList())

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            val inntektsmeldingId = håndterKorrigerteArbeidsgiveropplysninger(
                listOf(10.februar til 25.februar)
            )

            assertEquals("SSSHH SSSSSHH SSSSSHH S?????? ?????HH SSSSSHH SSSSSHH S", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(listOf(RV_IM_24, RV_IM_4), 2.vedtaksperiode.filter())
            assertTrue(inntektsmeldingId in observatør.inntektsmeldingHåndtert.map { it.first })
        }
    }

    @Test
    fun `AI fjerner gammel IM - Antall dager mellom opplyst agp og gammel agp er mindre enn 10 - flere perioder før korrigerte dager`() {
        a1 {
            nyttVedtak(10.januar til 30.januar)
            forlengVedtak(31.januar til 31.januar)
            forlengVedtak(februar)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertEquals("SSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)

            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertOverstyringIgangsatt("NY_PERIODE, REFUSJONSOPPLYSNINGER, NY_PERIODE, NY_PERIODE, REFUSJONSOPPLYSNINGER")

            val revurdering1Vedtaksperiode = inspektør.utbetaling(3)
            revurdering1Vedtaksperiode.also { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(0, utbetalingInspektør.personOppdrag.size)
                utbetalingInspektør.arbeidsgiverOppdrag.inspektør.also { oppdragInspektør ->
                    assertEquals(UEND, oppdragInspektør.endringskode)
                    assertEquals(1, oppdragInspektør.delytelseId(0))
                    assertNull(oppdragInspektør.datoStatusFom(0))
                }
            }
            val revurdering2Vedtaksperiode = inspektør.utbetaling(4)
            revurdering2Vedtaksperiode.also { utbetalingInspektør ->
                assertEquals(UEND, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(0, utbetalingInspektør.personOppdrag.size)
            }
            val revurdering3Vedtaksperiode = inspektør.utbetaling(5)
            revurdering3Vedtaksperiode.also { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(UEND, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
                utbetalingInspektør.arbeidsgiverOppdrag[0].inspektør.also { linjeInspektør ->
                    assertEquals(1.februar til 28.februar, linjeInspektør.periode)
                }
                assertEquals(0, utbetalingInspektør.personOppdrag.size)
            }
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Endring i både dager og inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(15.januar til 30.januar),
                beregnetInntekt = INNTEKT * 1.1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertOverstyringIgangsatt("NY_PERIODE, REFUSJONSOPPLYSNINGER, REFUSJONSOPPLYSNINGER")
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 1.1)
            }
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Endring i bare inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 1.1
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertOverstyringIgangsatt("NY_PERIODE, REFUSJONSOPPLYSNINGER, REFUSJONSOPPLYSNINGER")

            assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Endring i siste del av agp`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            nyttVedtak(10.januar til 31.januar)

            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(1.januar til 5.januar, 10.januar til 20.januar))

            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(12.januar til 27.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(1.januar til 5.januar, 10.januar til 20.januar))
            assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_IM_4, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - arbeidsgiver korrigerer AGP ved å stykke den opp`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(1.januar til 15.januar, 20.januar.somPeriode())
            )
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - arbeidsgiver korrigerer AGP men første og siste dag er lik`() {
        a1 {
            nyttVedtak(januar, arbeidsgiverperiode = listOf(1.januar til 10.januar, 15.januar til 20.januar))
            håndterKorrigerteArbeidsgiveropplysninger(
                listOf(1.januar til 8.januar, 13.januar til 20.januar)
            )
            assertEquals("SSSSSHH SSSAARR SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - korrigert agp i avventer_godkjenning`() {
        a1 {
            tilGodkjenning(januar, beregnetInntekt = INNTEKT)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

            håndterKorrigerteArbeidsgiveropplysninger(listOf(5.januar til 20.januar))
            assertVarsler(listOf(RV_IM_24, RV_IM_4), 1.vedtaksperiode.filter())

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)

            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        }
    }

    @Test
    fun `AI fjerner gammel IM - forlengelse til godkjenning - korrigerende agp mindre enn ti dager fra forrige`() {
        a1 {
            nyttVedtak(10.januar til 31.januar)
            nyPeriode(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))

            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.januar, listOf(10.januar til 25.januar))

            assertEquals("SSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `forlengelse til godkjenning - korrigerende agp mer enn ti dager fra forrige`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    private fun assertOverstyringIgangsatt(vararg event: String) {
        val events = event.flatMap { it.split(", ") }
        assertEquals(events, observatør.overstyringIgangsatt.map { it.årsak })
    }
}
