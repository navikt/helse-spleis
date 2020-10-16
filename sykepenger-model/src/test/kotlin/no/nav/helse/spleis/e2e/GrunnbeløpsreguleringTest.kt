package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.mai
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class GrunnbeløpsreguleringTest : AbstractEndToEndTest() {

    @Test
    fun `Skal g reguleres hvis virkning fra etter virkningsdato`() {
        val årligInntekt = 800000.00.årlig
        håndterSykmelding(Sykmeldingsperiode(1.mai(2020), 31.mai(2020), 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.mai(2020), 15.mai(2020))), refusjon = Triple(null, årligInntekt, emptyList()))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mai(2020), 31.mai(2020), gradFraSykmelding = 100), sendtTilNav = 31.mai(2020))
        håndterVilkårsgrunnlag(1.vedtaksperiode, årligInntekt)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterGrunnbeløpsregulering(virkningFra = 22.september(2020))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }
}
