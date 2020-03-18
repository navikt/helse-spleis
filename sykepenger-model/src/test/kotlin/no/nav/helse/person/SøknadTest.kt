package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.hendelser.Søknad.Periode.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SøknadTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `søknad matcher sykmelding`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.tilstand(0))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 50)))
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `mangler Sykmelding`() {
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `søknad kan ikke utvide sykdomstidslinje frem i tid`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100), Egenmelding(9.januar, 10.januar)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
    }

    @Test
    internal fun `søknad kan utvide sykdomstidslinje tilbake i tid`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Egenmelding(28.desember(2017), 29.desember(2017)), Sykdom(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
        assertEquals(9, inspektør.sykdomstidslinje(0).length()) { inspektør.sykdomstidslinje(0).toString() }
    }

    @Test
    internal fun `søknad med utdanning avvist`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100), Utdanning(4.januar, 5.januar)))
        assertTrue(inspektør.personLogg.hasBehov())
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `andre søknad ugyldig`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(6.januar, 10.januar, 100)))
        person.håndter(søknad(Sykdom(6.januar, 10.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
        assertEquals(5, inspektør.sykdomstidslinje(0).length())
        assertEquals(TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.tilstand(1))
        assertEquals(5, inspektør.sykdomstidslinje(1).length())
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        person.håndter(søknad(Sykdom(1.januar, 5.januar, 100)))
        person.håndter(sykmelding(Triple(4.januar, 10.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `to forskjellige arbeidsgivere er ikke støttet`() {
        person.håndter(sykmelding(Triple(1.januar, 5.januar, 100), orgnummer = "orgnummer1"))
        person.håndter(
                søknad(Sykdom(1.januar, 5.januar, 100), orgnummer = "orgnummer2")
            )
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun søknad(vararg perioder: Periode, orgnummer: String = "987654321") =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = perioder.last().tom.atStartOfDay()
        )

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder)
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, ConcreteSykdomstidslinje>()
        internal lateinit var personLogg: Aktivitetslogg

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personLogg = aktivitetslogg
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            sykdomstidslinjer[vedtaksperiodeindeks] = sykdomshistorikk.sykdomstidslinje()
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks]

        internal fun sykdomstidslinje(indeks: Int) = sykdomstidslinjer[indeks] ?:
            throw IllegalAccessException()
    }
}
