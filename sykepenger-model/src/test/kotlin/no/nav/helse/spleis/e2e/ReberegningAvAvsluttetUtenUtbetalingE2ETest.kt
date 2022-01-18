package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mai
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ReberegningAvAvsluttetUtenUtbetalingE2ETest : AbstractEndToEndTest() {

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
    fun `gjenopptar ikke behandling dersom det er nyere periode som er utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        nyttVedtak(1.mars, 31.mars)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.mai, 15.mai, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.mai, 28.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 15.mai, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE)
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

    @ForventetFeil("Dette er ikke støttet enda")
    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige 2`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
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
    fun `gjenopptar ikke behandling på neste periode etter at kort periode reberegnes`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(27.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(27.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_UFERDIG)
    }

    @ForventetFeil("Det sprenger")
    @Test
    fun `gjenopptar ikke behandling på neste gap periode etter at kort periode reberegnes`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(10.januar til 25.januar))
        håndterInntektsmeldingMedValidering(3.vedtaksperiode, listOf(10.januar til 25.januar), 30.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
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
    fun `Periode i MOTTATT_SYKMELDING_FERDIG_FORLENGELSE går til AVVENTER_SØKNAD_UFERDIG_FORLENGELSE, når tidligere og tilstøtende periode går fra AVSLUTTET_UTEN_UTBETALING til AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `Periode i AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE går til AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, når tidligere og tilstøtende periode går fra AVSLUTTET_UTEN_UTBETALING til AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))


        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_UFERDIG)
    }

    @Test
    fun `Flere arbeidsgivere, begge arbeidsgiverne venter på IM i AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, begge blir poket videre når tidligere periode blir truffet av IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertError(1.vedtaksperiode, "Kan ikke flytte en vedtaksperiode i AVSLUTTET_UTEN_UTBETALING ved flere arbeidsgivere", orgnummer = a1)

        // Asserts for når vi har bedre gjenkjenning av flere AG vedtaksperioder
        //assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_ARBEIDSGIVERE, orgnummer = a1)
        //assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        //assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_UFERDIG, orgnummer = a1)
        //assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, orgnummer = a2)
    }


    @Test
    fun `Flere arbeidsgivere, én arbeidsgiver venter på IM i AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, blir poket videre når tidligere periode blir truffet av IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)


        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(3.januar til 18.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertError(1.vedtaksperiode, "Kan ikke flytte en vedtaksperiode i AVSLUTTET_UTEN_UTBETALING ved flere arbeidsgivere", orgnummer = a2)

        // Asserts for når vi har bedre gjenkjenning av flere AG vedtaksperioder
//        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
//        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_ARBEIDSGIVERE, orgnummer = a2)
//
//        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_ARBEIDSGIVERE, AVVENTER_HISTORIKK, orgnummer = a1)
//        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_UFERDIG, orgnummer = a2)
    }

}
