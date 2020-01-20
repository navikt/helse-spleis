package no.nav.helse.sykdomstidslinje

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SykdomshistorikkTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var inspektør: HistorikkInspektør

    private lateinit var historikk: Sykdomshistorikk
    @BeforeEach internal fun initialiser() {
        historikk = Sykdomshistorikk()
    }

    @Test
    internal fun `NySøknad mottatt`() {
        historikk.håndter(nySøknad(Triple(1.januar, 5.januar, 100)))
        assertEquals(1, historikk.size)
        assertEquals(5, historikk.sykdomstidslinje().length())
    }

    @Test
    internal fun `SendtSøknad mottatt`() {
        historikk.håndter(nySøknad(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(sendtSøknad(
            ModelSendtSøknad.Periode.Sykdom(8.januar,10.januar, 100),
            ModelSendtSøknad.Periode.Egenmelding(2.januar, 3.januar)
        ))
        val inspektør = HistorikkInspektør(historikk)
        assertEquals(2, historikk.size)
        assertEquals(11, historikk.sykdomstidslinje().length())
        assertEquals(5, inspektør.hendelseSykdomstidslinje[1].length())
        assertEquals(5, inspektør.beregnetSykdomstidslinjer[1].length())
        assertEquals(9, inspektør.hendelseSykdomstidslinje[0].length())
        assertEquals(11, inspektør.beregnetSykdomstidslinjer[0].length())
        assertTrue(inspektør.hendelser[0] is ModelSendtSøknad)
        assertTrue(inspektør.hendelser[1] is ModelNySøknad)

    }

    @Test internal fun `Inntektsmelding mottatt`() {
        historikk.håndter(nySøknad(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(sendtSøknad(
            ModelSendtSøknad.Periode.Sykdom(8.januar,10.januar, 100),
            ModelSendtSøknad.Periode.Egenmelding(2.januar, 3.januar)
        ))
        historikk.håndter(inntektsmelding(
            listOf(1.januar..3.januar, 9.januar..12.januar),
            listOf(4.januar..8.januar)))
        val inspektør = HistorikkInspektør(historikk)
        assertEquals(3, historikk.size)
        assertEquals(12, historikk.sykdomstidslinje().length())
        assertEquals(5, inspektør.hendelseSykdomstidslinje[2].length())
        assertEquals(5, inspektør.beregnetSykdomstidslinjer[2].length())
        assertEquals(9, inspektør.hendelseSykdomstidslinje[1].length())
        assertEquals(11, inspektør.beregnetSykdomstidslinjer[1].length())
        assertEquals(12, inspektør.hendelseSykdomstidslinje[0].length())
        assertEquals(12, inspektør.beregnetSykdomstidslinjer[0].length())
        assertTrue(inspektør.hendelser[0] is ModelInntektsmelding)
        assertTrue(inspektør.hendelser[1] is ModelSendtSøknad)
        assertTrue(inspektør.hendelser[2] is ModelNySøknad)

    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>)
         = ModelNySøknad(
            UUID.randomUUID(),
            UNG_PERSON_FNR_2018,
            "12345",
            "987654321",
            LocalDateTime.now(),
            listOf(*sykeperioder),
            Aktivitetslogger(),
            "{}"
        )

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode)
         = ModelSendtSøknad(
            UUID.randomUUID(),
            UNG_PERSON_FNR_2018,
            "12345",
            "987654321",
            LocalDateTime.now(),
            listOf(*perioder),
            Aktivitetslogger()
        )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<ClosedRange<LocalDate>>,
        ferieperioder: List<ClosedRange<LocalDate>>,
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) = ModelInntektsmelding(
            UUID.randomUUID(),
            ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            "88888888",
            "12020052345",
            "100010101010",
            1.februar.atStartOfDay(),
            førsteFraværsdag,
            beregnetInntekt,
            Aktivitetslogger(),
            arbeidsgiverperioder,
            ferieperioder
        )

    private class HistorikkInspektør(sykdomshistorikk: Sykdomshistorikk): SykdomshistorikkVisitor {
        internal val hendelseSykdomstidslinje = mutableListOf<ConcreteSykdomstidslinje>()
        internal val beregnetSykdomstidslinjer = mutableListOf<ConcreteSykdomstidslinje>()
        internal val hendelser = mutableListOf<SykdomstidslinjeHendelse>()

        init {
            sykdomshistorikk.accept(this)
        }

        override fun preVisitComposite(tidslinje: CompositeSykdomstidslinje) {
            if (hendelseSykdomstidslinje.size == beregnetSykdomstidslinjer.size) {
                hendelseSykdomstidslinje.add(tidslinje)
            } else {
                beregnetSykdomstidslinjer.add(tidslinje)
            }
        }

        override fun visitHendelse(hendelse: SykdomstidslinjeHendelse) {
            hendelser.add(hendelse)
        }
    }

}
