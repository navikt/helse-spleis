package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeBuilderVedtaksperiodeTest {

    @Test
    fun `setter inntekt på økonomi`() {
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode()
        val økonomi: Økonomi = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, Økonomi.ikkeBetalt())
        assertEquals(21000.månedlig, økonomi.inspektør.aktuellDagsinntekt)
    }

    @Test
    fun `setter inntekt på økonomi om vurdering ikke er ok`() {
        val inntekt = 21000.månedlig
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode()
        val økonomi: Økonomi = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, Økonomi.ikkeBetalt())
        assertEquals(inntekt, økonomi.inspektør.aktuellDagsinntekt)
    }

    @Test
    fun `henter fastsatt inntekt selv om det er oppgitt inntektsendringer på skjæringstidspunktet`() {
        val inntekt = 21000.månedlig
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode(inntektsendringer = Avsender.SYKMELDT.beløpstidslinje(1.januar til 2.januar, inntekt * 2))
        val inntekt1 = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, Økonomi.ikkeBetalt()).inspektør.aktuellDagsinntekt
        val inntekt2 = utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(2.januar, Økonomi.ikkeBetalt()).inspektør.aktuellDagsinntekt
        assertEquals(inntekt, inntekt1)
        assertEquals(inntekt * 2, inntekt2)
    }

    @Test
    fun `feiler dersom orgnr ikke finnes i inntektsgrunnlaget eller det forekommer inntektsendringer`() {
        val utbetalingstidslinjeBuilderVedtaksperiode = utbetalingstidslinjeBuilderVedtaksperiode(fastsattÅrsinntekt = null)

        assertEquals(Inntekt.INGEN, utbetalingstidslinjeBuilderVedtaksperiode.medInntektHvisFinnes(1.januar, Økonomi.ikkeBetalt()).inspektør.aktuellDagsinntekt)
    }

    private fun utbetalingstidslinjeBuilderVedtaksperiode(
        inntekt: Inntekt = 21000.månedlig,
        skjæringstidspunkt: LocalDate = 1.januar,
        inntektsendringer: Beløpstidslinje = Beløpstidslinje(),
        fastsattÅrsinntekt: Inntekt? = inntekt
    ) = UtbetalingstidslinjeBuilderVedtaksperiode(
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        `6G` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt),
        skjæringstidspunkt = skjæringstidspunkt,
        regler = NormalArbeidstaker,
        arbeidsgiverperiode = listOf(1.januar til 16.januar),
        dagerNavOvertarAnsvar = emptyList(),
        refusjonstidslinje = Beløpstidslinje(),
        inntektsendringer = inntektsendringer
    )
}
