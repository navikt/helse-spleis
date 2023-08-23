package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattEtterHovedregel
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattIInfotrygd
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VedtakFattetE2ETest : AbstractEndToEndTest() {

    @Test
    fun `sender vedtak fattet for perioder innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 10.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(0, inspektør.utbetalinger.size)
        assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertNull(event.utbetalingId)
        assertNull(event.sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1, inspektør.utbetalinger.size)
        assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertEquals(inspektør.utbetaling(0).inspektør.utbetalingId, event.utbetalingId)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetaling(0).inspektør.tilstand)
        val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
            omregnetÅrsinntekt = 372000.0,
            innrapportertÅrsinntekt = 372000.0,
            avviksprosent = 0.0,
            `6G` = 561804.0,
            tags = emptySet(),
            arbeidsgivere = listOf(FastsattEtterHovedregel.Arbeidsgiver(a1, 372000.0))
        )
        assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden med bare ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 20.januar, 100.prosent), Ferie(17.januar, 20.januar))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(0, inspektør.utbetalinger.size)
        assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        assertEquals(0, inspektør.utbetalinger(1.vedtaksperiode).size)
        val event = observatør.vedtakFattetEvent.getValue(1.vedtaksperiode.id(ORGNUMMER))
        assertNull(event.utbetalingId)
        assertNull(event.sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet for forlengelseperioder utenfor arbeidsgiverperioden med bare ferie`() {
        nyttVedtak(1.januar, 20.januar, 100.prosent)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(21.januar, 31.januar, 100.prosent), Ferie(21.januar, 31.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertEquals(2, inspektør.utbetalinger.size)
        assertEquals(1, observatør.utbetalingUtenUtbetalingEventer.size)
        assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(2, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.getValue(2.vedtaksperiode.id(ORGNUMMER))
        assertEquals(inspektør.utbetaling(1).inspektør.utbetalingId, event.utbetalingId)
        assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetaling(1).inspektør.tilstand)
        val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
            omregnetÅrsinntekt = 372000.0,
            innrapportertÅrsinntekt = 372000.0,
            avviksprosent = 0.0,
            `6G` = 561804.0,
            tags = emptySet(),
            arbeidsgivere = listOf(FastsattEtterHovedregel.Arbeidsgiver(a1, 372000.0))
        )
        assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet ved fastsettelse etter hovedregel med flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)), beregnetInntekt = INNTEKT, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)), beregnetInntekt = INNTEKT, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    a1 inntekt INNTEKT + 1000.månedlig
                    a2 inntekt INNTEKT + 2000.månedlig
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(2, observatør.vedtakFattetEvent.size)
        val a1Sykepengegrunnlagsfakta = observatør.vedtakFattetEvent.values.first { it.organisasjonsnummer == a1 }.sykepengegrunnlagsfakta
        val a2Sykepengegrunnlagsfakta = observatør.vedtakFattetEvent.values.first { it.organisasjonsnummer == a2 }.sykepengegrunnlagsfakta
        assertEquals(a1Sykepengegrunnlagsfakta, a2Sykepengegrunnlagsfakta)

        val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
            omregnetÅrsinntekt = 744000.0,
            innrapportertÅrsinntekt = 780000.0,
            avviksprosent = 4.62,
            `6G` = 599148.0,
            tags = setOf(PersonObserver.VedtakFattetEvent.Tag.`6GBegrenset`),
            arbeidsgivere = listOf(
                FastsattEtterHovedregel.Arbeidsgiver(a1, 372000.0),
                FastsattEtterHovedregel.Arbeidsgiver(a2, 372000.0),
            )
        )
        assertEquals(forventetSykepengegrunnlagsfakta, a1Sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet etter skjønnsmessig fastsettelse med flere arbeidsgivere`() = Toggle.AltAvTjuefemprosentAvvikssaker.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)), beregnetInntekt = 45000.månedlig, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar(2020) til 16.januar(2020)), beregnetInntekt = 44000.månedlig, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    a1 inntekt INNTEKT + 65.67.månedlig
                    a2 inntekt INNTEKT + 113.53.månedlig
                }
            }
        ))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)
        håndterSkjønnsmessigFastsettelse(1.januar(2020), listOf(OverstyrtArbeidsgiveropplysning(a1, 46000.månedlig), OverstyrtArbeidsgiveropplysning(a2, 45000.månedlig)))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertEquals(2, observatør.vedtakFattetEvent.size)
        val a1Sykepengegrunnlagsfakta = observatør.vedtakFattetEvent.values.first { it.organisasjonsnummer == a1 }.sykepengegrunnlagsfakta
        val a2Sykepengegrunnlagsfakta = observatør.vedtakFattetEvent.values.first { it.organisasjonsnummer == a2 }.sykepengegrunnlagsfakta
        assertEquals(a1Sykepengegrunnlagsfakta, a2Sykepengegrunnlagsfakta)

        val forventetSykepengegrunnlagsfakta = FastsattEtterSkjønn(
            omregnetÅrsinntekt = 1068000.0,
            innrapportertÅrsinntekt = 746150.40,
            skjønnsfastsatt = 1092000.0,
            avviksprosent = 43.13,
            `6G` = 599148.0,
            tags = setOf(PersonObserver.VedtakFattetEvent.Tag.`6GBegrenset`),
            arbeidsgivere = listOf(
                FastsattEtterSkjønn.Arbeidsgiver(a1, 540000.0, 552000.0),
                FastsattEtterSkjønn.Arbeidsgiver(a2, 528000.0, 540000.0),
            )
        )
        assertEquals(forventetSykepengegrunnlagsfakta, a1Sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet med sykepengegrunnlag fastsatt i Infotrygd`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(1.februar, 28.februar)
        assertEquals(1, observatør.vedtakFattetEvent.size)
        val event = observatør.vedtakFattetEvent.values.single()
        val forventetSykepengegrunnlagsfakta = FastsattIInfotrygd(372000.0)
        assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
    }

    @Test
    fun `legger ikke til utbetalingsId dersom status er forkastet`(){
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "noe", refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, null, emptyList()))
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val liste = (1..16).map{
            ManuellOverskrivingDag(it.januar, Dagtype.Feriedag)
        }
        håndterOverstyrTidslinje(liste)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        val utbetaling = inspektør.utbetalinger.single()
        assertEquals(Utbetalingstatus.FORKASTET, utbetaling.inspektør.tilstand)
        val event = observatør.vedtakFattetEvent.values.single()
        assertEquals(null, event.utbetalingId)
    }
}
