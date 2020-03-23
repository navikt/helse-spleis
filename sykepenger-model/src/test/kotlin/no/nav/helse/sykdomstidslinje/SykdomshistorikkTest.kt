package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SykdomshistorikkTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private val inspektør: HistorikkInspektør get() = HistorikkInspektør(historikk)

    private lateinit var historikk: Sykdomshistorikk
    @BeforeEach
    internal fun initialiser() {
        historikk = Sykdomshistorikk()
    }

    @Test
    internal fun `Sykmelding mottatt`() {
        historikk.håndter(sykmelding(Triple(1.januar, 5.januar, 100)))
        assertEquals(1, historikk.size)
        assertEquals(5, historikk.sykdomstidslinje().length())
    }

    @Test
    internal fun `Søknad mottatt`() {
        historikk.håndter(sykmelding(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(
            søknad(
                Søknad.Periode.Sykdom(8.januar, 10.januar, 100),
                Søknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
        assertEquals(2, historikk.size)
        assertEquals(11, historikk.sykdomstidslinje().length())
        assertEquals(5, inspektør.hendelseSykdomstidslinje[1].length())
        assertEquals(5, inspektør.beregnetSykdomstidslinjer[1].length())
        assertEquals(9, inspektør.hendelseSykdomstidslinje[0].length())
        assertEquals(11, inspektør.beregnetSykdomstidslinjer[0].length())
    }

    @Test
    internal fun `Gradert sykmelding mottatt`() {
        historikk.håndter(sykmelding(Triple(1.januar, 5.januar, 50)))
        assertEquals(1, historikk.size)
        assertEquals(5, historikk.sykdomstidslinje().length())
        1.januar.datesUntil(6.januar).forEach {
            assertTrue(historikk.sykdomstidslinje().dag(it) is Sykedag)
            assertEquals(50.0, (historikk.sykdomstidslinje().dag(it) as Sykedag).grad)
        }
    }

    @Test
    internal fun `Gradert søknad mottatt`() {
        historikk.håndter(sykmelding(Triple(8.januar, 12.januar, 50)))
        historikk.håndter(
            søknad(
                Søknad.Periode.Sykdom(8.januar, 12.januar, 50, faktiskGrad = 50.0),
                Søknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
        8.januar.datesUntil(13.januar).forEach {
            assertTrue(historikk.sykdomstidslinje().dag(it) is Sykedag)
            assertEquals(50.0, (historikk.sykdomstidslinje().dag(it) as Sykedag).grad)
        }
    }

    @Test
    internal fun `Håndterer Ubestemt dag`() {
        historikk.håndter(sykmelding(Triple(8.januar, 12.januar, 100)))
        søknad(
            Søknad.Periode.Utdanning(9.januar, 12.januar),
            Søknad.Periode.Sykdom(10.januar, 12.januar, 100)
        ).also {
            historikk.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertEquals(2, historikk.size)
        assertEquals(5, historikk.sykdomstidslinje().length())
    }

    @Test
    internal fun `håndterer ubestemt dag`() {
        val hendelse = sykdomshendelse(Ubestemtdag(LocalDate.now()))
        historikk.håndter(hendelse)
        assertTrue(hendelse.hasErrors())
        assertEquals(1, historikk.size)
    }

    @Test
    internal fun `håndterer permisjonsdag fra søknad`() {
        val hendelse = sykdomshendelse(Permisjonsdag.Søknad(LocalDate.now()))
        historikk.håndter(hendelse)
        assertTrue(hendelse.hasErrors())
        assertEquals(1, historikk.size)
    }

    @Test
    internal fun `håndterer permisjonsdag fra aareg`() {
        val hendelse = sykdomshendelse(Permisjonsdag.Aareg(LocalDate.now()))
        historikk.håndter(hendelse)
        assertTrue(hendelse.hasErrors())
        assertEquals(1, historikk.size)
    }

    @Test
    internal fun `Inntektsmelding mottatt`() {
        historikk.håndter(sykmelding(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(
            søknad(
                Søknad.Periode.Sykdom(8.januar, 10.januar, 100),
                Søknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
        historikk.håndter(
            inntektsmelding(
                arbeidsgiverperioder = listOf(Periode(1.januar, 3.januar), Periode(9.januar, 12.januar)),
                ferieperioder = listOf(Periode(4.januar, 8.januar))
            )
        )
        assertEquals(3, historikk.size)
        assertEquals(12, historikk.sykdomstidslinje().length())
        assertEquals(5, inspektør.hendelseSykdomstidslinje[2].length())
        assertEquals(5, inspektør.beregnetSykdomstidslinjer[2].length())
        assertEquals(9, inspektør.hendelseSykdomstidslinje[1].length())
        assertEquals(11, inspektør.beregnetSykdomstidslinjer[1].length())
        assertEquals(12, inspektør.hendelseSykdomstidslinje[0].length())
        assertEquals(12, inspektør.beregnetSykdomstidslinjer[0].length())
    }

    @Test
    internal fun `Inntektsmelding først`() {
        historikk.håndter(inntektsmelding(listOf(Periode(9.januar, 24.januar)), emptyList(), førsteFraværsdag = 9.januar))
        assertEquals(1, historikk.size)
        assertEquals(16, historikk.sykdomstidslinje().length())
        assertEquals(9.januar, inspektør.beregnetSykdomstidslinjer[0].førsteDag())
        assertEquals(24.januar, inspektør.beregnetSykdomstidslinjer[0].sisteDag())
    }

    @Test
    internal fun `Inntektsmelding, etter søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        historikk.håndter(sykmelding(Triple(7.januar, 28.januar, 100)))
        historikk.håndter(søknad(Søknad.Periode.Sykdom(7.januar, 28.januar, 100)))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        historikk.håndter(inntektsmelding(listOf(Periode(9.januar, 24.januar)), emptyList(), førsteFraværsdag = 9.januar))
        assertEquals(3, historikk.size)
        assertEquals(22, historikk.sykdomstidslinje().length())
        assertEquals(7.januar, inspektør.beregnetSykdomstidslinjer[0].førsteDag())
        assertEquals(SykHelgedag.Søknad::class, inspektør.beregnetSykdomstidslinjer[0].dag(7.januar)!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, inspektør.beregnetSykdomstidslinjer[0].dag(8.januar)!!::class)
        assertEquals(9.januar, inspektør.beregnetSykdomstidslinjer[0].førsteFraværsdag())
        assertEquals(28.januar, inspektør.beregnetSykdomstidslinjer[0].sisteDag())
    }

    @Test
    internal fun `Inntektsmelding, før søknad, overskriver sykedager før arbeidsgiverperiode med arbeidsdager`() {
        historikk.håndter(sykmelding(Triple(7.januar, 28.januar, 100)))
        // Need to extend Arbeidsdag from first Arbeidsgiverperiode to beginning of Vedtaksperiode, considering weekends
        historikk.håndter(inntektsmelding(listOf(Periode(9.januar, 24.januar)), emptyList(), førsteFraværsdag = 9.januar))
        assertEquals(2, historikk.size)
        assertEquals(22, historikk.sykdomstidslinje().length())
        assertEquals(7.januar, inspektør.beregnetSykdomstidslinjer[0].førsteDag())
        assertEquals(SykHelgedag.Sykmelding::class, inspektør.beregnetSykdomstidslinjer[0].dag(7.januar)!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, inspektør.beregnetSykdomstidslinjer[0].dag(8.januar)!!::class)
        assertEquals(9.januar, inspektør.beregnetSykdomstidslinjer[0].førsteFraværsdag())
        assertEquals(28.januar, inspektør.beregnetSykdomstidslinjer[0].sisteDag())
    }

    @Test
    internal fun `JSON`() {
        val søknadId = UUID.randomUUID()
        val sykmeldingId = UUID.randomUUID()
        historikk.håndter(sykmelding(Triple(8.januar, 12.januar, 100), hendelseId = sykmeldingId))
        historikk.håndter(
            søknad(
                Søknad.Periode.Sykdom(8.januar, 10.januar, 100),
                Søknad.Periode.Egenmelding(2.januar, 3.januar),
                hendelseId = søknadId
            )
        )
        assertEquals(2, historikk.size)
        assertEquals(11, historikk.sykdomstidslinje().length())
        assertEquals(5, inspektør.hendelseSykdomstidslinje[1].length())
        assertEquals(5, inspektør.beregnetSykdomstidslinjer[1].length())
        assertEquals(9, inspektør.hendelseSykdomstidslinje[0].length())
        assertEquals(11, inspektør.beregnetSykdomstidslinjer[0].length())
        assertEquals(inspektør.hendelser[0], søknadId)
        assertEquals(inspektør.hendelser[1], sykmeldingId)
    }

    private fun sykdomshendelse(dag: Dag): SykdomstidslinjeHendelse {
        return object : SykdomstidslinjeHendelse(UUID.randomUUID()) {
            override fun sykdomstidslinje() = dag
            override fun sykdomstidslinje(tom: LocalDate) = dag
            override fun toSpesifikkKontekst() = SpesifikkKontekst("Testhendelse 1")
            override fun valider() = throw NotImplementedError("not implemented")
            override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = throw NotImplementedError("not implemented")
            override fun aktørId() = throw NotImplementedError("not implemented")
            override fun fødselsnummer() = throw NotImplementedError("not implemented")
            override fun organisasjonsnummer() = throw NotImplementedError("not implemented")
        }
    }

    private fun sykmelding(
        vararg sykeperioder: Triple<LocalDate, LocalDate, Int>,
        hendelseId: UUID = UUID.randomUUID()
    ) = Sykmelding(
        meldingsreferanseId = hendelseId,
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "12345",
        orgnummer = "987654321",
        sykeperioder = listOf(*sykeperioder)
    )

    private fun søknad(
        vararg perioder: Søknad.Periode,
        hendelseId: UUID = UUID.randomUUID()
    ) = Søknad(
        meldingsreferanseId = hendelseId,
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "12345",
        orgnummer = "987654321",
        perioder = listOf(*perioder),
        harAndreInntektskilder = false,
        sendtTilNAV = perioder.last().tom.atStartOfDay()
    )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode>,
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) = Inntektsmelding(
        meldingsreferanseId = UUID.randomUUID(),
        refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
        orgnummer = "88888888",
        fødselsnummer = "12020052345",
        aktørId = "100010101010",
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder
    )

    private class HistorikkInspektør(sykdomshistorikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
        internal val hendelseSykdomstidslinje = mutableListOf<ConcreteSykdomstidslinje>()
        internal val beregnetSykdomstidslinjer = mutableListOf<ConcreteSykdomstidslinje>()
        internal val hendelser = mutableListOf<UUID>()

        init {
            sykdomshistorikk.accept(this)
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            if (hendelseSykdomstidslinje.size == beregnetSykdomstidslinjer.size) {
                hendelseSykdomstidslinje.add(compositeSykdomstidslinje)
            } else {
                beregnetSykdomstidslinjer.add(compositeSykdomstidslinje)
            }
        }

        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            tidsstempel: LocalDateTime
        ) {
            hendelser.add(id)
        }
    }
}
