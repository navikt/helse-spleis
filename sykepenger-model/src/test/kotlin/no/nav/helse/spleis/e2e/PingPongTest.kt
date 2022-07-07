package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class PingPongTest : AbstractEndToEndTest() {

    @Test
    fun `Infotrygd betaler gap etter vi har betalt perioden etterpå`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.februar, 9.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 5.februar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)

        assertForventetFeil(
            forklaring = "Fordi OppdragBuilder stopper ved første ukjente dag (les Infotrygd-dag), vil vi for perioden 1.mars - 31.mars lage et oppdrag som starter 10. februar." +
                "Dette oppdraget matches så mot det forrige vi har utbetalt og vi kommer frem til at vi skal opphøre perioden 1.januar - 31.januar først.",
            nå = {
                assertWarning("Utbetalingens fra og med-dato er endret. Kontroller simuleringen", 3.vedtaksperiode.filter(ORGNUMMER))
                val første = inspektør.utbetaling(0)
                val utbetaling = inspektør.utbetaling(2)
                val utbetalingInspektør = utbetaling.inspektør
                assertEquals(første.inspektør.korrelasjonsId, utbetalingInspektør.korrelasjonsId)
                assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].datoStatusFom())
                assertEquals(10.februar til 30.mars, utbetalingInspektør.arbeidsgiverOppdrag[1].let { it.fom til it.tom })
            },
            ønsket = {
                /* vet ikke helt hva som er best her, men vi burde nok la saksbehandlere likevel få se perioden slik at de kan annullere personen */
                assertFalse(true)
            }
        )
    }

    @Test
    fun `riktig skjæringstidspunkt ved spleis - infotrygd - spleis`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar,  28.februar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(2.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `kort periode - infotrygd - spleis --- inntekt kommer fra infotrygd`() = Toggle.IkkeForlengInfotrygdperioder.disable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))
        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT - 100.månedlig, inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.beregningsgrunnlag)
    }

    @Test
    fun `spleis - infotrygd - spleis --- inntekt kommer fra første periode`() = Toggle.IkkeForlengInfotrygdperioder.disable {
        nyttVedtak(20.desember(2017), 16.januar)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 27.januar, 100.prosent)) // <-- kastes ut

        håndterInntektsmelding(listOf(20.desember(2017) til 5.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(20.desember(2017), inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT, inspektør.vilkårsgrunnlag(3.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.beregningsgrunnlag)
    }

    @Test
    fun `Kaster ut alt om vi oppdager at en senere periode er utbetalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        // Ingen søknad for første sykmelding - den sykmeldte sender den ikke inn eller vi er i et out of order-scenario

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))

        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(a1, 17.februar, 20.februar, 100.prosent, INNTEKT))
        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
    }
}
