package no.nav.helse.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.*

internal class YtelserHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `ytelser på feil tidspunkt`() {
        assertThrows<Aktivitetslogg.AktivitetException> { person.håndter(ytelser(vedtaksperiodeId = UUID.randomUUID())) }
        assertEquals(0, inspektør.vedtaksperiodeTeller)

        person.håndter(sykmelding())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)

        person.håndter(søknad())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP)

        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP)
    }

    @Test
    fun `historie nyere enn perioden`() {
        val sisteHistoriskeSykedag = førsteSykedag.plusMonths(2)
        håndterYtelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                    sisteHistoriskeSykedag.minusDays(14),
                    sisteHistoriskeSykedag,
                    1000
                )
            )
        )
    }

    @Test
    fun `ugyldig utbetalinghistorikk før inntektsmelding kaster perioden ut`() {
        håndterUgyldigYtelser()
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `ugyldig utbetalinghistorikk etter inntektsmelding kaster perioden ut`() {
        håndterYtelser(ukjentePerioder = listOf(jacksonObjectMapper().createObjectNode()))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `fordrepengeytelse før periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)
    }

    @Test
    fun `fordrepengeytelse i periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `fordrepengeytelse etter periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)
    }

    @Test
    fun `svangerskapsytelse før periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)
    }

    @Test
    fun `svangerskapsytelse i periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `svangerskapsytelse etter periode`() {
        håndterYtelser(svangerskapsytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)
    }

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.tilstand(0)
        ) { "Forventet tilstand $expectedTilstand: ${inspektør.personlogg}" }
    }

    private fun håndterYtelser(
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeytelse: Periode? = null,
        svangerskapsytelse: Periode? = null,
        ukjentePerioder: List<JsonNode> = emptyList()
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(
            ytelser(
                utbetalinger = utbetalinger,
                ukjentePerioder = ukjentePerioder,
                foreldrepengeYtelse = foreldrepengeytelse,
                svangerskapYtelse = svangerskapsytelse
            )
        )
    }

    private fun håndterUgyldigYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(ytelser(
            utbetalinger = emptyList(),
            ukjentePerioder = listOf(jacksonObjectMapper().createObjectNode()),
            foreldrepengeYtelse = null,
            svangerskapYtelse = null
        ))
    }

    private fun ytelser(
        vedtaksperiodeId: UUID = inspektør.vedtaksperiodeId(0),
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        ukjentePerioder: List<JsonNode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                ukjentePerioder = ukjentePerioder,
                utbetalinger = utbetalinger,
                inntektshistorikk = emptyList(),
                graderingsliste = emptyList(),
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
                aktivitetslogg = it
            ),
            aktivitetslogg = it
        )
    }
    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            sykeperioder = listOf(Triple(førsteSykedag, sisteSykedag, 100))
        )

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            perioder = listOf(Søknad.Periode.Sykdom(førsteSykedag, sisteSykedag, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay()
        )

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            null,
            31000.0,
            emptyList()
        )
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList()
        )

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2018, it), listOf(31000.0)
                )
            },
            erEgenAnsatt = false,
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNR, 1.januar(2017)))
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val vedtaksperiodeIder = mutableSetOf<UUID>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
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
    }
}
