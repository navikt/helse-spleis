package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Inntekthistorikk.Inntekt.Kilde.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class InntekthistorikkTest {
    private val tidligereInntekt = 1500.toBigDecimal()
    private val nyInntekt = 2000.toBigDecimal()

    @Test
    fun `Dupliser inntekt til fordel for nyere oppføring`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt, INFOTRYGD)
        assertEquals(1, historikk.size)
        assertEquals(nyInntekt, historikk.inntekt(3.januar))
        assertEquals(nyInntekt, historikk.inntekt(5.januar))
        assertEquals(nyInntekt, historikk.inntekt(1.januar)) // Using rule that first salary is used
    }

    @Test
    fun `Inntekt fra infotrygd blir ikke overstyrt av inntekt fra skatt`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt, SKATT)
        assertEquals(2, historikk.size)
        assertEquals(tidligereInntekt, historikk.inntekt(3.januar)) // Salary from infotrygd
    }

    private data class inntekt(
        val beløp: Double,
        val kilde: Inntekthistorikk.Inntekt.Kilde,
        val fom: LocalDate = 1.januar,
        val uuid: UUID = UUID.randomUUID()
    )

    private data class args(
        val label: String,
        val a: inntekt,
        val b: inntekt,
        val forventetBeløp: Double
    )

    @TestFactory
    fun `Prioritet mellom to inntektskilder for tredje januar`() = listOf(
        args(
            "Prioriter inntektsmelding over skatt1",
            inntekt(1000.0, INNTEKTSMELDING),
            inntekt(2000.0, SKATT),
            1000.0
        ),
        args(
            "Prioriter inntektsmelding over skatt2",
            inntekt(1000.0, SKATT),
            inntekt(2000.0, INNTEKTSMELDING),
            2000.0
        ),
        args(
            "Prioriter inntektsmelding over infotrygd1",
            inntekt(1000.0, INNTEKTSMELDING),
            inntekt(2000.0, INFOTRYGD),
            1000.0
        ),
        args(
            "Prioriter inntektsmelding over infotrygd2",
            inntekt(1000.0, INFOTRYGD),
            inntekt(2000.0, INNTEKTSMELDING),
            2000.0
        ),
        args(
            "Prioriter infotrygd over skatt1",
            inntekt(1000.0, SKATT),
            inntekt(2000.0, INFOTRYGD),
            2000.0
        ),
        args(
            "Prioriter infotrygd over skatt2",
            inntekt(1000.0, INFOTRYGD),
            inntekt(2000.0, SKATT),
            1000.0
        ),
        args(
            "??assumption?? inntekt gjelder bare fom dato",
            inntekt(1000.0, SKATT, 1.januar),
            inntekt(2000.0, INNTEKTSMELDING, 4.januar),
            1000.0
        ),
        args(
            "??assumption?? inntekt inkluderer fom dato",
            inntekt(1000.0, SKATT, 1.januar),
            inntekt(2000.0, INNTEKTSMELDING, 3.januar),
            2000.0
        ),
        args(
            "??assumption?? Når dato er før første fom dato, prioriter første1",
            inntekt(1000.0, SKATT, 5.januar),
            inntekt(2000.0, INNTEKTSMELDING, 6.januar),
            1000.0
        ),
        args(
            "??assumption?? Når dato er før første fom dato, prioriter første2",
            inntekt(2000.0, INNTEKTSMELDING, 6.januar),
            inntekt(1000.0, SKATT, 5.januar),
            1000.0
        ),
        args(
            "Prioriter rekkefølge hvis samme dato og kilde",
            inntekt(1000.0, SKATT),
            inntekt(2000.0, SKATT),
            2000.0
        )
    )
        .map {
            DynamicTest.dynamicTest(it.label) {
                val historikk = Inntekthistorikk()
                historikk.add(it.a.fom, it.a.uuid, BigDecimal(it.a.beløp), it.a.kilde)
                historikk.add(it.b.fom, it.b.uuid, BigDecimal(it.b.beløp), it.b.kilde)
                assertEquals(it.forventetBeløp, historikk.inntekt(3.januar)?.toDouble()) // Salary from infotrygd
            }
        }


    @Test
    fun `gir eldste inntekt når vi ikke har nyere`() {
        val historikk = Inntekthistorikk()
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        assertEquals(1, historikk.size)
        assertEquals(tidligereInntekt, historikk.inntekt(1.januar))
    }

    @Test
    fun `tom inntekthistorikk`() {
        val historikk = Inntekthistorikk()
        assertNull(historikk.inntekt(1.januar))
    }

    @Test
    fun likheter() {
        val inntektA = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        val inntektB = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        val inntektC = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), nyInntekt, INFOTRYGD)
        val inntektD = Inntekthistorikk.Inntekt(2.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        assertEquals(inntektA, inntektB)
        assertNotEquals(inntektA, inntektC)
        assertNotEquals(inntektA, inntektD)
        assertNotEquals(inntektA, null)
    }

    @Test
    fun sammenligning() {
        val inntektA = Inntekthistorikk.Inntekt(1.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        val inntektB = Inntekthistorikk.Inntekt(2.januar, UUID.randomUUID(), nyInntekt, INFOTRYGD)
        assertTrue(inntektA < inntektB)
    }

    @Test
    fun `Sykepengegrunnlag er begrenset til 6G når inntekt er høyere enn 6G`() {
        val førsteFraværsdag = 1.januar(2020)
        val `6GBeløp` = Grunnbeløp.`6G`.beløp(førsteFraværsdag)

        val årsinntektOver6G =
            listOf(Inntekthistorikk.Inntekt(førsteFraværsdag, UUID.randomUUID(), 49929.01.toBigDecimal(), INFOTRYGD))
        assertEquals(`6GBeløp`, Inntekthistorikk.Inntekt.sykepengegrunnlag(årsinntektOver6G, førsteFraværsdag))

        val årsinntektUnder6G =
            listOf(Inntekthistorikk.Inntekt(førsteFraværsdag, UUID.randomUUID(), 49928.toBigDecimal(), INFOTRYGD))
        assertTrue(Inntekthistorikk.Inntekt.sykepengegrunnlag(årsinntektUnder6G, førsteFraværsdag)!! < `6GBeløp`)
    }

    private val Inntekthistorikk.size: Int get() = Inntektsinspektør(this).inntektTeller

    private class Inntektsinspektør(historikk: Inntekthistorikk) : InntekthistorikkVisitor {
        internal var inntektTeller = 0

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
            inntektTeller += 1
        }

    }
}

