package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/*
 * Disse testene er klokkesensitive fordi makstidsjekken i håndter påminnelse bruker LocalDateTime.now()
 */

internal class SykmeldingMakstidE2ETest : AbstractEndToEndTest() {

    private val TIDLIGST_MULIGE_UTBETALINGSDAG = LocalDate.now().withDayOfMonth(1).minusMonths(3)

    @Test
    fun `perioder venter på søknad ut måneden og tre måneder frem i tid - hele perioden i samme måned`() {
        val tom = TIDLIGST_MULIGE_UTBETALINGSDAG.minusDays(1)
        val fom = tom.minusDays(20)
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))

        val tom2 = tom.plusMonths(2)
        val fom2 = fom.plusMonths(2)
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom2, tom2, 100.prosent))
        håndterInntektsmelding(listOf(fom2 til fom2.plusDays(16)))

        håndterPåminnelse(1.vedtaksperiode, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
        assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
    }

    @ForventetFeil("Timingfeil?")
    @Test
    fun `perioder venter på søknad ut måneden og tre måneder frem i tid - perioden fordelt over to måneder`() {
        val tom = TIDLIGST_MULIGE_UTBETALINGSDAG
        val fom = tom.minusDays(20)
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterPåminnelse(
            1.vedtaksperiode,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
        )
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = LocalDate.now())
        håndterInntektsmelding(listOf(fom til fom.plusDays(16)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val tom2 = tom.plusMonths(2)
        val fom2 = fom.plusMonths(2)
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom2, tom2, 100.prosent))
        håndterInntektsmelding(listOf(fom2 til fom2.plusDays(16)))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
        assertEquals(1, utbetalingstidslinje.inspektør.navDagTeller)
        assertEquals(4, utbetalingstidslinje.inspektør.foreldetDagTeller)
    }
}
