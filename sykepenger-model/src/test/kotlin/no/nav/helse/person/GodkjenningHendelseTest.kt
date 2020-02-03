package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class GodkjenningHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
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
    fun `utbetaling er godkjent`() {
        håndterYtelser()
        person.håndter(manuellSaksbehandling(true))
        assertTilstand(TilstandType.TIL_UTBETALING)
        val utbetalingsreferanse = personObserver.etterspurtBehov<String>(Behovstype.Utbetaling, "utbetalingsreferanse")
        assertNotNull(utbetalingsreferanse)
    }

    @Test
    fun `utbetaling ikke godkjent`() {
        håndterYtelser()
        person.håndter(manuellSaksbehandling(false))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `hendelse etter til utbetaling`() {
        håndterYtelser()
        person.håndter(manuellSaksbehandling(true))
        assertTilstand(TilstandType.TIL_UTBETALING)
        person.håndter(ytelser())
        assertTilstand(TilstandType.TIL_UTBETALING)
        assertEquals(1, personObserver.vedtaksperiodeIder.size)
    }

    @Test
    fun `dobbelt svar fra saksbehandler`() {
        håndterYtelser()
        person.håndter(manuellSaksbehandling(true))
        person.håndter(manuellSaksbehandling(true))
        assertTilstand(TilstandType.TIL_UTBETALING)
    }

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.tilstand(0)
        ) { "Forventet tilstand $expectedTilstand: $aktivitetslogger" }
    }

    private fun håndterYtelser() {
        person.håndter(nySøknad())
        person.håndter(sendtSøknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun manuellSaksbehandling(godkjent: Boolean) = ModelManuellSaksbehandling(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = personObserver.vedtaksperiodeId(0).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = godkjent,
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

    private fun ytelser(
        vedtaksperiodeId: UUID = personObserver.vedtaksperiodeId(0),
        utbetalinger: List<ModelSykepengehistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = foreldrepengeYtelse,
            svangerskapsytelse = svangerskapYtelse,
            aktivitetslogger = Aktivitetslogger()
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

    private fun nySøknad() =
        ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(Triple(førsteSykedag, sisteSykedag, 100)),
            aktivitetslogger = Aktivitetslogger()
        )

    private fun sendtSøknad() =
        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            rapportertdato = LocalDateTime.now(),
            perioder = listOf(ModelSendtSøknad.Periode.Sykdom(førsteSykedag, sisteSykedag, 100)),
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
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 1000.0,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
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

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = TilstandType.START
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            sykdomstidslinjer[vedtaksperiodeindeks] = compositeSykdomstidslinje
        }

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            this@GodkjenningHendelseTest.aktivitetslogger = aktivitetslogger
        }

        internal fun tilstand(indeks: Int) = tilstander[indeks]
    }

    private inner class TestPersonObserver : PersonObserver {
        val vedtaksperiodeIder = mutableSetOf<UUID>()
        private val etterspurteBehov = mutableMapOf<UUID, MutableList<Behov>>()

        fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)

        fun etterspurteBehov() =
            etterspurteBehov.getValue(vedtaksperiodeIder.elementAt(0)).toList()

        fun <T> etterspurtBehov(behov: Behovstype, felt: String): T? {
            return personObserver.etterspurteBehov()
                .first { behov.name in it.behovType() }[felt]
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            vedtaksperiodeIder.add(event.id)
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            etterspurteBehov.computeIfAbsent(UUID.fromString(event.vedtaksperiodeId())) { mutableListOf() }
                .add(event)
        }
    }
}
