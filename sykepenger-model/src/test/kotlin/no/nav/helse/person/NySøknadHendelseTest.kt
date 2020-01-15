package no.nav.helse.person

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelNySøknadTest
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NySøknadHendelseTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "05015112345"
    }

    private lateinit var person: Person
    private lateinit var observatør: TestPersonObserver
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
        observatør = TestPersonObserver().also {
            person.addObserver(it)
        }
    }

    @Test internal fun `NySøknad skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertTrue(observatør.utløst)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, observatør.gjeldendeTilstand)
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
    }

    @Test internal fun `En ny NySøknad er ugyldig`() {
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        person.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertTrue(observatør.utløst)
        assertEquals(TilstandType.TIL_INFOTRYGD, observatør.gjeldendeTilstand)
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnummer: String = "987654321") = ModelNySøknad(
        UUID.randomUUID(),
        ModelNySøknadTest.UNG_PERSON_FNR_2018,
        "12345",
        orgnummer,
        LocalDateTime.now(),
        listOf(*sykeperioder)
    )

    private class TestPersonObserver: PersonObserver {
        internal var utløst = false
        internal lateinit var gjeldendeTilstand: TilstandType
        internal lateinit var forrigeTilstand: TilstandType
        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            utløst = true
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            gjeldendeTilstand = event.gjeldendeTilstand
            forrigeTilstand = event.forrigeTilstand
        }
    }

    private inner class TestPersonInspektør(person: Person): PersonVisitor {
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
