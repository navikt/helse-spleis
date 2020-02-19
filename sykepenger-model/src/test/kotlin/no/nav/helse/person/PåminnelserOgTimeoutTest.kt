package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class PåminnelserOgTimeoutTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "1234"
    }

    private lateinit var person: Person
    private lateinit var personObserver: TestPersonObserver
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun opprettPerson() {
        personObserver = TestPersonObserver()
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(personObserver)
    }

    @Test
    fun `timeoutverdier`() {
        assertEquals(Duration.ofDays(30), Vedtaksperiode.StartTilstand.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSykmelding.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmelding.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerSøknad.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.AvventerVilkårsprøving.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.AvventerHistorikk.timeout)
        assertEquals(Duration.ofDays(7), Vedtaksperiode.AvventerGodkjenning.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilUtbetaling.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilInfotrygd.timeout)
    }

    @Test
    fun `påminnelse i mottatt sykmelding`() {
        person.håndter(sykmelding())
        person.håndter(påminnelse(TilstandType.MOTTATT_SYKMELDING))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i mottatt søknad`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        assertTilstand(TilstandType.UNDERSØKER_HISTORIKK)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 1,
            inneholder = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        )
        person.håndter(påminnelse(TilstandType.UNDERSØKER_HISTORIKK))
        assertTilstand(TilstandType.UNDERSØKER_HISTORIKK)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 2,
            inneholder = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        )
    }

    @Test
    fun `påminnelse i mottatt inntektsmelding`() {
        person.håndter(sykmelding())
        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.AVVENTER_SØKNAD))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i vilkårsprøving`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.AVVENTER_VILKÅRSPRØVING))
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 2,
            inneholder = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt, Behovstype.Opptjening)
        )
    }

    @Test
    fun `påminnelse i ytelser`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(TilstandType.AVVENTER_HISTORIKK))
        assertTilstand(TilstandType.AVVENTER_HISTORIKK)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 3,
            inneholder = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        )
    }

    @Test
    fun `påminnelse i til godkjenning`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(påminnelse(TilstandType.AVVENTER_GODKJENNING))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 1,
            inneholder = listOf(Behovstype.Godkjenning)
        )
    }

    @Test
    fun `påminnelse i til utbetaling`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(manuellSaksbehandling())
        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
        assertBehov(
            behov = personObserver.etterspurteBehov(inspektør.vedtaksperiodeId(0)),
            antall = 1,
            inneholder = listOf(Behovstype.Utbetaling)
        )
    }

    private fun assertBehov(behov: List<Behov>, antall: Int, inneholder: List<Behovstype>) {
        val behovTyperAsString = inneholder.map { it.name }
        assertEquals(antall, behov
            .filter { it.behovType() == behovTyperAsString }
            .count()
        )
    }

    @Test
    fun `ignorerer påminnelser på tidligere tilstander`() {
        person.håndter(sykmelding())
        person.håndter(påminnelse(TilstandType.TIL_INFOTRYGD))
        assertTilstand(TilstandType.MOTTATT_SYKMELDING)

        person.håndter(søknad())
        person.håndter(påminnelse(TilstandType.MOTTATT_SYKMELDING))
        assertTilstand(TilstandType.UNDERSØKER_HISTORIKK)

        person.håndter(påminnelse(TilstandType.UNDERSØKER_HISTORIKK))
        assertTilstand(TilstandType.UNDERSØKER_HISTORIKK)

        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.AVVENTER_INNTEKTSMELDING))
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING)

        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(TilstandType.AVVENTER_VILKÅRSPRØVING))
        assertTilstand(TilstandType.AVVENTER_HISTORIKK)

        person.håndter(ytelser())
        person.håndter(påminnelse(TilstandType.AVVENTER_HISTORIKK))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)

        person.håndter(manuellSaksbehandling())
        person.håndter(påminnelse(TilstandType.AVVENTER_GODKJENNING))
        assertTilstand(TilstandType.TIL_UTBETALING)

        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
    }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(1.januar, 20.januar, 100)),
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            harAndreInntektskilder = false
        )

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(1.januar, 20.januar, 100)),
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg()
        )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(1.januar, 1.januar.plusDays(15))),
            ferieperioder = emptyList(),
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg()
        )

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2017, it), listOf(31000.0)
                )
            },
            erEgenAnsatt = false,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017))))
        )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        utbetalingshistorikk = Utbetalingshistorikk(
            ukjentePerioder = emptyList(),
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                    17.januar(2017),
                    20.januar(2017),
                    1000
                )
            ),
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg()
        ),
        foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = null,
            svangerskapsytelse = null,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg()
        ),
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun manuellSaksbehandling() = ManuellSaksbehandling(
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = true,
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun påminnelse(tilstandType: TilstandType) = Påminnelse(
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.tilstand(0)
        ) { "Forventet tilstand $expectedTilstand: $aktivitetslogger" }
    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
        private val vedtaksperiodeIder = mutableSetOf<UUID>()

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

        internal fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)
        internal fun tilstand(indeks: Int) = tilstander[indeks]
        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            this@PåminnelserOgTimeoutTest.aktivitetslogger = aktivitetslogger
        }

    }
    private inner class TestPersonObserver : PersonObserver {

        private val etterspurteBehov = mutableMapOf<UUID, MutableList<Behov>>()

        fun etterspurteBehov(vedtaksperiodeId: UUID) =
            etterspurteBehov.getValue(vedtaksperiodeId).toList()

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            etterspurteBehov.computeIfAbsent(UUID.fromString(behov.vedtaksperiodeId())) { mutableListOf() }
                .add(behov)
        }
    }
}
