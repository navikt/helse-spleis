package no.nav.helse.person

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadHendelseTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var problemer: Problemer

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
        problemer = Problemer()
    }

    @Test
    internal fun `sendtsøknad matcher nysøknad`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        assertFalse(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)), problemer)
        assertFalse(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SENDT_SØKNAD, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        assertThrows<Problemer> {
            person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 50)), problemer) }
        assertTrue(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `mangler NySøknad`() {
        assertThrows<Problemer> { person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)), problemer) }
        assertTrue(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `sendtSøknad kan utvide sykdomstidslinje`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100), Egenmelding(9.januar, 10.januar)), problemer)
        assertFalse(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SENDT_SØKNAD, inspektør.tilstand(0))
        assertEquals(10, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `sendtSøknad med utdanning avvist`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        assertThrows<Problemer> { person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100), Utdanning(4.januar, 5.januar)), problemer) }
        assertTrue(problemer.hasErrors())
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `andre sendSøknad ugyldig`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)), problemer)
        assertFalse(problemer.hasErrors())
        assertThrows<Problemer> { person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)), problemer) }
        assertTrue(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)), problemer)
        person.håndter(nySøknad(Triple(6.januar, 10.januar, 100)), problemer)
        person.håndter(sendtSøknad(Sykdom(6.januar, 10.januar, 100)), problemer)
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)), problemer)
        assertFalse(problemer.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SENDT_SØKNAD, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
        assertEquals(TilstandType.MOTTATT_SENDT_SØKNAD, inspektør.tilstand(1))
        assertEquals(5, inspektør.sykdomstidslinje(1).length())
    }

    @Test
    internal fun `to forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"), problemer)
        assertThrows<Problemer> {
            person.håndter(
                sendtSøknad(Sykdom(1.januar, 5.januar, 100), orgnummer = "orgnummer2"),
                problemer)
        }
        assertTrue(problemer.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun sendtSøknad(vararg perioder: Periode, orgnummer: String = "987654321") =
        ModelSendtSøknad(
            UUID.randomUUID(),
            UNG_PERSON_FNR_2018,
            "12345",
            orgnummer,
            LocalDateTime.now(),
            listOf(*perioder),
            problemer
        )

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        ModelNySøknad(
            UUID.randomUUID(),
            UNG_PERSON_FNR_2018,
            "12345",
            orgnummer,
            LocalDateTime.now(),
            listOf(*sykeperioder),
            problemer,
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

        internal fun sykdomstidslinje(indeks: Int) = sykdomstidslinjer[indeks] ?:
            throw IllegalAccessException()
    }
}
