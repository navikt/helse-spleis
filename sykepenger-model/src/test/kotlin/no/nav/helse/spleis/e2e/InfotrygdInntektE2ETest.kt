package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdInntektE2ETest : AbstractEndToEndTest() {

    @Test
    fun `bruker laveste registrerte inntekt i infotrygd om det er fler inntekter samme dato for samme arbeidsgiver`() {
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 25000.månedlig, true, null),
            Inntektsopplysning(ORGNUMMER, 1.januar, 23000.månedlig, false, null),
            Inntektsopplysning(ORGNUMMER, 1.januar, 24000.månedlig, true, 1.januar)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        assertInntektForDato(23000.månedlig, 1.januar, inspektør = inspektør)
        assertWarning("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato. Kontroller sykepengegrunnlaget.")
    }
}
