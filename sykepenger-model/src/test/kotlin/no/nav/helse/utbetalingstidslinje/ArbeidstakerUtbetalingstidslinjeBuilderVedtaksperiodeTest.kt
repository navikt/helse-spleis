package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import no.nav.helse.økonomi.inspectors.inspektør

internal class ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiodeTest {

    @Test
    fun `setter inntekt på økonomi`() {
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode()
        val økonomi: Økonomi = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, 100.prosent)
        assertEquals(21000.månedlig, økonomi.inspektør.aktuellDagsinntekt)
    }

    @Test
    fun `setter inntekt på økonomi om vurdering ikke er ok`() {
        val inntekt = 21000.månedlig
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode()
        val økonomi: Økonomi = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, 100.prosent)
        assertEquals(inntekt, økonomi.inspektør.aktuellDagsinntekt)
    }

    @Test
    fun `feiler dersom orgnr ikke finnes i inntektsgrunnlaget eller det forekommer inntektsendringer`() {
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode(fastsattÅrsinntekt = null)

        assertEquals(INGEN, utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, 100.prosent).inspektør.aktuellDagsinntekt)
    }

    private fun utbetalingstidslinjeBuilderVedtaksperiode(
        inntekt: Inntekt = 21000.månedlig,
        fastsattÅrsinntekt: Inntekt? = inntekt
    ) = ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
        arbeidsgiverperiode = listOf(1.januar til 16.januar),
        dagerNavOvertarAnsvar = emptyList(),
        refusjonstidslinje = Beløpstidslinje(),
        fastsattÅrsinntekt = fastsattÅrsinntekt ?: INGEN,
        inntektjusteringer = Beløpstidslinje()
    )
}
