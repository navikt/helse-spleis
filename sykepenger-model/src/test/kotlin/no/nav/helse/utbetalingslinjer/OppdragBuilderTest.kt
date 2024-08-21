package no.nav.helse.utbetalingslinjer

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class OppdragBuilderTest {

    @Test
    fun `kan starte oppdrag på helg`() {
        val builder = OppdragBuilder(mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 22.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar, result.single().inspektør.fom)
        assertEquals(22.januar, result.single().inspektør.tom)
    }

    @Test
    fun `kan slutte linje på helg`() {
        val builder = OppdragBuilder(mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 22.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 23.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 24.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 25.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 26.januar, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        builder.ikkeBetalingsdag()
        builder.betalingsdag(femhundreKronerIRefusjon, 30.januar, 100)
        val result = builder.build()

        assertEquals(2, result.size)
        assertEquals(20.januar til 28.januar, result[0].inspektør.fom til result[0].inspektør.tom)
        assertEquals(30.januar.somPeriode(), result[1].inspektør.tom til result[1].inspektør.tom)
    }

    @Test
    fun `kan slutte oppdrag på helg`() {
        val builder = OppdragBuilder(mottaker = "ornr", fagområde = Fagområde.SykepengerRefusjon)
        builder.betalingshelgedag(20.januar, 100)
        builder.betalingshelgedag(21.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 22.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 23.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 24.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 25.januar, 100)
        builder.betalingsdag(femhundreKronerIRefusjon, 26.januar, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(20.januar til 26.januar, result[0].inspektør.fom til result[0].inspektør.tom)
    }

    private val femhundreKronerIRefusjon: Økonomi = Økonomi.gjenopprett(ØkonomiInnDto(
        grad = ProsentdelDto(100.0),
        totalGrad = ProsentdelDto(100.0),
        arbeidsgiverRefusjonsbeløp = InntektbeløpDto.DagligDouble(500.0),
        aktuellDagsinntekt = InntektbeløpDto.DagligDouble(500.0),
        beregningsgrunnlag = InntektbeløpDto.DagligDouble(500.0),
        dekningsgrunnlag = InntektbeløpDto.DagligDouble(500.0),
        grunnbeløpgrense = null,
        arbeidsgiverbeløp = InntektbeløpDto.DagligDouble(500.0),
        personbeløp = InntektbeløpDto.DagligDouble(0.0),
        er6GBegrenset = false
    ), erAvvistDag = false)
}