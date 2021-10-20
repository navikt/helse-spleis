package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.TilstandType
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class OverstyrTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(1))
        assertNotEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId())
        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `vedtaksperiode rebehandler informasjon etter overstyring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellArbeidsgiverdag(18.januar)))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertNotEquals(inspektør.utbetaling(0).arbeidsgiverOppdrag().fagsystemId(), inspektør.utbetaling(1).arbeidsgiverOppdrag().fagsystemId())
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().sykepengeperiode()?.start)
    }

    @Test
    fun `grad over grensen overstyres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(30.0, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt etter overstyring av grad`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `overstyrt til fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(TilstandType.AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().arbeidsgiverOppdrag().size)
        assertEquals(21.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[0].tom)
        assertEquals(24.januar, inspektør.utbetalinger.last().arbeidsgiverOppdrag()[1].fom)
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyring(listOf(manuellFeriedag(26.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av sykHelgDag`() {
        håndterSykmelding(Sykmeldingsperiode(17.desember(2017), 31.desember(2017), 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(17.desember(2017), 1.januar)), førsteFraværsdag = 17.desember(2017))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(17.desember(2017), 31.desember(2017), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(17.desember(2017), 1.januar)), førsteFraværsdag = 10.januar)
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(10.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        inspektør.arbeidsgiver.oppdaterSykdom(object : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
            override fun sykdomstidslinje() = Sykdomstidslinje.ukjent(2.januar, 9.januar, TestEvent.testkilde)
            override fun valider(periode: Periode) = throw RuntimeException("Brukes ikke i testene")
            override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = throw RuntimeException("Brukes ikke i testene")
            override fun aktørId() = throw RuntimeException("Brukes ikke i testene")
            override fun fødselsnummer() = throw RuntimeException("Brukes ikke i testene")
            override fun organisasjonsnummer() = throw RuntimeException("Brukes ikke i testene")
        })
        håndterOverstyringSykedag(2.januar til 9.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)

        assertEquals("H SSSSSHH SSSSSHH USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals(" PNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(2.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere perioder til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyring((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere periode i AVVENTER_SIMULERING_REVURDERING til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyring((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
            TilstandType.AVVENTER_HISTORIKK_REVURDERING,
            TilstandType.AVVENTER_SIMULERING_REVURDERING,
            TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }


    @Test
    fun `skal kunne overstyre dagtype i utkast til revurdering ved revurdering av inntekt`() {
            nyttVedtak(1.januar, 31.januar)
            forlengVedtak(1.februar, 28.februar)

            håndterOverstyring(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
            håndterYtelser(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            håndterOverstyring((20.januar til 29.januar).map { manuellFeriedag(it) })
            håndterYtelser(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
            assertEquals(23075, inspektør.utbetalinger.last().arbeidsgiverOppdrag().totalbeløp())
            assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
            assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

            assertTilstander(1.vedtaksperiode,
                TilstandType.START,
                TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
                TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_VILKÅRSPRØVING,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_SIMULERING,
                TilstandType.AVVENTER_GODKJENNING,
                TilstandType.TIL_UTBETALING,
                TilstandType.AVSLUTTET,
                TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
            )

            assertTilstander(2.vedtaksperiode,
                TilstandType.START,
                TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                TilstandType.AVVENTER_HISTORIKK,
                TilstandType.AVVENTER_SIMULERING,
                TilstandType.AVVENTER_GODKJENNING,
                TilstandType.TIL_UTBETALING,
                TilstandType.AVSLUTTET,
                TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_SIMULERING_REVURDERING,
                TilstandType.AVVENTER_GODKJENNING_REVURDERING,
                TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                TilstandType.AVVENTER_SIMULERING_REVURDERING,
                TilstandType.AVVENTER_GODKJENNING_REVURDERING
            )
        }
}
