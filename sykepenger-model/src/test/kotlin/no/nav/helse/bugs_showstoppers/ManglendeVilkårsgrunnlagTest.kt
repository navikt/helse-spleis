package no.nav.helse.bugs_showstoppers

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ManglendeVilkårsgrunnlagTest : AbstractEndToEndTest() {

    @Test
    fun `arbeidsgiverperiode med brudd i helg`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 5.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(4.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 12.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 1.februar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 1.februar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        // 6. og 7. januar ble tolket som FriskHelg og medførte brudd i arbeidsgiverperioden
        // og dermed ble også skjæringstidspunktet forskjøvet
        håndterInntektsmelding(listOf(
            1.januar til 3.januar,
            4.januar til 5.januar,
            // 6. og 7. januar er helg
            8.januar til 12.januar,
            13.januar til 18.januar
        ), 1.januar)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        assertEquals(listOf(InntekterForSammenligningsgrunnlag, Opptjening, Medlemskap), inspektør.etterspurteBehov(1.vedtaksperiode).map { it.type })
        assertTrue(inspektør.etterspurteBehov(2.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(3.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(4.vedtaksperiode).isEmpty())
    }

    @Test
    fun `inntektsmelding drar periode tilbake og lager tilstøtende`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 5.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(26.januar, 2.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(26.januar, 2.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(5.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 21.februar, 100.prosent))

        // inntektsmelding inneholder en ukjent dag — 8. januar — som vi ikke
        // har registrert før i forbindelse med verken søknad eller sykmelding
        // Dette medfører at perioden 26. januar - 2. februar "dras tilbake"
        // til 6. januar, siden 8. januar er en mandag og 5. januar (forrige agp-innslag) er en fredag
        // så regnes lørdag + søndag som del av arbeidsgiverperioden også.
        // Dermed ble perioden 6. januar - 2. februar regnet som tilstøtende til 1.-5. januar, selv om
        // de to har forskjellige skjæringstidspunkt.
        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            8.januar til 8.januar,
            24.januar til 2.februar
        ), 24.januar)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        assertTrue(inspektør.etterspurteBehov(1.vedtaksperiode).isEmpty())
        assertEquals(listOf(InntekterForSammenligningsgrunnlag, Opptjening, Medlemskap), inspektør.etterspurteBehov(2.vedtaksperiode).map { it.type })
        assertTrue(inspektør.etterspurteBehov(3.vedtaksperiode).isEmpty())
    }
}
