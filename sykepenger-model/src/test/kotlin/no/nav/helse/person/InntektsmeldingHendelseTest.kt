package no.nav.helse.person

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingHendelseTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val AKTØRID = "12345"
        internal const val ORGNR = "987654321"
    }

    private lateinit var person: Person
    private lateinit var aktivitetslogger: Aktivitetslogger

    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        aktivitetslogger = Aktivitetslogger()
    }

    @Test
    internal fun `inntektsmelding uten en eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        person.håndter(nyInntektsmelding(), aktivitetslogger)
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun nyInntektsmelding(
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) =
        ModelInntektsmelding(
            UUID.randomUUID(),
            ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            ORGNR,
            UNG_PERSON_FNR_2018,
            AKTØRID,
            1.februar.atStartOfDay(),
            førsteFraværsdag,
            beregnetInntekt,
            aktivitetslogger,
            listOf(1.januar .. 16.januar),
            emptyList()
        )

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>)
        = ModelNySøknad(
            UUID.randomUUID(),
            UNG_PERSON_FNR_2018,
            AKTØRID,
            ORGNR,
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
