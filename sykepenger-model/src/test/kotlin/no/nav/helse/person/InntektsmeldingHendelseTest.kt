package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.collections.set

internal class InntektsmeldingHendelseTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "12345"
        private const val ORGNR = "987654321"
        private const val INNTEKT = 1234.0
    }

    private lateinit var person: Person

    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `legger inn beregnet inntekt i inntekthistorikk`() {
        val inntekthistorikk = Inntekthistorikk()
        inntektsmelding(beregnetInntekt = INNTEKT, førsteFraværsdag = 1.januar)
            .addInntekt(inntekthistorikk)
        assertEquals(INNTEKT.toBigDecimal(), inntekthistorikk.inntekt(1.januar))
    }

    @Test
    internal fun `førsteFraværsdag settes i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding(førsteFraværsdag = 1.januar))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.førsteFraværsdag(0))
    }

    @Test
    internal fun `inntektsmelding før søknad`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `inntektsmelding etter søknad`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(søknad(Søknad.Periode.Sykdom(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertFalse(inspektør.personlogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `søknad etter inntektsmelding`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(søknad(Søknad.Periode.Sykdom(6.januar, 20.januar, 100)))
        assertFalse(inspektør.personlogg.hasErrors(), inspektør.personlogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(sykmelding(Triple(19.januar, 30.januar, 100)))
        assertTrue(inspektør.personlogg.hasWarnings())
        assertFalse(inspektør.personlogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.tilstand(0))
    }


    @Test
    internal fun `mangler sykmelding`() {
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personlogg.hasWarnings())
        assertFalse(inspektør.personlogg.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `flere inntektsmeldinger`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personlogg.hasWarnings())
        assertFalse(inspektør.personlogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.tilstand(0))
    }

    @Test
    internal fun `annen arbeidsgiver`() {
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100), orgnr = "123"))
        person.håndter(inntektsmelding(virksomhetsnummer = "456"))
        assertTrue(inspektør.personlogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.tilstand(0))
    }

    @Test
    internal fun `overlapp med ferie og arbeidsgiverperiode`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(1.januar, INNTEKT, emptyList()),
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf(Periode(16.januar, 31.januar))
        )
        assertTrue(inntektsmelding.valider().hasErrors())
        person.håndter(sykmelding(Triple(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding)
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
            ferieperioder = emptyList()
        )

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>, orgnr: String = ORGNR) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        orgnummer = orgnr,
        sykeperioder = listOf(*sykeperioder)
    )

    private fun søknad(vararg perioder: Søknad.Periode, orgnummer: String = ORGNR) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = perioder.last().tom.atStartOfDay()
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
        private val førsteFraværsdager = mutableMapOf<Int, LocalDate?>()
        internal lateinit var personlogg: Aktivitetslogg

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personlogg = aktivitetslogg
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
