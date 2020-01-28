package no.nav.helse.sykdomstidslinje

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.SykdomshistorikkVisitor
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

    private lateinit var inspektør: HistorikkInspektør

    private lateinit var historikk: Sykdomshistorikk
    @BeforeEach
    internal fun initialiser() {
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
        historikk.håndter(
            sendtSøknad(
                ModelSendtSøknad.Periode.Sykdom(8.januar, 10.januar, 100),
                ModelSendtSøknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
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

    @Test
    internal fun `Håndterer Ubestemt dag`() {
        historikk.håndter(nySøknad(Triple(8.januar, 12.januar, 100)))
        sendtSøknad(
            ModelSendtSøknad.Periode.Utdanning(9.januar, 12.januar),
            ModelSendtSøknad.Periode.Sykdom(10.januar, 12.januar, 100)
        ).also {
            historikk.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertEquals(2, historikk.size)
        assertEquals(5, historikk.sykdomstidslinje().length())
    }

    @Test
    internal fun `Inntektsmelding mottatt`() {
        historikk.håndter(nySøknad(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(
            sendtSøknad(
                ModelSendtSøknad.Periode.Sykdom(8.januar, 10.januar, 100),
                ModelSendtSøknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
        historikk.håndter(
            inntektsmelding(
                listOf(Periode(1.januar, 3.januar), Periode(9.januar, 12.januar)),
                listOf(Periode(4.januar, 8.januar))
            )
        )
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

    @Test
    internal fun `JSON`() {
        historikk.håndter(nySøknad(Triple(8.januar, 12.januar, 100)))
        historikk.håndter(
            sendtSøknad(
                ModelSendtSøknad.Periode.Sykdom(8.januar, 10.januar, 100),
                ModelSendtSøknad.Periode.Egenmelding(2.januar, 3.januar)
            )
        )
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

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "12345",
        orgnummer = "987654321",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(*sykeperioder),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode) = ModelSendtSøknad(
        hendelseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "12345",
        orgnummer = "987654321",
        rapportertdato = LocalDateTime.now(),
        perioder = listOf(*perioder),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode>,
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) = ModelInntektsmelding(
        hendelseId = UUID.randomUUID(),
        refusjon = ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
        orgnummer = "88888888",
        fødselsnummer = "12020052345",
        aktørId = "100010101010",
        mottattDato = 1.februar.atStartOfDay(),
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        originalJson = "{}",
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        aktivitetslogger = Aktivitetslogger()
    )

    private class HistorikkInspektør(sykdomshistorikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
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
