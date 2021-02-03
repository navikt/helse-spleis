package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Inntektshistorikk.Inntektsendring
import no.nav.helse.person.Inntektshistorikk.Inntektsendring.Kilde
import no.nav.helse.person.Inntektshistorikk.Inntektsendring.Kilde.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektshistorikkTest {
    private val tidligereInntekt = 1500.daglig
    private val nyInntekt = 2000.daglig
    private lateinit var historikk: Inntektshistorikk
    private var inntektsbeløp = 0

    @BeforeEach
    fun setup() {
        historikk = Inntektshistorikk()
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
        val skjæringstidspunkt = 1.januar(2020)
        val `6GBeløp` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)

        val årsinntektOver6G =
            listOf(Inntektsendring(skjæringstidspunkt, UUID.randomUUID(), 49929.01.månedlig, INFOTRYGD))
        assertEquals(
            `6GBeløp`,
            Inntektshistorikk.Inntektsendring.sykepengegrunnlag(årsinntektOver6G, skjæringstidspunkt)
        )

        val årsinntektUnder6G =
            listOf(Inntektsendring(skjæringstidspunkt, UUID.randomUUID(), 49928.månedlig, INFOTRYGD))
        assertTrue(
            Inntektshistorikk.Inntektsendring.sykepengegrunnlag(
                årsinntektUnder6G,
                skjæringstidspunkt
            )!! < `6GBeløp`
        )
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

    @Test
    fun `Inntekt competition winners`() {
        listOf(
            "a" er 1.januar.SKATT,
            "b" er 5.januar.INNTEKTSMELDING,
            "c" er 5.januar.INFOTRYGD,
            "d" er 10.januar.INFOTRYGD,
            "e" er 10.januar.INNTEKTSMELDING
        ).assertions { challengers ->
            assertChampion("a" beats challengers, 1.desember(2017) to 4.januar)
            assertChampion("b" beats challengers, 5.januar to 9.januar)
            assertChampion("e" beats challengers, 10.januar to 28.februar)
        }
    }

    private fun assertChampion(winner: Pair<String, List<Pair<String, InntektArgs>>>, periode: Periode) {
        periode.forEach { dato ->
            val indeks = winner.second.foldIndexed(-1) { index, acc, _ ->
                if (historikk.inntekt(dato)?.equals(((index + 1) * 1000).daglig) == true) index else acc
            }
            assertEquals(winner.first, winner.second[indeks].first, "for date: $dato")
        }
    }

    private infix fun String.beats(challengers: List<Pair<String, InntektArgs>>) = this to challengers

    private infix fun String.er(args: InntektArgs) = this to args

    private infix fun LocalDate.to(other: LocalDate) = Periode(this, other)

    private fun List<Pair<String, InntektArgs>>.assertions(block: (List<Pair<String, InntektArgs>>) -> Unit) {
        historikk = Inntektshistorikk()
        inntektsbeløp = 0
        this.forEach { it.second.also { args -> args.add(); args.merkelapp(it.first) } }
        block(this)
    }

    private infix fun Kilde.versus(other: Kilde): Kilde {
        historikk = Inntektshistorikk()
        historikk.add(1.januar, UUID.randomUUID(), 1000.daglig, this)
        historikk.add(1.januar, UUID.randomUUID(), 2000.daglig, other)
        return if (1000.daglig == historikk.inntekt(3.januar)) this else other
    }

    private inner class InntektCompetition(private val a: InntektArgs, private val b: InntektArgs) {
        infix fun on(dato: LocalDate): Boolean {
            fun assertInntekt(left: InntektArgs, right: InntektArgs, expected: Number): Boolean {
                historikk = Inntektshistorikk()
                inntektsbeløp = 0
                left.add()
                right.add()
                return expected.daglig == historikk.inntekt(dato)
            }
            return assertInntekt(a, b, 1000) && assertInntekt(b, a, 2000)
        }
    }


    private val LocalDate.INFOTRYGD get() = inntekt(this, Kilde.INFOTRYGD)

    private val LocalDate.SKATT get() = inntekt(this, Kilde.SKATT)

    private val LocalDate.INNTEKTSMELDING get() = inntekt(this, Kilde.INNTEKTSMELDING)

    private fun inntekt(dato: LocalDate, kilde: Kilde) = InntektArgs(dato, kilde)

    private open inner class InntektArgs(protected val dato: LocalDate, private val kilde: Kilde) {
        internal lateinit var merkelapp: String

        internal fun merkelapp(verdi: String) {
            merkelapp = verdi
        }

        internal open fun add() {
            inntektsbeløp += 1000
            historikk.add(dato, UUID.randomUUID(), inntektsbeløp.daglig, kilde)
        }

        internal infix fun beats(other: InntektArgs) = InntektCompetition(this, other)
    }

    private inner class AvsluttetArgs(dato: LocalDate, kilde: Kilde) : InntektArgs(dato, kilde) {
        override fun add() {
            inntektsbeløp += 1000
            historikk.add(dato, UUID.randomUUID(), 0.daglig, SKATT)
        }
    }

    private val Inntektshistorikk.size: Int get() = Inntektsinspektør(this).inntektTeller

    private class Inntektsinspektør(historikk: Inntektshistorikk) : InntekthistorikkVisitor {
        internal var inntektTeller = 0

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntektsendring: Inntektsendring, hendelseId: UUID) {
            inntektTeller += 1
        }

    }
}

