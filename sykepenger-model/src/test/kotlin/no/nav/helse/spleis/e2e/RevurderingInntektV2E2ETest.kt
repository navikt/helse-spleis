package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class RevurderingInntektV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere enslig periode`() {
        val forventetEndring = 200.daglig
        val overstyrtInntekt = INNTEKT + forventetEndring
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.januar, 1431.daglig, INGEN, INNTEKT)
        håndterOverstyrInntekt(overstyrtInntekt, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        assertDag<Sykedag, NavDag>(
            dato = 17.januar,
            arbeidsgiverbeløp = 1431.daglig,
            personbeløp = forventetEndring,
            aktuellDagsinntekt = overstyrtInntekt
        )
        assertDiff(2200)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }


    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertDiff(-5588)

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertDiff(-3047)

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering, også når det er snakk om flere perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertDiff(-15748)

        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertDiff(-8587)

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
        )

        assertTilstander(2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i AvventerHistorikkRevurdering`() {
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
        assertDiff(-3047)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i AvventerSimuleringRevurdering`() {
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterOverstyrInntekt(inntekt = 25000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
        assertDiff(-3047)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering ved revurdering av tidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(
            dato = 22.januar,
            arbeidsgiverbeløp = 1431.daglig,
            personbeløp = INGEN,
            aktuellDagsinntekt = INNTEKT
        )

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(
            dato = 22.januar,
            arbeidsgiverbeløp = INGEN,
            personbeløp = INGEN,
            aktuellDagsinntekt = INNTEKT
        )
        håndterSimulering(2.vedtaksperiode)

        val overstyrtInntekt = 20000.månedlig
        håndterOverstyrInntekt(inntekt = overstyrtInntekt, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        assertDag<Sykedag, NavDag>(
            dato = 17.januar,
            arbeidsgiverbeløp = overstyrtInntekt.rundTilDaglig(),
            personbeløp = INGEN,
            aktuellDagsinntekt = overstyrtInntekt
        )
        håndterSimulering(2.vedtaksperiode)
        assertDiff(-21286)

        // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
        assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
        assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    @Test
    fun `Ved overstyring av revurdering av inntekt til under krav til minste sykepengegrunnlag skal vi opphøre den opprinnelige utbetalingen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = 50000.årlig)
        håndterYtelser(1.vedtaksperiode)
        val inntekter = listOf(grunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(3)))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ORGNUMMER, 1.januar, 50000.årlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(46000.årlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertTrue(utbetalinger[0].inspektør.erUtbetalt)
        assertTrue(utbetalinger[1].inspektør.erUbetalt)
        assertEquals(1, utbetalinger.map { it.inspektør.arbeidsgiverOppdrag.fagsystemId() }.toSet().size)
        assertDiff(-2112)
    }

    @Test
    fun `revurder inntekt ukjent skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 2.januar)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
        )
        assertEquals(1, inspektør.utbetalinger.size)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt - med AGP imellom`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        nyttVedtak(1.mars, 31.mars, 100.prosent)
        nullstillTilstandsendringer()

        val korrelasjonsIdPåUtbetaling1 = inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING
        )

        assertEquals(3, inspektør.utbetalinger.size)
        val korrelasjonsIdPåUtbetaling2 = inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId
        val korrelasjonsIdPåUtbetaling3 = inspektør.gjeldendeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdPåUtbetaling1, korrelasjonsIdPåUtbetaling2)
        assertNotEquals(korrelasjonsIdPåUtbetaling2, korrelasjonsIdPåUtbetaling3)
        assertDiff(506)

        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)

        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)

        assertEquals(4, inspektør.utbetalinger.size)
        val korrelasjonsIdPåUtbetaling4 = inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId
        val korrelasjonsIdPåUtbetaling5 = inspektør.gjeldendeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdPåUtbetaling3, korrelasjonsIdPåUtbetaling5)
        assertNotEquals(korrelasjonsIdPåUtbetaling4, korrelasjonsIdPåUtbetaling5)
        assertDiff(0)
    }

    @Test
    fun `revurder inntekt tidligere skjæringstidspunkt - med samme AGP`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        nyttVedtak(2.februar, 28.februar, 100.prosent)
        nullstillTilstandsendringer()

        håndterOverstyrInntekt(inntekt = 32000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING
        )

        assertEquals(3, inspektør.utbetalinger.size)

        val utbetalingstidslinje = inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode).utbetalingstidslinje()
        val korrelasjonsIdPåUtbetaling1 = inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode).inspektør.korrelasjonsId
        val korrelasjonsIdPåUtbetaling2 = inspektør.gjeldendeUtbetalingForVedtaksperiode(2.vedtaksperiode).inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdPåUtbetaling1, korrelasjonsIdPåUtbetaling2)
        assertDiff(506)

        assertForventetFeil(
            forklaring = "Revurdering av 1.vedtaksperiode skal bevare tilstanden for utbetalingen til 2.vedtaksperiode",
            nå = {
                assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", utbetalingstidslinje.toString().trim())
            },
            ønsket = {
                assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNNANHH NNNNNHH NNNNNHH NNNNNHH NNN", utbetalingstidslinje.toString().trim())
            }
        )
    }




    private inline fun <reified D: Dag, reified UD: Utbetalingsdag>assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN, aktuellDagsinntekt: Inntekt = INGEN) {
        inspektør.sykdomshistorikk.inspektør.tidslinje(0)[dato].let {
            assertTrue(it is D) { "Forventet ${D::class.simpleName} men var ${it::class.simpleName}"}
        }
        inspektør.sisteUtbetalingUtbetalingstidslinje()[dato].let {
            assertTrue(it is UD) { "Forventet ${UD::class.simpleName} men var ${it::class.simpleName}"}
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
            assertEquals(aktuellDagsinntekt, it.økonomi.inspektør.aktuellDagsinntekt)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.utbetalinger.last().inspektør.nettobeløp)
    }
}