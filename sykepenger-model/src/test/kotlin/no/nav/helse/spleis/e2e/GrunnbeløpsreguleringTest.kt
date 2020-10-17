package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.mai
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnbeløpsreguleringTest : AbstractEndToEndTest() {
    private companion object {
        private val HØY_INNTEKT = 800000.00.årlig
        private val VIRKNINGSDATO_2020_GRUNNBELØP = 21.september(2020)
        private val GYLDIGHETSDATO_2020_GRUNNBELØP = 1.mai(2020)
    }

    @Test
    fun `justere periode etter ny g-sats, men før virkningsdato`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(virkningFra = VIRKNINGSDATO_2020_GRUNNBELØP)
        håndterYtelser(1.vedtaksperiode) // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, AKSEPTERT)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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

    @Test
    fun `justere periode som deler oppdrag med en periode som ikke skal justeres`() {
        val fomPeriode1 = 30.april(2020)
        val tomPeriode1 = fomPeriode1.plusMonths(1)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fomPeriode1, tomPeriode1)

        val fomPeriode2 = tomPeriode1.plusDays(7)
        val tomPeriode2 = fomPeriode2.plusMonths(1)
        utbetaltVedtaksperiodeBegrensetAv6G(2, fomPeriode2, tomPeriode2, listOf(Periode(fomPeriode1, fomPeriode1.plusDays(15))), fomPeriode2)

        håndterGrunnbeløpsregulering(virkningFra = VIRKNINGSDATO_2020_GRUNNBELØP)
        håndterYtelser(2.vedtaksperiode) // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, AKSEPTERT)

        // TODO: sjekke at Oppdraget ser OK ut

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
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

    @Test
    fun `periode som skal ha 2019-sats får ikke 2020-sats`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP.minusDays(1)
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(virkningFra = VIRKNINGSDATO_2020_GRUNNBELØP)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `periode med nytt grunnbeløp trengs ikke justering`() {
        val fom = VIRKNINGSDATO_2020_GRUNNBELØP
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(virkningFra = VIRKNINGSDATO_2020_GRUNNBELØP)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `g-regulering før virkningsdato har ikke virkning`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(virkningFra = VIRKNINGSDATO_2020_GRUNNBELØP.minusDays(1))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    private fun utbetaltVedtaksperiodeBegrensetAv6G(
        vedtaksperiodeIndeks: Int,
        fom: LocalDate,
        tom: LocalDate,
        arbeidsgiverperiode: List<Periode> = listOf(Periode(
            fom,
            fom.plusDays(15)
        )),
        førsteFraværsdag: LocalDate = fom
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, gradFraSykmelding = 100), sendtTilNav = tom)
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIndeks.vedtaksperiode,
            arbeidsgiverperiode,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = Triple(null, HØY_INNTEKT, emptyList())
        )
        håndterVilkårsgrunnlag(vedtaksperiodeIndeks.vedtaksperiode, HØY_INNTEKT)
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode) // No history
        håndterSimulering(vedtaksperiodeIndeks.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiodeIndeks.vedtaksperiode, true)
        håndterUtbetalt(vedtaksperiodeIndeks.vedtaksperiode, AKSEPTERT)
    }
}
