package no.nav.helse.serde.reflection

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektReflectTest {
    @Test
    internal fun `kontroller at alle felter er gjort rede for`() {
        assertMembers<Inntekthistorikk.Inntekt, InntektReflect>(
            skalMappes = listOf("fom", "hendelse", "beløp")
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    internal fun `mapper Inntekt til map`() {
        val map = InntektReflect(inntekt).toMap()

        assertEquals(3, map.size)
        assertEquals(1.januar, map["fom"])
        assertEquals(InntektsmeldingReflectTest.inntektsmelding.hendelseId(), map["hendelse"])
        assertEquals(1000.0.toBigDecimal(), map["beløp"])
    }

    internal val inntekt =
        Inntekthistorikk.Inntekt(1.januar, InntektsmeldingReflectTest.inntektsmelding, 1000.0.toBigDecimal())
}
