package no.nav.helse.bugs_showstoppers

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class RareFeilOgBugs : AbstractEndToEndTest() {
    // Skjedd som følge av at vi ikke har greid å deserialisere infotrygdhistorikken riktig,
    // slik at vi plutselig mangler historikken der vi tidligere hadde den. Dette medfører at vi tror vi har et annet skjæringstidspunkt
    // enn det vi egentlig har, som igjen gjør at vi ikke finner noen inntekt
    @Test
    fun `perioder som feilaktig er stuck i AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE blir unstuck når vi får oppdatert historikk`() {
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 1.januar, INNTEKT, true),
        )

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = inntektshistorikk
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        // skal ikke kunne skje i virkeligheten, brukes for å reprodusere bug der vi ikke lenger
        // greier å deserialisere eksisterende infotrygdhistorikk riktig
        håndterUtbetalingshistorikkUtenValidering()

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }
}
