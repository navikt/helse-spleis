package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var personObserver: TestPersonObserver
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun opprettPerson() {
        personObserver = TestPersonObserver()
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(personObserver)
        aktivitetslogger = Aktivitetslogger()
    }

    @Test
    fun `egen ansatt`() {
        håndterVilkårsgrunnlag(egenAnsatt = true, inntekter = emptyList())

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.vedtaksperiodeIder.size)
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `avvik i inntekt`() {
        håndterVilkårsgrunnlag(egenAnsatt = false, inntekter = emptyList())

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.vedtaksperiodeIder.size)
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `ikke egen ansatt og ingen avvik i inntekt`() {
        val månedslønn = 1000.0
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = månedslønn,
            inntekter = tolvMånederMedInntekt(månedslønn)
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.vedtaksperiodeIder.size)
        assertTilstand(TilstandType.AVVENTER_HISTORIKK)
        val utgangspunktForBeregningAvYtelse = inspektør.sykdomstidslinje(0).førsteDag()
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(0)
        assertEquals(
            utgangspunktForBeregningAvYtelse.minusDays(1),
            personObserver.etterspurtBehov(
                vedtaksperiodeId,
                Behovstype.Sykepengehistorikk,
                "utgangspunktForBeregningAvYtelse"
            )
        )
    }

    @Test
    fun `ikke egen ansatt og mer enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0
        val `25 % mer` = månedslønn * 1.25 + 1
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mer`,
            inntekter = tolvMånederMedInntekt(månedslønn)
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.vedtaksperiodeIder.size)
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `ikke egen ansatt og mindre enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0
        val `25 % mindre` = månedslønn * 0.75 - 1
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mindre`,
            inntekter = tolvMånederMedInntekt(månedslønn)
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.vedtaksperiodeIder.size)
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    private fun tolvMånederMedInntekt(beregnetInntekt: Double): List<ModelVilkårsgrunnlag.Måned> {
        return (1..12).map {
            ModelVilkårsgrunnlag.Måned(
                YearMonth.of(2018, it), listOf(
                    ModelVilkårsgrunnlag.Inntekt(beregnetInntekt)
                )
            )
        }
    }

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.tilstand(0)
        ) { "Forventet tilstand $expectedTilstand: $aktivitetslogger" }
    }

    private fun håndterVilkårsgrunnlag(
        egenAnsatt: Boolean,
        beregnetInntekt: Double = 1000.0,
        inntekter: List<ModelVilkårsgrunnlag.Måned>
    ) {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding(beregnetInntekt = beregnetInntekt))
        person.håndter(vilkårsgrunnlag(egenAnsatt = egenAnsatt, inntekter = inntekter))
    }

    private fun nySøknad() =
        ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(Triple(1.januar, 31.januar, 100)),
            aktivitetslogger = aktivitetslogger
        )

    private fun sendtSøknad() =
        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            sendtNav = LocalDateTime.now(),
            perioder = listOf(ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100)),
            aktivitetslogger = aktivitetslogger
        )

    private fun inntektsmelding(beregnetInntekt: Double) =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(null, beregnetInntekt, emptyList()),
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            mottattDato = 1.februar.atStartOfDay(),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = emptyList(),
            aktivitetslogger = aktivitetslogger
        )

    private fun vilkårsgrunnlag(
        egenAnsatt: Boolean,
        inntekter: List<ModelVilkårsgrunnlag.Måned>
    ) =
        ModelVilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            rapportertDato = LocalDateTime.now(),
            inntektsmåneder = inntekter,
            erEgenAnsatt = egenAnsatt,
            aktivitetslogger = aktivitetslogger
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {

        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        internal val vedtaksperiodeIder = mutableSetOf<UUID>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
            vedtaksperiodeIder.add(id)
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            sykdomstidslinjer[vedtaksperiodeindeks] = compositeSykdomstidslinje
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)

        internal fun tilstand(indeks: Int) = tilstander[indeks]

        internal fun sykdomstidslinje(indeks: Int) = sykdomstidslinjer[indeks] ?: throw IllegalAccessException()

    }

    private inner class TestPersonObserver : PersonObserver {
        private val etterspurteBehov = mutableMapOf<UUID, MutableList<Behov>>()

        fun etterspurteBehov(vedtaksperiodeId: UUID) = etterspurteBehov.getValue(vedtaksperiodeId).toList()

        fun <T> etterspurtBehov(vedtaksperiodeId: UUID, behov: Behovstype, felt: String): T? {
            return personObserver.etterspurteBehov(vedtaksperiodeId)
                .first { behov.name in it.behovType() }[felt]
        }

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            etterspurteBehov.computeIfAbsent(UUID.fromString(behov.vedtaksperiodeId())) { mutableListOf() }
                .add(behov)
        }
    }
}
