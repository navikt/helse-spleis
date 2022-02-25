package no.nav.helse.person.filter

import no.nav.helse.hentErrors
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsfilterTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `full refusjon kan utbetales`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling())
            .inntektkilde(FLERE_ARBEIDSGIVERE)
            .inntektsmeldingtidsstempel(LocalDateTime.now().minusHours(25))
            .build()
        filter.assertKanUtbetales()
    }

    @Test
    fun `ingen refusjon for en arbeidsgiver kan utbetales`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling())
            .inntektkilde(EN_ARBEIDSGIVER)
            .build()
        filter.assertKanUtbetales()
    }

    @Test
    fun `ingen refusjon på utbetalingen for en arbeidsgiver med gammel inntektsmelding`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0))))
            .inntektkilde(EN_ARBEIDSGIVER)
            .inntektsmeldingtidsstempel(LocalDateTime.now().minusHours(25))
            .build()
        filter.assertKanUtbetales()
    }

    @Test
    fun `ingen refusjon for en arbeidsgiver med for gammel inntektsmelding`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(true)
            .utbetaling(lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0))))
            .inntektkilde(EN_ARBEIDSGIVER)
            .inntektsmeldingtidsstempel(LocalDateTime.now().minusHours(25))
            .build()
        filter.assertKanIkkeUtbetales("Ikke kandidat for brukerutbetaling ettersom inntektsmelding ble mottatt for mer enn 24 timer siden")
    }

    @Test
    fun `delvis refusjon kan utbetales`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600))))
            .inntektkilde(EN_ARBEIDSGIVER)
            .build()
        filter.assertKanUtbetales()
    }

    @Test
    fun `flere arbeidsgivere uten brukerutbetaling kan betales`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling())
            .inntektkilde(FLERE_ARBEIDSGIVERE)
            .build()
        filter.assertKanUtbetales()
    }

    @Test
    fun `flere arbeidsgivere med brukerutbetaling på utbetalingen kan ikke utbetaless`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(false)
            .utbetaling(lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0))))
            .inntektkilde(FLERE_ARBEIDSGIVERE)
            .build()
        filter.assertKanIkkeUtbetales("Utbetaling inneholder brukerutbetaling (men ikke for den aktuelle vedtaksperioden)")
    }

    @Test
    fun `flere arbeidsgivere uten brukerutbetaling på en utbetalingslinje kan ikke utbetales`() {
        val filter = Utbetalingsfilter.Builder()
            .utbetalingstidslinjerHarBrukerutbetaling(true)
            .utbetaling(lagUtbetaling())
            .inntektkilde(FLERE_ARBEIDSGIVERE)
            .build()
        filter.assertKanIkkeUtbetales("Utbetalingstidslinje inneholder brukerutbetaling")
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
    private fun Utbetalingsfilter.assertKanUtbetales() = assertFalse(kanIkkeUtbetales(aktivitetslogg))
    private fun Utbetalingsfilter.assertKanIkkeUtbetales(forventetError: String) {
        assertTrue(kanIkkeUtbetales(aktivitetslogg))
        assertEquals(listOf(forventetError), aktivitetslogg.hentErrors())
    }
}
