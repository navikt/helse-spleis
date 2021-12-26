package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import java.util.*

internal class ReberegningAvAvsluttetUtenUtbetalingE2ETest : AbstractEndToEndTest() {

    @ForventetFeil("Dette støtter vi ikke enda")
    @Test
    fun `reberegner avsluttet periode dersom inntektsmelding kommer inn`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
    }

    @Test
    fun `reberegner ikke avsluttet periode dersom inntektsmelding kommer inn og det er utbetalt nyere periode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(1.mars, 31.mars)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `reberegner ikke avsluttet periode dersom perioden er innenfor agp etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 28.februar, 100.prosent))

        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
    }

    @ForventetFeil("Dette er ikke støttet enda")
    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `gjenopptar behandling på neste periode dersom inntektsmelding treffer avsluttet periode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }

    @ForventetFeil("Dette støtter vi ikke enda")
    @Test
    fun `gjenopptar behandling på neste periode avsluttet periode etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
    }

    @Test
    fun `gjenopptar ikke behandling på neste periode avsluttet periode etter IM dersom det er nyere vedtak`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        nyttVedtak(1.mars, 31.mars)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `gjenopptar ikke behandling på neste periode avsluttet periode som fortsatt er innenfor agp`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `reberegner avsluttet periode dersom perioden er utenfor arbeidsgiverperioden - uten inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.februar, 5.mars, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(1.januar til 16.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)
        person.arbeidsgiver(ORGNUMMER).addInntekt(inntektsmelding, 1.januar)

        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `reberegner avsluttet periode dersom perioden er utenfor arbeidsgiverperioden - med inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(12.februar, 5.mars, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(1.januar til 16.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `oppdager at neste periode er utenfor agp - gap til forrige - ingentign skjer`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(26.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(26.januar, 26.januar, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(5.januar til 20.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `påminnelse treffer første periode utenfor agp`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(26.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(26.januar, 26.januar, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(5.januar til 20.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `påminnelse treffer nyere periode - ingenting skjer`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(26.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(26.januar, 26.januar, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(5.januar til 20.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) // påminnelsen treffer siste periode, men periode nr 2 er også utenfor AGP
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(5.januar til 20.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `reberegner ikke avsluttet periode dersom perioden er innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 28.februar, 100.prosent))

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(5.januar til 20.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `reberegner ikke avsluttet periode dersom det finnes nyere perioder som er avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))

        nyttVedtak(1.mars, 31.mars)

        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = inntektsmelding(inntektsmeldingId, listOf(1.januar til 16.januar))
        person.arbeidsgiver(ORGNUMMER).oppdaterSykdom(inntektsmelding)

        håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
    }

}
