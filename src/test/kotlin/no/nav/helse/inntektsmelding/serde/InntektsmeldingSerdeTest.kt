package no.nav.helse.inntektsmelding.serde

import no.nav.inntektsmeldingkontrakt.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

internal class InntektsmeldingSerdeTest {

    val inntektsmelding = Inntektsmelding(
        inntektsmeldingId = "1",
        arbeidstakerFnr= "12345678910",
        arbeidstakerAktorId= "1234567891011",
        virksomhetsnummer= "123456789",
        arbeidsgiverFnr= "10987654321",
        arbeidsgiverAktorId= "1110987654321",
        arbeidsgivertype= Arbeidsgivertype.VIRKSOMHET,
        arbeidsforholdId= "42",
        beregnetInntekt= BigDecimal(10000.00),
        refusjon= Refusjon(),
        endringIRefusjoner= emptyList(),
        opphoerAvNaturalytelser= emptyList(),
        gjenopptakelseNaturalytelser= emptyList(),
        arbeidsgiverperioder= listOf(
            Periode(
                fom = LocalDate.of(2019, Month.APRIL, 1),
                tom = LocalDate.of(2019, Month.APRIL, 16)
            )
        ),
        status = Status.GYLDIG,
        arkivreferanse = "ENARKIVREFERANSE"
    )

    @Test
    fun serdeTest() {
        val serde = InntektsmeldingSerde()
        assertEquals(inntektsmelding, serde.deserializer().deserialize("", serde.serializer().serialize("", inntektsmelding)))
    }
}