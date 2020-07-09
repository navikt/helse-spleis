package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Test

internal class ToArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        private const val rød = "rød"
        private const val blå = "blå"
    }

    private val rødInspektør get() = TestArbeidsgiverInspektør(person, rød)
    private val blåInspektør get() = TestArbeidsgiverInspektør(person, blå)

    @Test
    fun `overlappende arbeidsgivere sendt til infotrygd`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = blå))

        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = rød))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(vilkårsgrunnlag(rød.id(0), INNTEKT, orgnummer = rød))
        assertNoErrors(rødInspektør)

        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = blå))
        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = blå))
        person.håndter(vilkårsgrunnlag(blå.id(0), INNTEKT, orgnummer = blå))
        assertNoErrors(blåInspektør)

        person.håndter(ytelser(rød.id(0), orgnummer = rød))
        assertErrors(rødInspektør)
        assertErrors(blåInspektør)
        assertTilstander(
            rød.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertTilstander(
            blå.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = rød))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(vilkårsgrunnlag(rød.id(0), INNTEKT, orgnummer = rød))
        person.håndter(ytelser(rød.id(0), orgnummer = rød))
        person.håndter(simulering(rød.id(0), orgnummer = rød))
        person.håndter(utbetalingsgodkjenning(rød.id(0), true, orgnummer = rød))
        person.håndter(utbetaling(rød.id(0), AKSEPTERT, orgnummer = rød))

        assertNoErrors(rødInspektør)
        assertTilstander(
            rød.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        person.håndter(sykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100), orgnummer = blå))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100), orgnummer = blå))
        person.håndter(inntektsmelding(listOf(Periode(1.mars, 16.mars)), orgnummer = blå))
        person.håndter(vilkårsgrunnlag(blå.id(0), INNTEKT, orgnummer = blå))
        person.håndter(ytelser(blå.id(0), orgnummer = blå))
        person.håndter(simulering(blå.id(0), orgnummer = blå))
        person.håndter(utbetalingsgodkjenning(blå.id(0), true, orgnummer = blå))
        person.håndter(utbetaling(blå.id(0), AKSEPTERT, orgnummer = blå))

        assertNoErrors(rødInspektør)
        assertTilstander(
            blå.id(0),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }
}
