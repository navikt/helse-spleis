package no.nav.helse.person

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

internal class YtelserHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `ytelser på feil tidspunkt`() {
        person.håndter(ytelser(vedtaksperiodeId = UUID.randomUUID()))
        assertEquals(0, inspektør.vedtaksperiodeTeller)

        person.håndter(nySøknad())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.MOTTATT_NY_SØKNAD)

        person.håndter(sendtSøknad())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.AVVENTER_INNTEKTSMELDING)

        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `historie nyere enn 6 måneder`() {
        val sisteHistoriskeSykedag = førsteSykedag.minusDays(180)
        håndterYtelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                    sisteHistoriskeSykedag.minusDays(14),
                    sisteHistoriskeSykedag,
                    1000
                )
            )
        )

        assertTilstand(TilstandType.TIL_INFOTRYGD)
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
        ) { "Forventet tilstand $expectedTilstand: $aktivitetslogger" }
    }

    private fun håndterYtelser(
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeytelse: Periode? = null,
        svangerskapsytelse: Periode? = null
    ) {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(
            ytelser(
                utbetalinger = utbetalinger,
                foreldrepengeYtelse = foreldrepengeytelse,
                svangerskapYtelse = svangerskapsytelse
            )
        )
    }

    private fun ytelser(
        vedtaksperiodeId: UUID = inspektør.vedtaksperiodeId(0),
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Ytelser(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNR,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = foreldrepengeYtelse,
            svangerskapsytelse = svangerskapYtelse,
            aktivitetslogger = Aktivitetslogger()
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

    private fun nySøknad() =
        NySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(Triple(førsteSykedag, sisteSykedag, 100)),
            aktivitetslogger = Aktivitetslogger()
        )

    private fun sendtSøknad() =
        SendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            sendtNav = LocalDateTime.now(),
            perioder = listOf(SendtSøknad.Periode.Sykdom(førsteSykedag, sisteSykedag, 100)),
            aktivitetslogger = Aktivitetslogger(),
            harAndreInntektskilder = false
        )

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            null,
            31000.0,
            emptyList()
        )
    ) =
        Inntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            mottattDato = 1.februar.atStartOfDay(),
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        )

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            rapportertDato = LocalDateTime.now(),
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2018, it), listOf(31000.0)
                )
            },
            erEgenAnsatt = false,
            aktivitetslogger = Aktivitetslogger(),
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNR, 1.januar(2017))))
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val vedtaksperiodeIder = mutableSetOf<UUID>()
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

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            this@YtelserHendelseTest.aktivitetslogger = aktivitetslogger
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)
        internal fun tilstand(indeks: Int) = tilstander[indeks]
    }
}
