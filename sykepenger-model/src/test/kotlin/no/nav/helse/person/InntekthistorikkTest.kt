package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Inntekthistorikk.Inntektsendring
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntekthistorikkTest {
    private val tidligereInntekt = 1500.0
    private val nyInntekt = 2000.0
    private lateinit var historikk: Inntekthistorikk
    private var inntektsbeløp = 0

    @BeforeEach
    fun setup() {
        historikk = Inntekthistorikk()
        inntektsbeløp = 1000
    }

    @Test
    fun `Dupliser inntekt til fordel for nyere oppføring`() {
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt, INFOTRYGD)
        assertEquals(1, historikk.size)
        assertEquals(nyInntekt, historikk.inntekt(3.januar))
        assertEquals(nyInntekt, historikk.inntekt(5.januar))
        assertEquals(nyInntekt, historikk.inntekt(1.januar)) // Using rule that first salary is used
    }

    @Test
    fun `Inntekt fra infotrygd blir ikke overstyrt av inntekt fra skatt`() {
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        historikk.add(3.januar, UUID.randomUUID(), nyInntekt, SKATT)
        assertEquals(2, historikk.size)
        assertEquals(tidligereInntekt, historikk.inntekt(3.januar)) // Salary from infotrygd
    }

    @Test
    fun `gir eldste inntekt når vi ikke har nyere`() {
        historikk.add(3.januar, UUID.randomUUID(), tidligereInntekt, INFOTRYGD)
        assertEquals(1, historikk.size)
        assertEquals(tidligereInntekt, historikk.inntekt(1.januar))
    }

    @Test
    fun `tom inntekthistorikk`() {
        assertNull(historikk.inntekt(1.januar))
    }



    @Test
    fun `Sykepengegrunnlag er begrenset til 6G når inntekt er høyere enn 6G`() {
        val førsteFraværsdag = 1.januar(2020)
        val `6GBeløp` = Grunnbeløp.`6G`.beløp(førsteFraværsdag)

        val årsinntektOver6G =
            listOf(Inntektsendring(førsteFraværsdag, UUID.randomUUID(), 49929.01.toBigDecimal(), INFOTRYGD))
        assertEquals(`6GBeløp`, Inntekthistorikk.Inntektsendring.sykepengegrunnlag(årsinntektOver6G, førsteFraværsdag))

        val årsinntektUnder6G =
            listOf(Inntektsendring(førsteFraværsdag, UUID.randomUUID(), 49928.toBigDecimal(), INFOTRYGD))
        assertTrue(Inntekthistorikk.Inntektsendring.sykepengegrunnlag(årsinntektUnder6G, førsteFraværsdag)!! < `6GBeløp`)
    }

    @Test
    fun `Prioritert rekkefølge på kilde for lik dato`() {
        assertEquals(INFOTRYGD, INFOTRYGD versus SKATT)
        assertEquals(INNTEKTSMELDING, INFOTRYGD versus INNTEKTSMELDING)
        assertEquals(INNTEKTSMELDING, INNTEKTSMELDING versus INFOTRYGD)
        assertEquals(INFOTRYGD, SKATT versus INFOTRYGD)
        assertEquals(INNTEKTSMELDING, INNTEKTSMELDING versus SKATT)
        assertEquals(INNTEKTSMELDING, SKATT versus INNTEKTSMELDING)
    }

    @Test
    fun `Prioritert rekkefølge på kilde for ulik dato`() {
        assertTrue(3.januar.INNTEKTSMELDING beats 2.januar.INNTEKTSMELDING on 5.januar)
        assertTrue(1.januar.SKATT beats 4.januar.INNTEKTSMELDING on 3.januar)
        assertTrue(3.januar.INNTEKTSMELDING beats 1.januar.SKATT on 3.januar)
        assertTrue(5.januar.INFOTRYGD beats 6.januar.INNTEKTSMELDING on 3.januar)
        assertFalse(3.januar.SKATT beats 3.januar.SKATT on 3.januar) //Entry order decides
    }

    private infix fun Kilde.versus(other: Kilde): Kilde {
        historikk = Inntekthistorikk()
        historikk.add(1.januar, UUID.randomUUID(), 1000, this)
        historikk.add(1.januar, UUID.randomUUID(), 2000, other)
        return if (1000.0 == historikk.inntekt(3.januar)?.toDouble()) this else other
    }

    private inner class InntektCompetition(private val a: InntektArgs, private val b: InntektArgs) {
        infix fun on(dato: LocalDate): Boolean {
            fun assertInntekt(left : InntektArgs, right : InntektArgs, expected : Number) : Boolean {
                historikk = Inntekthistorikk()
                inntektsbeløp = 0
                left.add()
                right.add()
                return expected.toDouble() == historikk.inntekt(dato)?.toDouble()
            }
            return assertInntekt(a,b,1000) && assertInntekt(b,a,2000)
        }
    }


    private val LocalDate.INFOTRYGD get() = inntekt(this, Kilde.INFOTRYGD)

    private val LocalDate.SKATT get() = inntekt(this, Kilde.SKATT)

    private val LocalDate.INNTEKTSMELDING get() = inntekt(this, Kilde.INNTEKTSMELDING)

    private inner class InntektArgs(private val dato: LocalDate, private val kilde: Kilde) {
        fun add() {
            inntektsbeløp += 1000
            historikk.add(dato, UUID.randomUUID(), inntektsbeløp, kilde)
        }

        internal infix fun beats(other: InntektArgs) = InntektCompetition(this, other)
    }

    private fun inntekt(dato: LocalDate, kilde: Kilde) = InntektArgs(
        dato,
        kilde
    )

    private val Inntekthistorikk.size: Int get() = Inntektsinspektør(this).inntektTeller

    private class Inntektsinspektør(historikk: Inntekthistorikk) : InntekthistorikkVisitor {
        internal var inntektTeller = 0

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntektsendring: Inntektsendring, id: UUID) {
            inntektTeller += 1
        }

    }
}

