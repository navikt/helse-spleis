package no.nav.helse.person.filter

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class BrukerutbetalingfilterTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `refusjon passer filter`() {
        assertTrue(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("31108512345"), Periodetype.FØRSTEGANGSBEHANDLING, lagUtbetaling(), Inntektskilde.EN_ARBEIDSGIVER))
    }

    @Test
    fun `ingen refusjon passer filter`() {
        val ingenRefusjon = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
        assertTrue(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("31108512345"), Periodetype.FØRSTEGANGSBEHANDLING, ingenRefusjon, Inntektskilde.EN_ARBEIDSGIVER))
    }

    @Test
    fun `feil periodetype`() {
        assertFalse(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("31108512345"), Periodetype.FORLENGELSE, lagUtbetaling(), Inntektskilde.EN_ARBEIDSGIVER))
    }

    @Test
    fun `feil fødselsdato passer ikke`() {
        assertFalse(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("02108512345"), Periodetype.FØRSTEGANGSBEHANDLING, lagUtbetaling(), Inntektskilde.EN_ARBEIDSGIVER))
    }

    @Test
    fun `delvis refusjon passer ikke`() {
        val delvisRefusjon = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600)))
        assertFalse(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("31108512345"), Periodetype.FØRSTEGANGSBEHANDLING, delvisRefusjon, Inntektskilde.EN_ARBEIDSGIVER))
    }

    @Test
    fun `inntektskilde fra flere arbeidsgivere passer ikke`() {
        assertFalse(Brukerutbetalingfilter(Fødselsnummer.tilFødselsnummer("31108512345"), Periodetype.FØRSTEGANGSBEHANDLING, lagUtbetaling(), Inntektskilde.FLERE_ARBEIDSGIVERE))
    }

    private fun assertTrue(filter: Featurefilter) {
        assertTrue(filter.filtrer(aktivitetslogg)) { "Forventet at filteret skal være sant.\n$aktivitetslogg"}
    }

    private fun assertFalse(filter: Featurefilter) {
        assertFalse(filter.filtrer(aktivitetslogg)) { "Forventet at filteret skal være usant.\n$aktivitetslogg"}
    }

    private fun lagUtbetaling(tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV), forrige: Utbetaling? = null): Utbetaling {
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            tidslinje.first().dato
        ).betal()

        return Utbetaling.lagUtbetaling(
            forrige?.let { listOf(forrige) } ?: emptyList(),
            "fnr",
            UUID.randomUUID(),
            "orgnr",
            tidslinje,
            tidslinje.last().dato,
            aktivitetslogg,
            LocalDate.MAX,
            0,
            0,
            forrige
        )
    }
}
