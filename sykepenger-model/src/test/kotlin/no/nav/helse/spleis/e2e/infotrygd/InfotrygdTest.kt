package no.nav.helse.spleis.e2e.infotrygd

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver.VedtaksperiodeVenterEvent
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_37
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.IdInnhenter
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertHarIkkeTag
import no.nav.helse.spleis.e2e.assertHarTag
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykepengegrunnlagForArbeidsgiver
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `Saksbehandler legger til en utbetaling i forkant av en periode som er lagt til grunn ved infotrygdovergang`() {
        createOvergangFraInfotrygdPerson()
        assertEquals(emptyList<VedtaksperiodeVenterEvent>(), observatør.vedtaksperiodeVenter)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        val refusjonFør = inspektør.refusjon(1.vedtaksperiode)

        val eksisterendeUtbetaling = ArbeidsgiverUtbetalingsperiode("a1", 1.januar, 31.januar)
        val nyUtbetaling = ArbeidsgiverUtbetalingsperiode("a1", 20.desember(2017), 31.desember(2017))
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(eksisterendeUtbetaling, nyUtbetaling)

        assertEquals(20.desember(2017), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(emptyList<VedtaksperiodeVenterEvent>(), observatør.vedtaksperiodeVenter)
        nullstillTilstandsendringer()
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)

        this@InfotrygdTest.håndterSykepengegrunnlagForArbeidsgiver(skjæringstidspunkt = 20.desember(2017))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@InfotrygdTest.håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertVarsler(listOf(RV_IV_10, RV_IT_14), 1.vedtaksperiode.filter(a1))
        assertEquals(refusjonFør, inspektør.refusjon(1.vedtaksperiode))
    }

    @Test
    fun `Legger på en tag når perioden til godkjenning overlapper med en periode i Infotrygd`() {
        nyttVedtak(januar)
        assertEquals(1, observatør.utkastTilVedtakEventer.size)
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        this@InfotrygdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsler(listOf(RV_IT_3), 1.vedtaksperiode.filter())
        assertEquals(2, observatør.utkastTilVedtakEventer.size)
        hendelselogg.assertHarTag(
            vedtaksperiode = 1.vedtaksperiode,
            forventetTag = "OverlapperMedInfotrygd"
        )

        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring()
        this@InfotrygdTest.håndterYtelser(1.vedtaksperiode)

        hendelselogg.assertHarIkkeTag(
            vedtaksperiode = 1.vedtaksperiode,
            ikkeForventetTag = "OverlapperMedInfotrygd"
        )
    }

    @Test
    fun `Arbeidsgiverperiode utført i Infotrygd med kort gap til periode i Spleis som utbetales i Infotrygd mens den står til godkjenning`() {
        nyttVedtak(10.februar til 28.februar)
        val februarKorrelasjonsId = gjeldendeKorrelasjonsId(1.vedtaksperiode)
        assertEquals(listOf(10.februar til 25.februar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))

        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))

        this@InfotrygdTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@InfotrygdTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertEquals(februarKorrelasjonsId, gjeldendeKorrelasjonsId(1.vedtaksperiode))
        assertVarsler(listOf(RV_IT_37), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IT_37), 1.vedtaksperiode.filter())

        håndterSøknad(10.mars til 31.mars)
        håndterArbeidsgiveropplysninger(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            arbeidsgiverperioder = null
        )
        assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@InfotrygdTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)

        // Mens Mars står til godkjenning utbetales den i Infotrygd
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar), ArbeidsgiverUtbetalingsperiode(a1, 10.mars, 31.mars))
        assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        this@InfotrygdTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertNotEquals(februarKorrelasjonsId, gjeldendeKorrelasjonsId(2.vedtaksperiode))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertVarsler(listOf(RV_IT_3), 2.vedtaksperiode.filter())

        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        this@InfotrygdTest.håndterYtelser(2.vedtaksperiode)
    }

    @Test
    fun `Når perioden utbetales i Infotrygd kan det medføre at vi feilaktig annullerer tidligere utebetalte perioder`() {
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        nyttVedtak(mars)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        val korrelasjonsIdMars = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

        håndterSøknad(juli)
        håndterArbeidsgiveropplysninger(listOf(1.juli til 16.juli), vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)

        assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)

        this@InfotrygdTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar), ArbeidsgiverUtbetalingsperiode(a1, 1.juli, 31.juli))

        assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)

        this@InfotrygdTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@InfotrygdTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        val korrelasjonsIdJuli = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
        håndterUtbetalt()

        assertNotEquals(korrelasjonsIdMars, korrelasjonsIdJuli)
        assertTrue(inspektør.utbetalinger.none { it.erAnnullering })
        assertVarsler(listOf(RV_IT_3), 3.vedtaksperiode.filter())

        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        this@InfotrygdTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        val nyKorrelasjonsIdJuli = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
        assertNotEquals(korrelasjonsIdMars, nyKorrelasjonsIdJuli)
        assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)
        assertVarsler(listOf(RV_UT_23, RV_IT_3), 3.vedtaksperiode.filter())
    }

    @Test
    fun `En uheldig bivirkning av å behandle perioder uten AGP`() {
        nyttVedtak(1.januar(2017) til 31.januar(2017))

        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.mars(2017), 10.mars(2017)))

        nyttVedtak(februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        inspektør.utbetalinger(2.vedtaksperiode).last().inspektør.korrelasjonsId

        nyttVedtak(april, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        inspektør.utbetalinger(3.vedtaksperiode).last().inspektør.korrelasjonsId

        håndterSøknad(4.juni til 6.juni)
        assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(emptyList(), førsteFraværsdag = 4.juni, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
        håndterVilkårsgrunnlag(4.vedtaksperiode)
        this@InfotrygdTest.håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        this@InfotrygdTest.håndterOverstyrTidslinje((4.juni til 6.juni).map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
        // _veldig_ viktig detalj: En periode uten AGP
        // Når vi finner utbetalingen vi skal bygge videre på tolkes tom AGP som Infotrygd, så vi bygger videre på første utbetaling
        // etter siste infotrygdutbetaling, og eventuelle utbetalinger som ligger mellom blir annullert.
        // Før var dette en riktig antagelse fordi tom AGP som ikke skyltes Infotrygd skulle til AUU
        // Men det er gjort en endring slik at en periode som har vært beregnet aldri skal inn i AUU
        assertEquals(emptyList<Periode>(), inspektør.arbeidsgiverperiode(4.vedtaksperiode))

        // hvis denne vedtaksperioden går til godkjenning så har det tidligere hendt
        // at vi har laget et feilaktig annulleringsoppdrag
        assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertVarsler(listOf(RV_IM_8), 4.vedtaksperiode.filter())
    }

    @Test
    fun `infotrygd flytter skjæringstidspunkt`() {
        nyttVedtak(januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 9.februar))
        assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().first().vilkårsgrunnlag.size)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Infotrygdhistorikk som er nærme`() {
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar))
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `Infotrygdhistorikk som ikke medfører utkasting`() {
        this@InfotrygdTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar))
        )
        håndterSøknad(Sykdom(20.februar, 28.mars, 100.prosent))
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `eksisterende infotrygdforlengelse`() {
        createOvergangFraInfotrygdPerson()
        nyPeriode(mars)
        this@InfotrygdTest.håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Kan ikke overstyre inntekt på Infotrygd-sykepengegrunnlag`() {
        createOvergangFraInfotrygdPerson()
        val antallInnslagFør = inspektør.vilkårsgrunnlagHistorikkInnslag().size

        this@InfotrygdTest.håndterOverstyrArbeidsgiveropplysninger(
            1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(
                a1, 15000.månedlig,
                emptyList()
            )
        )
        )
        assertEquals(antallInnslagFør, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
    }

    @Test
    fun `Kan endre refusjonsopplysninger på Infotrygd-sykepengegrunnlag, men inntekten ignoreres`() {
        createOvergangFraInfotrygdPerson()
        val antallInnslagFør = inspektør.vilkårsgrunnlagHistorikkInnslag().size

        val meldingsreferanse = UUID.randomUUID()
        this@InfotrygdTest.håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1, 15000.månedlig,
                    listOf(Triple(1.januar, null, 15000.månedlig))
                )
            ),
            meldingsreferanseId = meldingsreferanse
        )
        assertEquals(antallInnslagFør, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

        assertBeløpstidslinje(Beløpstidslinje.fra(februar, 15000.månedlig, meldingsreferanse.saksbehandler), inspektør.refusjon(1.vedtaksperiode))
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    private fun gjeldendeKorrelasjonsId(vedtaksperiodeIdInnhenter: IdInnhenter) =
        inspektør.vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
}
