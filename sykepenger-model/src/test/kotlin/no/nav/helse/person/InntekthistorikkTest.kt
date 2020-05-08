package no.nav.helse.person

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class InntekthistorikkTest {
    private val tidligereInntekt = 1500.toBigDecimal()
    private val nyInntekt = 2000.toBigDecimal()

    @Test
    fun `Dupliser inntekt til fordel for nyere oppføring`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt)
        assertEquals(1, historikk.size)
        assertEquals(nyInntekt, historikk.inntekt(3.januar))
        assertEquals(nyInntekt, historikk.inntekt(5.januar))
        assertEquals(nyInntekt, historikk.inntekt(1.januar)) // Using rule that first salary is used
    }

    @Test
    fun `gir eldste inntekt når vi ikke har nyere`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt)
        assertEquals(1, historikk.size)
        assertEquals(tidligereInntekt, historikk.inntekt(1.januar))
    }

    @Test
    fun `tom inntekthistorikk`() {
        val historikk = Inntekthistorikk()
        assertNull(historikk.inntekt(1.januar))
    }

    @Test
    fun `likheter`() {
        val inntektA = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt)
        val inntektB = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt)
        val inntektC = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), nyInntekt)
        val inntektD = Inntekthistorikk.Inntekt(2.januar, UUID.randomUUID(), tidligereInntekt)
        assertEquals(inntektA, inntektB)
        assertNotEquals(inntektA, inntektC)
        assertNotEquals(inntektA, inntektD)
        assertNotEquals(inntektA, null)
    }

    @Test
    fun `sammenligning`() {
        val inntektA = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt)
        val inntektB = Inntekthistorikk.Inntekt(2.januar, UUID.randomUUID(), nyInntekt)
        assertTrue(inntektA < inntektB)
    }

    private val Inntekthistorikk.size: Int get() = Inntektsinspektør(this).inntektTeller

    private class Inntektsinspektør(historikk: Inntekthistorikk): InntekthistorikkVisitor {
        internal var inntektTeller = 0
        init { historikk.accept(this) }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
            inntektTeller += 1
        }

    }
}
