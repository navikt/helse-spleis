package no.nav.helse.person

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelNySøknadTest
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NySøknadHendelseTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
        aktivitetslogger = Aktivitetslogger()
    }

    @Test
    internal fun `NySøknad skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), aktivitetslogger)
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
    }

    @Test
    internal fun `En ny NySøknad er ugyldig`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), aktivitetslogger)
        assertThrows<Aktivitetslogger> { person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), aktivitetslogger) }
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `To forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"), aktivitetslogger)
        assertThrows<Aktivitetslogger> {
            person.håndter(nySøknad(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer2"), aktivitetslogger)
        }
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), aktivitetslogger)
        person.håndter(nySøknad(Triple(6.januar, 10.januar, 100)), aktivitetslogger)
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(1))
    }

    @Test
    internal fun `To søknader uten overlapp hvor den ene ikke er 100%`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), aktivitetslogger)
        assertThrows<Aktivitetslogger> { person.håndter(nySøknad(Triple(6.januar, 10.januar, 50)), aktivitetslogger) }

        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        ModelNySøknad(
            UUID.randomUUID(),
            ModelNySøknadTest.UNG_PERSON_FNR_2018,
            "12345",
            orgnummer,
            LocalDateTime.now(),
            listOf(*sykeperioder),
            aktivitetslogger,
            "{}"
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            sykdomstidslinjer[vedtaksperiodeindeks] = compositeSykdomstidslinje
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks]
    }
}
