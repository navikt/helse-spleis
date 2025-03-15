package no.nav.helse.spleis.e2e.infotrygd

import java.util.*
import no.nav.helse.april
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `En uheldig bivirkning av å behandle perioder uten AGP`() {
        nyttVedtak(1.januar(2017) til 31.januar(2017))

        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.mars(2017), 10.mars(2017), 100.prosent, INNTEKT))

        nyttVedtak(februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        inspektør.utbetalinger(2.vedtaksperiode).last().inspektør.korrelasjonsId

        nyttVedtak(april, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        inspektør.utbetalinger(3.vedtaksperiode).last().inspektør.korrelasjonsId

        håndterSøknad(4.juni til 6.juni)
        assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(emptyList(),  førsteFraværsdag = 4.juni, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
        håndterVilkårsgrunnlag(4.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterOverstyrTidslinje((4.juni til 6.juni).map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
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
        håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 9.februar))
        assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().first().vilkårsgrunnlag.size)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Infotrygdhistorikk som er nærme`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true))
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `Infotrygdhistorikk som ikke medfører utkasting`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true))
        )
        håndterSøknad(Sykdom(20.februar, 28.mars, 100.prosent))
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `eksisterende infotrygdforlengelse`() {
        createOvergangFraInfotrygdPerson()
        nyPeriode(mars)
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Kan ikke overstyre inntekt på Infotrygd-sykepengegrunnlag`() {
        createOvergangFraInfotrygdPerson()
        val antallInnslagFør = inspektør.vilkårsgrunnlagHistorikkInnslag().size

        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 15000.månedlig,
            emptyList())))
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
        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(a1, 15000.månedlig,
                listOf(Triple(1.januar, null, 15000.månedlig)))),
            meldingsreferanseId = meldingsreferanse
        )
        assertEquals(antallInnslagFør, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

        assertBeløpstidslinje(Beløpstidslinje.fra(februar, 15000.månedlig, meldingsreferanse.saksbehandler), inspektør.refusjon(1.vedtaksperiode))
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }
}
