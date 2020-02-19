package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.collections.set

internal class InntektsmeldingHendelseTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "12345"
        private const val ORGNR = "987654321"
    }

    private lateinit var person: Person
    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var aktivitetslogg: Aktivitetslogg

    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        aktivitetslogger = Aktivitetslogger()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    internal fun `førsteFraværsdag settes i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding(førsteFraværsdag = 1.januar))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.førsteFraværsdag(0))
    }

    @Test
    internal fun `inntektsmelding før sendt søknad`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SENDT_SØKNAD, inspektør.tilstand(0))
    }

    @Test
    internal fun `inntektsmelding etter sendt søknad`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(sendtSøknad(SendtSøknad.Periode.Sykdom(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertFalse(aktivitetslogger.hasErrorsOld())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, inspektør.tilstand(0))
    }

    @Test
    internal fun `sendt søknad etter inntektsmelding`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(sendtSøknad(SendtSøknad.Periode.Sykdom(6.januar, 20.januar, 100)))
        assertFalse(aktivitetslogger.hasErrorsOld(), aktivitetslogger.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, inspektør.tilstand(0))
    }

    @Test
    internal fun `Ny søknad med overlapp på en periode`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(nySøknad(Triple(19.januar, 30.januar, 100)))
        assertTrue(aktivitetslogger.hasErrorsOld())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }


    @Test
    internal fun `mangler ny søknad`() {
        person.håndter(inntektsmelding())
        assertTrue(aktivitetslogger.hasErrorsOld())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `flere inntektsmeldinger`() {
        person.håndter(nySøknad(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(aktivitetslogger.hasWarningsOld())
        assertFalse(aktivitetslogger.hasErrorsOld())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SENDT_SØKNAD, inspektør.tilstand(0))
    }

    @Test
    internal fun `annen arbeidsgiver`() {
        person.håndter(nySøknad(Triple(6.januar,20.januar, 100), orgnr = "123"))
        person.håndter(inntektsmelding(virksomhetsnummer = "456"))
        assertTrue(aktivitetslogger.hasErrorsOld())
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
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = virksomhetsnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = emptyList(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnr: String = ORGNR) = NySøknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        orgnummer = orgnr,
        sykeperioder = listOf(*sykeperioder),
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg
    )

    private fun sendtSøknad(vararg perioder: SendtSøknad.Periode, orgnummer: String = ORGNR) =
        SendtSøknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg,
            harAndreInntektskilder = false
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
        private val førsteFraværsdager = mutableMapOf<Int, LocalDate?>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
        }

        override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
            førsteFraværsdager[vedtaksperiodeindeks] = førsteFraværsdag
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
