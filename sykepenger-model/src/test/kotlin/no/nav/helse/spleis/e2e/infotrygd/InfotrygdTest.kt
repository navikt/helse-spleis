package no.nav.helse.spleis.e2e.infotrygd

import java.util.*
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
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
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `infotrygd flytter skjæringstidspunkt`() {
        nyttVedtak(januar)
        nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 9.februar))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().first().vilkårsgrunnlag.size)
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

        assertInntektsgrunnlag(1.januar, a1, INNTEKT)
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
        assertInntektsgrunnlag(1.januar, a1, INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }
}
