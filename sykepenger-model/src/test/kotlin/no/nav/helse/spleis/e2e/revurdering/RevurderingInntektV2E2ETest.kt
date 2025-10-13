package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengTilGodkjentVedtak
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingInntektV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere enslig periode`() {
        val forventetEndring = 200.daglig
        val overstyrtInntekt = INNTEKT + forventetEndring
        nyttVedtak(januar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.januar, 1431.daglig, INGEN, INNTEKT)
        håndterOverstyrInntekt(overstyrtInntekt, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertDag<Sykedag, NavDag>(
            dato = 17.januar,
            arbeidsgiverbeløp = 1431.daglig,
            personbeløp = forventetEndring,
            aktuellDagsinntekt = overstyrtInntekt
        )
        assertDiff(2200)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertDiff(-5588)

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
            assertEquals(2, utbetalinger.size)
            assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.last().inspektør.tilstand)
        }
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertDiff(-3047)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering, også når det er snakk om flere perioder`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertDiff(-5588)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertDiff(-3047)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i AvventerHistorikkRevurdering`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
        assertDiff(-3047)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i AvventerSimuleringRevurdering`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
        assertDiff(-3047)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
    }

    @Test
    fun `Ved overstyring av revurdering av inntekt til under krav til minste sykepengegrunnlag skal vi opphøre den opprinnelige utbetalingen`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 50000.årlig,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsler(listOf(RV_SV_1, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        assertTrue(inspektør.utbetaling(0).erUtbetalt)
        assertTrue(inspektør.utbetaling(1).erUbetalt)
        assertEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag.fagsystemId, inspektør.utbetaling(1).arbeidsgiverOppdrag.fagsystemId)
        assertDiff(-2112)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt - med AGP imellom`() {
        nyttVedtak(januar, 100.prosent)
        nyttVedtak(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()

        val korrelasjonsIdPåUtbetaling1 = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING
        )

        assertEquals(3, inspektør.antallUtbetalinger)
        val korrelasjonsIdPåUtbetaling2 = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId
        val korrelasjonsIdPåUtbetaling3 = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdPåUtbetaling1, korrelasjonsIdPåUtbetaling2)
        assertNotEquals(korrelasjonsIdPåUtbetaling2, korrelasjonsIdPåUtbetaling3)
        assertDiff(506)

        håndterSimulering(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)

        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)

        assertEquals(4, inspektør.antallUtbetalinger)
        val korrelasjonsIdPåUtbetaling4 = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId
        val korrelasjonsIdPåUtbetaling5 = inspektør.sisteAvsluttedeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdPåUtbetaling3, korrelasjonsIdPåUtbetaling5)
        assertNotEquals(korrelasjonsIdPåUtbetaling4, korrelasjonsIdPåUtbetaling5)
        assertDiff(0)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt - med samme AGP`() {
        nyttVedtak(januar, 100.prosent)
        nyttVedtak(2.februar til 28.februar, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        assertEquals(3, inspektør.antallUtbetalinger)

        assertDiff(506)
        inspektør.utbetaling(2).also { revurderingJanuar ->
            val januarutbetaling = inspektør.utbetaling(0)
            assertEquals(januarutbetaling.korrelasjonsId, revurderingJanuar.korrelasjonsId)

            assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString().trim())
            assertEquals(januar, januarutbetaling.periode)
            assertEquals(januar, revurderingJanuar.periode)
            assertEquals(1, revurderingJanuar.personOppdrag.size)
            assertEquals(1, revurderingJanuar.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.UEND, revurderingJanuar.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(0, revurderingJanuar.arbeidsgiverOppdrag.inspektør.nettoBeløp)

            revurderingJanuar.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                assertEquals(17.januar, arbeidsgiveroppdrag.fom(0))
                assertEquals(31.januar, arbeidsgiveroppdrag.tom(0))
                assertEquals(1431, arbeidsgiveroppdrag.beløp(0))
                assertEquals(Endringskode.UEND, arbeidsgiveroppdrag.endringskode(0))
            }
            assertEquals(506, revurderingJanuar.personOppdrag.inspektør.nettoBeløp)
            revurderingJanuar.personOppdrag.inspektør.also { personoppdrag ->
                assertEquals(17.januar, personoppdrag.fom(0))
                assertEquals(31.januar, personoppdrag.tom(0))
                assertEquals(46, personoppdrag.beløp(0))
                assertEquals(Endringskode.NY, personoppdrag.endringskode(0))
            }
        }

        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertEquals(4, inspektør.antallUtbetalinger)

        inspektør.utbetaling(3).also { revurderingFebruar ->
            val februarutbetaling = inspektør.utbetaling(1)
            assertEquals(februarutbetaling.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            revurderingFebruar.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                assertEquals(2.februar, arbeidsgiveroppdrag.fom(0))
                assertEquals(28.februar, arbeidsgiveroppdrag.tom(0))
                assertEquals(1431, arbeidsgiveroppdrag.beløp(0))
                assertEquals(Endringskode.UEND, arbeidsgiveroppdrag.endringskode(0))
            }
        }
    }

    @Test
    fun `revurder inntekt til under krav til minste sykepengegrunnlag`() {
        nyttVedtak(januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        assertEquals(2, inspektør.antallUtbetalinger)
        assertDiff(-15741)

        assertVarsel(RV_SV_1, AktivitetsloggFilter.person())
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.harUtbetalingsdager())
    }

    @Test
    fun `revurder inntekt til under krav til minste sykepengegrunnlag slik at utbetaling opphører, og så revurder igjen til over krav til minste sykepengegrunnlag`() {
        nyttVedtak(januar, 100.prosent, beregnetInntekt = 5000.månedlig)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 3000.månedlig)))
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        assertEquals(2, inspektør.antallUtbetalinger)
        assertDiff(-2541)

        assertVarsel(RV_SV_1, AktivitetsloggFilter.person())
        assertFalse(inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.harUtbetalingsdager())
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(5000.månedlig, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)

        var opprinneligFagsystemId: String?
        inspektør.utbetaling(0).arbeidsgiverOppdrag.apply {
            assertEquals(Endringskode.NY, inspektør.endringskode)
            opprinneligFagsystemId = fagsystemId
            assertEquals(1, size)
            first().inspektør.apply {
                assertEquals(Endringskode.NY, endringskode)
                assertEquals(1, delytelseId)
                assertEquals(null, refDelytelseId)
                assertEquals(null, refFagsystemId)
            }
        }
        inspektør.utbetaling(1).arbeidsgiverOppdrag.apply {
            assertEquals(Endringskode.ENDR, inspektør.endringskode)
            assertEquals(opprinneligFagsystemId, fagsystemId)
            assertEquals(1, size)
            first().inspektør.apply {
                assertEquals(Endringskode.ENDR, endringskode)
                assertEquals(1, delytelseId)
                assertEquals(null, refDelytelseId)
                assertEquals(null, refFagsystemId)
                assertEquals(17.januar, datoStatusFom)
            }
        }
        inspektør.utbetaling(2).arbeidsgiverOppdrag.apply {
            assertEquals(Endringskode.ENDR, inspektør.endringskode)
            assertEquals(opprinneligFagsystemId, fagsystemId)
            assertEquals(1, size)
            first().inspektør.apply {
                assertEquals(Endringskode.NY, endringskode)
                assertEquals(2, delytelseId)
                assertEquals(1, refDelytelseId)
                assertEquals(fagsystemId, refFagsystemId)
            }
        }
        assertDiff(2541)
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - arbeidsgiversøknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar))
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()
        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 30000.månedlig)
        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertDiff(-1012)
    }

    @Test
    fun `revurdering av inntekt delegeres til den første perioden som har en utbetalingstidslinje - periode uten utbetaling først`() {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.januar, 31.januar))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 30000.månedlig)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        this@RevurderingInntektV2E2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@RevurderingInntektV2E2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        assertEquals(4, inspektør.antallUtbetalinger)
        assertEquals(2, inspektør.utbetalinger(1.vedtaksperiode).size)
        assertEquals(2, inspektør.utbetalinger(2.vedtaksperiode).size)
        assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(1))
        assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetalingtilstand(2))
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(3))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertDiff(-920)
    }

    @Test
    fun `revurdere inntekt slik at det blir brukerutbetaling`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(
                INGEN,
                null,
                emptyList()
            )
        )
        håndterOverstyrInntekt(inntekt = INNTEKT, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertDiff(0)
        assertTrue(inspektør.utbetaling(1).personOppdrag.harUtbetalinger())
        assertTrue(inspektør.utbetaling(1).arbeidsgiverOppdrag.harUtbetalinger()) // opphører arbeidsgiveroppdraget
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).personOppdrag))
        assertEquals(17.januar, inspektør.utbetaling(1).arbeidsgiverOppdrag.first().inspektør.datoStatusFom)

        assertEquals(15741, inspektør.utbetaling(1).personOppdrag.nettoBeløp())
        assertEquals(-15741, inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
    }

    @Test
    fun `revurdere inntekt slik at det blir delvis refusjon`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(
                25000.månedlig,
                null,
                emptyList()
            )
        )
        håndterOverstyrInntekt(inntekt = INNTEKT, skjæringstidspunkt = 1.januar)
        this@RevurderingInntektV2E2ETest.håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertDiff(0)
        assertTrue(inspektør.utbetaling(1).personOppdrag.harUtbetalinger())
        assertTrue(inspektør.utbetaling(1).arbeidsgiverOppdrag.harUtbetalinger()) // opphører arbeidsgiveroppdraget
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).personOppdrag))
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).arbeidsgiverOppdrag))

        assertEquals(3047, inspektør.utbetaling(1).personOppdrag.nettoBeløp())
        assertEquals(-3047, inspektør.utbetaling(1).arbeidsgiverOppdrag.nettoBeløp())
    }

    @Test
    fun `revurdere mens en periode er til utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertEquals(1, observatør.avsluttetMedVedtakEvent.size)
        assertNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode.id(a1)])
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
        assertEquals(2, observatør.avsluttetMedVedtakEvent.size)
        assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode.id(a1)])
    }

    @Test
    fun `revurdere mens en periode har feilet i utbetaling`() {
        nyttVedtak(januar)
        forlengTilGodkjentVedtak(februar)
        nullstillTilstandsendringer()
        håndterUtbetalt(status = Oppdragstatus.FEIL)
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    fun `refusjonsopplysninger er uendret etter revurdert inntekt`() {
        nyttVedtak(januar)
        val refusjonsopplysningerFørRevurdering = inspektør.refusjon(1.vedtaksperiode)
        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        val refusjonsopplysningerEtterRevurdering = inspektør.refusjon(1.vedtaksperiode)
        assertBeløpstidslinje(refusjonsopplysningerFørRevurdering, refusjonsopplysningerEtterRevurdering)
    }

    private inline fun <reified D : Dag, reified UD : Utbetalingsdag> assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN, aktuellDagsinntekt: Inntekt = INGEN) {
        inspektør.sykdomshistorikk.tidslinje(0)[dato].let {
            assertTrue(it is D) { "Forventet ${D::class.simpleName} men var ${it::class.simpleName}" }
        }
        inspektør.utbetalingstidslinjer(1.vedtaksperiode)[dato].let {
            assertTrue(it is UD) { "Forventet ${UD::class.simpleName} men var ${it::class.simpleName}" }
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
            assertEquals(aktuellDagsinntekt, it.økonomi.inspektør.aktuellDagsinntekt)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.sisteUtbetaling().nettobeløp)
    }
}
