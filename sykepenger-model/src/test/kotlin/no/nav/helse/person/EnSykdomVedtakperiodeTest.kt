package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.fixtures.desember
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.MOTTATT_NY_SØKNAD
import no.nav.helse.person.TilstandType.START
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal class EnSykdomVedtakperiodeTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 1000.00
        private val rapportertdato = 1.februar.atStartOfDay()
    }

    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var vedtaksperiodeId: String
    private var forventetEndringTeller = 0

    @BeforeEach
    internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    @Test
    fun `motta ny søknad`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
        }
        assertTilstander(0, START, MOTTATT_NY_SØKNAD)
    }

    private fun assertEndringTeller() {
        forventetEndringTeller += 1
        assertEquals(forventetEndringTeller, observatør.endreTeller)
    }

    private fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[indeks])
    }

    private fun assertNoErrors(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogger.hasErrors())
        assertFalse(inspektør.arbeidsgiverLogger.hasErrors())
        assertFalse(inspektør.periodeLogger.hasErrors())
    }

    private fun assertNoWarnings(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogger.hasWarnings())
        assertFalse(inspektør.arbeidsgiverLogger.hasWarnings())
        assertFalse(inspektør.periodeLogger.hasWarnings())
    }

    private fun assertMessages(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogger.hasMessages())
        assertTrue(inspektør.arbeidsgiverLogger.hasMessages())
//        assertTrue(inspektør.periodeLogger.hasMessages())
    }

    private fun håndterNySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(nySøknad(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterSendtSøknad(vararg perioder: ModelSendtSøknad.Periode) {
        assertFalse(observatør.ettersburteBehov(Inntektsberegning))
        assertFalse(observatør.ettersburteBehov(EgenAnsatt))
        person.håndter(sendtSøknad(*perioder))
        assertEndringTeller()
    }

    private fun håndterInntektsmelding(arbeidsgiverperioder: List<Periode>) {
        assertFalse(observatør.ettersburteBehov(Inntektsberegning))
        assertFalse(observatør.ettersburteBehov(EgenAnsatt))
        person.håndter(inntektsmelding(arbeidsgiverperioder))
        assertEndringTeller()
    }

    private fun håndterVilkårsgrunnlag(inntekt: Double) {
        assertTrue(observatør.ettersburteBehov(Inntektsberegning))
        assertTrue(observatør.ettersburteBehov(EgenAnsatt))
        assertFalse(observatør.ettersburteBehov(Sykepengehistorikk))
        assertFalse(observatør.ettersburteBehov(Foreldrepenger))
        person.håndter(vilkårsgrunnlag(INNTEKT))
        assertEndringTeller()
    }

    private fun håndterYtelser(utbetalinger: List<Triple<LocalDate, LocalDate, Int>>) {
        assertTrue(observatør.ettersburteBehov(Sykepengehistorikk))
        assertTrue(observatør.ettersburteBehov(Foreldrepenger))
        assertFalse(observatør.ettersburteBehov(GodkjenningFraSaksbehandler))
        person.håndter(ytelser(utbetalinger))
        assertEndringTeller()
    }

    private fun håndterManuelSaksbehandling(utbetalingGodkjent: Boolean) {
        assertTrue(observatør.ettersburteBehov(GodkjenningFraSaksbehandler))
        person.håndter(manuellSaksbehandling(utbetalingGodkjent))
        assertEndringTeller()
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        orgnummer = ORGNUMMER,
        rapportertdato = rapportertdato,
        sykeperioder = listOf(*sykeperioder),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode) = ModelSendtSøknad(
        hendelseId = UUID.randomUUID(),
        fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        orgnummer = ORGNUMMER,
        rapportertdato = rapportertdato,
        perioder = listOf(*perioder),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) = ModelInntektsmelding(
        hendelseId = UUID.randomUUID(),
        refusjon = ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
        orgnummer = ORGNUMMER,
        fødselsnummer = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        mottattDato = rapportertdato,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        originalJson = "{}",
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        aktivitetslogger = Aktivitetslogger()
    )

    private fun vilkårsgrunnlag(inntekt: Double) = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        rapportertDato = rapportertdato,
        inntektsmåneder = (1..12).map {
            ModelVilkårsgrunnlag.Måned(
                YearMonth.of(2017, it),
                listOf(ModelVilkårsgrunnlag.Inntekt(inntekt))
            )
        },
        erEgenAnsatt = false,
        aktivitetslogger = Aktivitetslogger(),
        originalJson = "{}"
    )

    private fun ytelser(
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ) = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = vedtaksperiodeId,
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = utbetalinger.map {
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    it.first,
                    it.second,
                    it.third
                )
            },
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(foreldrepenger, svangerskapspenger, Aktivitetslogger()),
        rapportertdato = rapportertdato,
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

    private fun manuellSaksbehandling(utbetalingGodkjent: Boolean) = ModelManuellSaksbehandling(
        hendelseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = utbetalingGodkjent,
        rapportertdato = rapportertdato,
        aktivitetslogger = Aktivitetslogger()
    )

    private inner class TestObservatør : PersonObserver {
        internal var endreTeller = 0
        private val etterspurteBehov = mutableMapOf<String, Boolean>()
        private var periodeIndek = -1
        private val periodeIndekser = mutableMapOf<UUID, Int>()
        internal val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()

        internal fun ettersburteBehov(key: Behovstype) = etterspurteBehov.getOrDefault(key.name, false)

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            endreTeller += 1
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            val indeks = periodeIndekser.getOrPut(event.id, {
                periodeIndek++
                tilstander[periodeIndek] = mutableListOf(START)
                periodeIndek
            })
            tilstander[indeks]?.add(event.gjeldendeTilstand) ?: fail("Missing collection initialization")
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            event.behovType().forEach { etterspurteBehov[it] = true }
            vedtaksperiodeId = event.vedtaksperiodeId()
        }

    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
        private val sykdomstidslinjer = mutableMapOf<Int, CompositeSykdomstidslinje>()
        internal lateinit var personLogger: Aktivitetslogger
        internal lateinit var arbeidsgiver: Arbeidsgiver
        internal lateinit var arbeidsgiverLogger: Aktivitetslogger
        internal lateinit var periodeLogger: Aktivitetslogger
        internal lateinit var inntektshistorikk: Inntekthistorikk
        internal lateinit var sykdomshistorikk: Sykdomshistorikk
        internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            personLogger = aktivitetslogger
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver
        }

        override fun visitArbeidsgiverAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            arbeidsgiverLogger = aktivitetslogger
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            this.inntektshistorikk = inntekthistorikk
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = mutableListOf()
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            this.sykdomshistorikk = sykdomshistorikk
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
        }

        private inner class Dagteller : SykdomstidslinjeVisitor {
            override fun visitSykedag(sykedag: Sykedag) = inkrementer(Sykedag::class)

            override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = inkrementer(SykHelgedag::class)

            private fun inkrementer(klasse: KClass<out Dag>) {
                dagtelling.compute(klasse) { _, value ->
                    1 + (value ?: 0)
                }
            }
        }

        override fun visitVedtaksperiodeAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            periodeLogger = aktivitetslogger
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks]?.add(tilstand.type) ?: fail("Missing collection initialization")
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks] ?: fail("Missing collection initialization")
    }
}
