package no.nav.helse.person

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class InntekthistorikkTest {
    private val tidligereInntekt = 1500.toBigDecimal()
    private val nyInntekt = 2000.toBigDecimal()

    @Test internal fun `Dupliser inntekt til fordel for nyere oppføring`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt)
        assertEquals(1, historikk.size)
        assertEquals(nyInntekt, historikk.inntekt(3.januar))
        assertEquals(nyInntekt, historikk.inntekt(5.januar))
        assertEquals(nyInntekt, historikk.inntekt(1.januar)) // Using rule that first salary is used
    }

    @Test internal fun `Null kom tilbake for tom inntektshistorie`() {
        val historikk = Inntekthistorikk()
        assertNull(historikk.inntekt(1.januar))
    }

    private val Inntekthistorikk.size: Int get() = Inntektsinspektør(this).inntektTeller

    private class Inntektsinspektør(historikk: Inntekthistorikk): InntekthistorikkVisitor {
        internal var inntektTeller = 0
        init { historikk.accept(this) }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
            inntektTeller += 1
        }

    }
}
