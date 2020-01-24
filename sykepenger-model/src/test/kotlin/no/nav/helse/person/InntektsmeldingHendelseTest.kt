package no.nav.helse.person

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set

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
    internal fun `kan behandle inntektsmelding om vi mottar den etter mottatt søknad`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_INNTEKTSMELDING, inspektør.tilstand(0))
    }

    @Test
    internal fun `førsteFraværsdag settes i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.førsteFraværsdag(0))
    }

    @Test
    internal fun `kan behandle inntektsmelding om vi mottar den etter mottatt ny søknad og sendt søknad`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(sendtSøknad(ModelSendtSøknad.Periode.Sykdom(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertFalse(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.VILKÅRSPRØVING, inspektør.tilstand(0))
    }

    @Test
    internal fun `vedtaksperioden må behandles i infotrygd om vi mottar en inntektsmelding uten tilhørende søknad`() {
        person.håndter(inntektsmelding())
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `vedtaksperiode må behandles i infotrygd om vi får inn en inntektsmelding nummer to`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `inntektsmelding med tilhørende søknad men med forskjellige arbeidsgivere støttes ikke`() {
        person.håndter(nySøknad(Triple(6.januar,20.januar, 100), orgnr = "123"))
        person.håndter(inntektsmelding(virksomhetsnummer = "456"))
        assertTrue(aktivitetslogger.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    private fun inntektsmelding(
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList(),
        virksomhetsnummer: String = ORGNR
    ) =
        ModelInntektsmelding(
            UUID.randomUUID(),
            ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            virksomhetsnummer,
            UNG_PERSON_FNR_2018,
            AKTØRID,
            1.februar.atStartOfDay(),
            førsteFraværsdag,
            beregnetInntekt,
            aktivitetslogger,
            "{}",
            listOf(1.januar..16.januar),
            emptyList()
        )

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnr: String = ORGNR) = ModelNySøknad(
        UUID.randomUUID(),
        UNG_PERSON_FNR_2018,
        AKTØRID,
        orgnr,
        LocalDateTime.now(),
        listOf(*sykeperioder),
        aktivitetslogger,
        "{}"
    )

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode, orgnummer: String = ORGNR) =
        ModelSendtSøknad(
            UUID.randomUUID(),
            SendtSøknadHendelseTest.UNG_PERSON_FNR_2018,
            "12345",
            orgnummer,
            LocalDateTime.now(),
            listOf(*perioder),
            aktivitetslogger,
            "{}"
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
        private val førsteFraværsdager = mutableMapOf<Int, LocalDate?>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
            førsteFraværsdager[vedtaksperiodeindeks] = vedtaksperiode.førsteFraværsdag()
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            sykdomstidslinjer[vedtaksperiodeindeks] = compositeSykdomstidslinje
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks]
        internal fun førsteFraværsdag(indeks: Int) = førsteFraværsdager[indeks]
    }

}
