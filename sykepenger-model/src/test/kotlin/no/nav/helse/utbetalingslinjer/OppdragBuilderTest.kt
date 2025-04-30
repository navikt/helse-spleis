package no.nav.helse.utbetalingslinjer

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdragBuilderTest {

    private fun refusjonBuilder() = OppdragBuilder(
        mottaker = "orgnr",
        fagområde = Fagområde.SykepengerRefusjon,
        klassekode = Klassekode.RefusjonIkkeOpplysningspliktig
    )
    private fun personBuilder() = OppdragBuilder(
        mottaker = "fnr",
        fagområde = Fagområde.Sykepenger,
        klassekode = Klassekode.SykepengerArbeidstakerOrdinær
    )

    @Test
    fun `kan starte oppdrag på helg`() {
        val builder = refusjonBuilder()
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
        val builder = refusjonBuilder()
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
        val builder = refusjonBuilder()
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

    @Test
    fun `siste dager i arbeidsgiverperioden er lørdag og søndag`() {
        val builder = personBuilder()
        builder.arbeidsgiverperiodedag(20.januar, 100)
        builder.arbeidsgiverperiodedag(21.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 22.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 23.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 24.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 25.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 26.januar, 100)
        builder.betalingshelgedag(27.januar, 100)
        builder.betalingshelgedag(28.januar, 100)
        val result = builder.build()

        assertEquals(1, result.size)
        assertEquals(22.januar til 26.januar, result[0].inspektør.fom til result[0].inspektør.tom)
    }

    @Test
    fun `flekkvis utbetalingsdager i arbeidsgiverperioden`() {
        val builder = personBuilder()
        // to SykNav-dager i arbeidsgiverperioden
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 1.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 2.januar, 100)
        // litt vanlig arbeidsgiverperiode
        builder.arbeidsgiverperiodedag(3.januar, 100)
        builder.arbeidsgiverperiodedag(4.januar, 100)
        // litt mer SykNav
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 5.januar, 100)
        // litt mer vanlig arbeidsgiverperiode
        builder.arbeidsgiverperiodedag(6.januar, 100)
        builder.arbeidsgiverperiodedag(7.januar, 100)
        // så over til NavDager f.eks.
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 8.januar, 100)
        builder.betalingsdag(femhundreKronerIBrukerutbetaling, 9.januar, 100)
        val result = builder.build()

        assertEquals(2, result.size)
        assertEquals(1.januar til 2.januar, result[0].inspektør.fom til result[0].inspektør.tom)
        assertEquals(5.januar til 9.januar, result[1].inspektør.fom til result[1].inspektør.tom)
    }

    private val femhundreKronerIRefusjon: Økonomi = Økonomi.gjenopprett(
        ØkonomiInnDto(
            grad = ProsentdelDto(100.0),
            totalGrad = ProsentdelDto(100.0),
            utbetalingsgrad = ProsentdelDto(100.0),
            arbeidsgiverRefusjonsbeløp = InntektbeløpDto.DagligDouble(500.0),
            aktuellDagsinntekt = InntektbeløpDto.DagligDouble(500.0),
            dekningsgrunnlag = InntektbeløpDto.DagligDouble(500.0),
            arbeidsgiverbeløp = InntektbeløpDto.DagligDouble(500.0),
            personbeløp = InntektbeløpDto.DagligDouble(0.0),
        )
    )

    private val femhundreKronerIBrukerutbetaling: Økonomi = Økonomi.gjenopprett(
        ØkonomiInnDto(
            grad = ProsentdelDto(100.0),
            totalGrad = ProsentdelDto(100.0),
            utbetalingsgrad = ProsentdelDto(100.0),
            arbeidsgiverRefusjonsbeløp = InntektbeløpDto.DagligDouble(500.0),
            aktuellDagsinntekt = InntektbeløpDto.DagligDouble(500.0),
            dekningsgrunnlag = InntektbeløpDto.DagligDouble(500.0),
            arbeidsgiverbeløp = InntektbeløpDto.DagligDouble(0.0),
            personbeløp = InntektbeløpDto.DagligDouble(500.0),
        )
    )
}
