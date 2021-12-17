package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.ForlengelseFraInfotrygd.NEI
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class DeleGrunnlagsdataTest : AbstractEndToEndTest() {

    @Test
    fun `vilkårsgrunnlag deles med påfølgende tilstøtende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.april, 30.april, 100.prosent))
        val inntektsmelding1Id = håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        val inntektsmelding2Id = håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 5.april)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterVilkårsgrunnlag(4.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.april(2017) til 1.mars(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt(4.vedtaksperiode)

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNotEquals(inspektør.vilkårsgrunnlag(3.vedtaksperiode), inspektør.vilkårsgrunnlag(4.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(1.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(2.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(3.vedtaksperiode))
        assertTrue(inntektsmelding2Id in inspektør.hendelseIder(4.vedtaksperiode))
    }

    @Test
    fun `vilkårsgrunnlag deles med foregående`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.april, 30.april, 100.prosent))

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(4.vedtaksperiode))
    }

    @Test
    fun `vilkårsgrunnlag hentes ikke når perioden er overgang fra IT`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 1.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        val vilkårsgrunnlagPeriode1 = inspektør.vilkårsgrunnlag(1.vedtaksperiode)
        val vilkårsgrunnlagPeriode2 = inspektør.vilkårsgrunnlag(2.vedtaksperiode)
        assertTrue(vilkårsgrunnlagPeriode1 is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag)
        assertSame(vilkårsgrunnlagPeriode1, vilkårsgrunnlagPeriode2)
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `ingen vilkårsgrunnlag når perioden har opphav i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(25.februar, 28.februar))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar, 31.januar, 100.prosent, 15000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.mars)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode) is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag)
        assertTrue(inspektør.vilkårsgrunnlag(2.vedtaksperiode) is VilkårsgrunnlagHistorikk.Grunnlagsdata)
        assertEquals(JA, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `inntektsmelding bryter ikke opp forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(18.januar, 1.februar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(18.januar, 1.februar)), førsteFraværsdag = 4.mars)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `setter ikke inntektsmeldingId flere ganger`() {
        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(20.februar, 28.februar, 100.prosent))

        val sykmeldingId = håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(3, inspektør.hendelseIder(2.vedtaksperiode).size)
        assertTrue(inspektør.hendelseIder(2.vedtaksperiode).containsAll(listOf(sykmeldingId, søknadId, inntektsmeldingId)))
    }

    @Test
    fun `vilkårsgrunnlag tilbakestilles når vi ikke er en forlengelse likevel`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(
            1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent),
            Arbeid(18.januar, 21.januar)
        )
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(22.januar, 22.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 22.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertNotEquals(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(1.vedtaksperiode))
        assertEquals(NEI, inspektør.forlengelseFraInfotrygd(2.vedtaksperiode))
    }

    @Test
    fun `når vilkårsgrunnlag mangler sjekk på minimum inntekt gjøres denne sjekken - søknad arbeidsgiver`() {
        val inntekt = 93634.årlig / 2 - 1.årlig
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = inntekt
        )

        val vilkårsgrunnlagElement = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = sykepengegrunnlag(inntekt, listOf(
                ArbeidsgiverInntektsopplysning(
                    ORGNUMMER.toString(),
                    Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), inntekt)
                )
            )),
            sammenligningsgrunnlag = inntekt,
            avviksprosent = Prosent.prosent(0.0),
            antallOpptjeningsdagerErMinst = 29,
            harOpptjening = true,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = null,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )

        // Gjøres utelukkende for å teste oppførsel som ikke skal kunne skje lenger
        // (minimumInntekt kan være null i db, men ikke i modellen, mangler en migrering)
        val vilkårsgrunnlagHistorikk = PersonInspektør(person).vilkårsgrunnlagHistorikk
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlagElement)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 17.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(17.januar, 17.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val grunnlagsdataInspektør = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!)
        assertEquals(false, grunnlagsdataInspektør.harMinimumInntekt)
        inspektør.utbetalingUtbetalingstidslinje(0).inspektør.also {
            assertEquals(0, it.navDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
            assertEquals(23, it.avvistDagTeller)
        }
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `når vilkårsgrunnlag mangler sjekk på minimum inntekt gjøres denne sjekken`() {
        val inntekt = 93634.årlig / 2 - 1.årlig
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 17.januar, 100.prosent), SendtSøknad.Søknadsperiode.Ferie(17.januar, 17.januar))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = inntekt
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt)
        val vilkårsgrunnlagElement = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = sykepengegrunnlag(
                inntekt,
                listOf(
                    ArbeidsgiverInntektsopplysning(
                        ORGNUMMER.toString(),
                        Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), inntekt)
                    )
                )
            ),
            sammenligningsgrunnlag = inntekt,
            avviksprosent = Prosent.prosent(0.0),
            antallOpptjeningsdagerErMinst = 29,
            harOpptjening = true,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = null,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // Gjøres utelukkende for å teste oppførsel som ikke skal kunne skje lenger
        // (minimumInntekt kan være null i db, men ikke i modellen, mangler en migrering)
        val vilkårsgrunnlagHistorikk = PersonInspektør(person).vilkårsgrunnlagHistorikk
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlagElement)

        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET_UTEN_UTBETALING
        )
        håndterSykmelding(Sykmeldingsperiode(18.januar, 18.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(18.januar, 18.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val grunnlagsdataInspektør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.let { GrunnlagsdataInspektør(it) }
        assertEquals(false, grunnlagsdataInspektør?.harMinimumInntekt)

        inspektør.utbetalingUtbetalingstidslinje(1).inspektør.also {
            assertEquals(0, it.navDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
            assertEquals(22, it.avvistDagTeller)
        }
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
    }

    @Test
    fun `når vilkårsgrunnlag mangler sjekk på minimum inntekt gjøres denne sjekken - inntekt er lik minimum inntekt`() {
        val inntekt = 93634.årlig / 2
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 17.januar, 100.prosent), SendtSøknad.Søknadsperiode.Ferie(17.januar, 17.januar))
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = inntekt
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt)
        val vilkårsgrunnlagElement = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = sykepengegrunnlag(
                inntekt,
                listOf(
                    ArbeidsgiverInntektsopplysning(
                        ORGNUMMER.toString(),
                        Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), inntekt)
                    )
                )
            ),
            sammenligningsgrunnlag = inntekt,
            avviksprosent = Prosent.prosent(0.0),
            antallOpptjeningsdagerErMinst = 29,
            harOpptjening = true,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = null,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        håndterYtelser(1.vedtaksperiode)

        // Gjøres utelukkende for å teste oppførsel som ikke skal kunne skje lenger
        // (minimumInntekt kan være null i db, men ikke i modellen, mangler en migrering)
        val vilkårsgrunnlagHistorikk = PersonInspektør(person).vilkårsgrunnlagHistorikk
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlagElement)

        håndterSykmelding(Sykmeldingsperiode(18.januar, 18.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(18.januar, 18.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val grunnlagsdataInspektør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.let { GrunnlagsdataInspektør(it) }
        assertEquals(true, grunnlagsdataInspektør?.harMinimumInntekt)
    }

    @Test
    fun `Bruker ikke vilkårsgrunnlag for annet skjæringstidpunkt ved beregning av utbetalingstidslinje, selv om skjæringstidspunktet er senere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(
            1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)


        håndterSykmelding(Sykmeldingsperiode(22.januar, 10.februar, 100.prosent))
        håndterSøknadMedValidering(
            2.vedtaksperiode, Sykdom(22.januar, 10.februar, 100.prosent)
        )
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 15.desember(2017), 21.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 15.desember(2017), INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        inspektør.utbetalingUtbetalingstidslinje(1).inspektør.also {
            assertEquals(15, it.navDagTeller)
            assertEquals(0, it.arbeidsgiverperiodeDagTeller)
            assertEquals(0, it.avvistDagTeller)
        }
    }

    private fun sykepengegrunnlag(inntekt: Inntekt, arbeidsgiverInntektsopplysning: List<ArbeidsgiverInntektsopplysning> = listOf()) = Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysning,
        sykepengegrunnlag = inntekt,
        grunnlagForSykepengegrunnlag = inntekt,
        begrensning = ER_IKKE_6G_BEGRENSET
    )
}
