package no.nav.helse.person

import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.hendelser.SendtSøknad.Periode
import no.nav.helse.hendelser.SendtSøknad.Periode.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
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
    internal fun `sendtsøknad matcher nysøknad`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.UNDERSØKER_HISTORIKK, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 50)))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `mangler NySøknad`() {
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `sendtSøknad kan utvide sykdomstidslinje`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100), Egenmelding(9.januar, 10.januar)))
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.UNDERSØKER_HISTORIKK, inspektør.tilstand(0))
        assertEquals(10, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `sendtSøknad med utdanning avvist`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100), Utdanning(4.januar, 5.januar)))
        assertTrue(aktivitetslogger.hasNeeds())
        assertFalse(aktivitetslogger.hasErrors(), aktivitetslogger.toString())
        println(aktivitetslogger)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `andre sendSøknad ugyldig`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(aktivitetslogger.hasErrors())
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `flere sendte søknader`() {
        person.håndter(nySøknad(Triple(6.januar, 10.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(6.januar, 10.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(6.januar, 10.januar, 100)))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }


    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(nySøknad(Triple(6.januar, 10.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(6.januar, 10.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.UNDERSØKER_HISTORIKK, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
        assertEquals(TilstandType.AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING, inspektør.tilstand(1))
        assertEquals(5, inspektør.sykdomstidslinje(1).length())
    }

    @Test
    internal fun `Ny søknad med overlapp på en periode`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(sendtSøknad(Sykdom(1.januar, 5.januar, 100)))
        person.håndter(nySøknad(Triple(4.januar, 10.januar, 100)))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `to forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"))
        person.håndter(
                sendtSøknad(Sykdom(1.januar, 5.januar, 100), orgnummer = "orgnummer2")
            )
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun sendtSøknad(vararg perioder: Periode, orgnummer: String = "987654321") =
        SendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sendtNav = LocalDateTime.now(),
            perioder = listOf(*perioder),
            aktivitetslogger = aktivitetslogger,
            harAndreInntektskilder = false
        )

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        NySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(*sykeperioder),
            aktivitetslogger = aktivitetslogger
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
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
