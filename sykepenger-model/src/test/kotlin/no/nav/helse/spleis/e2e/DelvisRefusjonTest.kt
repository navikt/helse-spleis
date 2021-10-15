package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DelvisRefusjonTest : AbstractEndToEndTest() {


    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag på`() = Toggles.RefusjonPerDag.enable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())

        inspektør.utbetalingstidslinjer(1.vedtaksperiode).forEach {
            it.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                val forventetArbeidsgiverbeløp = if (it is Utbetalingstidslinje.Utbetalingsdag.NavDag) 1431 else 0
                assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp)
                assertEquals(1431, arbeidsgiverRefusjonsbeløp)
                assertEquals(0, personbeløp)
            }
        }
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag av`() = Toggles.RefusjonPerDag.disable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach { assertEquals(1431, it.beløp) }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())

        inspektør.utbetalingstidslinjer(1.vedtaksperiode).forEach {
            it.økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                val forventetArbeidsgiverbeløp = if (it is Utbetalingstidslinje.Utbetalingsdag.NavDag) 1431 else 0
                assertEquals(forventetArbeidsgiverbeløp, arbeidsgiverbeløp)
                assertEquals(null, arbeidsgiverRefusjonsbeløp)
                assertEquals(0, personbeløp)
            }
        }
    }

    @Test
    fun `Arbeidsgiverperiode tilstøter Infotrygd`() = Toggles.RefusjonPerDag.enable { //TODO (Holder det å sjekke 1 dag tilbake i tid isteden for 16? [RefusjonsHistorikk.kt:45])
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertFalse(inspektør.warnings.contains("Fant ikke refusjon for perioden. Defaulter til full refusjon."))
    }


    @Test
    fun `Finner refusjon ved forlengelse fra Infotrygd`() = Toggles.RefusjonPerDag.enable {
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)
        assertFalse(inspektør.warnings.contains("Fant ikke refusjon for perioden. Defaulter til full refusjon."))
    }

}
