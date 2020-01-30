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
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattNySøknad.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSendtSøknad.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattInntektsmelding.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.Vilkårsprøving.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.BeregnUtbetaling.timeout)
        assertEquals(Duration.ofDays(7), Vedtaksperiode.TilGodkjenning.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilUtbetaling.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilInfotrygd.timeout)
    }

    @Test
    fun `påminnelse i ny søknad`() {
        person.håndter(nySøknad())
        person.håndter(påminnelse(TilstandType.MOTTATT_NY_SØKNAD))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i sendt søknad`() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(påminnelse(TilstandType.MOTTATT_SENDT_SØKNAD))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i mottatt inntektsmelding`() {
        person.håndter(nySøknad())
        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.MOTTATT_INNTEKTSMELDING))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i vilkårsprøving`() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.VILKÅRSPRØVING))
        assertTilstand(TilstandType.VILKÅRSPRØVING)
        assertBehov(
            behov = personObserver.etterspurteBehov(0),
            antall = 2,
            inneholder = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt)
        )
    }

    @Test
    fun `påminnelse i ytelser`() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(TilstandType.BEREGN_UTBETALING))
        assertTilstand(TilstandType.BEREGN_UTBETALING)
        assertBehov(
            behov = personObserver.etterspurteBehov(0),
            antall = 2,
            inneholder = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        )
    }

    @Test
    fun `påminnelse i til godkjenning`() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(påminnelse(TilstandType.TIL_GODKJENNING))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
        assertBehov(
            behov = personObserver.etterspurteBehov(0),
            antall = 1,
            inneholder = listOf(Behovstype.GodkjenningFraSaksbehandler)
        )
    }

    @Test
    fun `påminnelse i til utbetaling`() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(manuellSaksbehandling())
        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
        assertBehov(
            behov = personObserver.etterspurteBehov(0),
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
        person.håndter(nySøknad())
        person.håndter(påminnelse(TilstandType.TIL_INFOTRYGD))
        assertTilstand(TilstandType.MOTTATT_NY_SØKNAD)

        person.håndter(sendtSøknad())
        person.håndter(påminnelse(TilstandType.MOTTATT_NY_SØKNAD))
        assertTilstand(TilstandType.MOTTATT_SENDT_SØKNAD)

        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.MOTTATT_SENDT_SØKNAD))
        assertTilstand(TilstandType.VILKÅRSPRØVING)

        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(TilstandType.VILKÅRSPRØVING))
        assertTilstand(TilstandType.BEREGN_UTBETALING)

        person.håndter(ytelser())
        person.håndter(påminnelse(TilstandType.BEREGN_UTBETALING))
        assertTilstand(TilstandType.TIL_GODKJENNING)

        person.håndter(manuellSaksbehandling())
        person.håndter(påminnelse(TilstandType.TIL_GODKJENNING))
        assertTilstand(TilstandType.TIL_UTBETALING)

        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
    }

    private fun sendtSøknad() =
        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            rapportertdato = LocalDateTime.now(),
            perioder = listOf(ModelSendtSøknad.Periode.Sykdom(1.januar, 20.januar, 100)),
            originalJson = "{}",
            aktivitetslogger = Aktivitetslogger()
        )

    private fun nySøknad() =
        ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(Triple(1.januar, 20.januar, 100)),
            originalJson = "{}",
            aktivitetslogger = Aktivitetslogger()
        )

    private fun inntektsmelding() =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(null, 1000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            mottattDato = 1.februar.atStartOfDay(),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.0,
            originalJson = "{}",
            arbeidsgiverperioder = listOf(Periode(1.januar, 1.januar.plusDays(15))),
            ferieperioder = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        )

    private fun vilkårsgrunnlag() =
        ModelVilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = personObserver.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            rapportertDato = LocalDateTime.now(),
            inntektsmåneder = (1..12).map {
                ModelVilkårsgrunnlag.Måned(
                    YearMonth.of(2018, it), listOf(
                        ModelVilkårsgrunnlag.Inntekt(1000.0)
                    )
                )
            },
            erEgenAnsatt = false,
            aktivitetslogger = Aktivitetslogger()
        )

    private fun ytelser() = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = personObserver.vedtaksperiodeId(0).toString(),
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = listOf(
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    17.januar(2017),
                    20.januar(2017),
                    1000
                )
            ),
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = null,
            svangerskapsytelse = null,
            aktivitetslogger = Aktivitetslogger()
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

    private fun manuellSaksbehandling() = ModelManuellSaksbehandling(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = personObserver.vedtaksperiodeId(0).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = true,
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

    private fun påminnelse(tilstandType: TilstandType) = ModelPåminnelse(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = personObserver.vedtaksperiodeId(0).toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
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

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            sykdomstidslinjer[vedtaksperiodeindeks] = compositeSykdomstidslinje
        }

        internal fun tilstand(indeks: Int) = tilstander[indeks]

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            this@PåminnelserOgTimeoutTest.aktivitetslogger = aktivitetslogger
        }
    }

    private inner class TestPersonObserver : PersonObserver {
        val vedtaksperiodeIder = mutableSetOf<UUID>()
        private val etterspurteBehov = mutableMapOf<UUID, MutableList<Behov>>()

        fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)

        fun etterspurteBehov(vedtaksperiodeindeks: Int) =
            etterspurteBehov.getValue(vedtaksperiodeId(vedtaksperiodeindeks)).toList()

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            vedtaksperiodeIder.add(event.id)
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            etterspurteBehov.computeIfAbsent(UUID.fromString(event.vedtaksperiodeId())) { mutableListOf() }
                .add(event)
        }
    }
}
