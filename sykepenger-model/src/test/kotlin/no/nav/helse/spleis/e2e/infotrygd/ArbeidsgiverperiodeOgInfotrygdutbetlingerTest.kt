package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mandag
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodeOgInfotrygdutbetlingerTest: AbstractDslTest() {

    @Test
    fun `Utbetalt fra første dag i Infotrygd`() {
        assertEquals(
            emptyList<Periode>(),
            arbeidsgiverperiodeVed(vedtaksperiode = januar, infotrygdutbetaling = 1.januar)
        )
        observatør.assertEtterspurt(1.vedtaksperiode, PersonObserver.Inntekt::class, PersonObserver.Refusjon::class)
    }

    @Test
    fun `Utbetalt fra andre dag i Infotrygd`() {
        assertEquals(
            listOf(1.januar.somPeriode()),
            arbeidsgiverperiodeVed(vedtaksperiode = januar, infotrygdutbetaling = 2.januar)
        )
    }

    @Test
    fun `Vedtaksperiode starter på lørdag, Infotrygd utbetaler fra mandag`() {
        assertEquals(
            listOf(6.januar til 7.januar),
            arbeidsgiverperiodeVed(vedtaksperiode = lørdag(6.januar) til 31.januar, infotrygdutbetaling = mandag(8.januar))
        )
    }

    @Test
    fun `Infotrygd utbetalt med 15 dagers gap til vedtaksperiode`() {
        val vedtaksperiode = 17.januar til 17.februar
        val infotrygdutbetaling = 1.januar
        assertEquals(15, infotrygdutbetaling.somPeriode().periodeMellom(vedtaksperiode.start)?.count())
        assertEquals(
            emptyList<Periode>(),
            arbeidsgiverperiodeVed(vedtaksperiode, infotrygdutbetaling)
        )
    }

    @Test
    fun `Infotrygd utbetalt med 16 dagers gap til vedtaksperiode`() {
        val vedtaksperiode = 18.januar til 18.februar
        val infotrygdutbetaling = 1.januar
        assertEquals(16, infotrygdutbetaling.somPeriode().periodeMellom(vedtaksperiode.start)?.count())
        assertEquals(
            listOf(18.januar til 2.februar),
            arbeidsgiverperiodeVed(vedtaksperiode, infotrygdutbetaling)
        )
    }

    private fun arbeidsgiverperiodeVed(vedtaksperiode: Periode, infotrygdutbetaling: LocalDate): List<Periode> = a1 {
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(orgnummer, infotrygdutbetaling, infotrygdutbetaling))
        val vedtaksperiodeId = håndterSøknad(vedtaksperiode)!!
        if (vedtaksperiode.overlapperMed(infotrygdutbetaling.somPeriode())) assertVarsler(vedtaksperiodeId, RV_IT_3)
        inspektør.arbeidsgiverperiode(vedtaksperiodeId)
    }
}
