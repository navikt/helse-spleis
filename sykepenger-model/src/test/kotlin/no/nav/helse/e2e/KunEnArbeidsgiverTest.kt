package no.nav.helse.e2e

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Sykdom
import no.nav.helse.person.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal class KunEnArbeidsgiverTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00
        private val rapportertdato = 1.februar.atStartOfDay()
    }

    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var hendelselogger: Aktivitetslogger
    private var forventetEndringTeller = 0

    @BeforeEach internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also {person.addObserver(it)}
    }

    @Test internal fun `ingen historie med SendtSøknad først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0, emptyList())   // No history
        håndterManuelSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(0,
            START, MOTTATT_NY_SØKNAD, MOTTATT_SENDT_SØKNAD,
            VILKÅRSPRØVING, BEREGN_UTBETALING, TIL_GODKJENNING, TIL_UTBETALING)
    }

    @Test internal fun `ingen historie med Inntektsmelding først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterSendtSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0, emptyList())   // No history
        håndterManuelSaksbehandling(0,true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
        }
        assertTilstander(0,
            START, MOTTATT_NY_SØKNAD, MOTTATT_INNTEKTSMELDING,
            VILKÅRSPRØVING, BEREGN_UTBETALING, TIL_GODKJENNING, TIL_UTBETALING)
    }

    @Test internal fun `ingen nav utbetaling kreves`() {
        håndterNySøknad(Triple(3.januar, 5.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 5.januar)))
        håndterSendtSøknad(Sykdom(3.januar, 5.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
        }
        håndterYtelser(0, emptyList())   // No history
        assertTrue(hendelselogger.hasErrors())
        println(hendelselogger)
        assertTilstander(0,
            START, MOTTATT_NY_SØKNAD, MOTTATT_INNTEKTSMELDING,
            VILKÅRSPRØVING, BEREGN_UTBETALING, TIL_INFOTRYGD)
    }

    @Test internal fun `To perioder med opphold`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0, emptyList())   // No history
        håndterManuelSaksbehandling(0, true)
        inspektør.also {
            assertNoErrors(it)
            assertNoWarnings(it)
            assertMessages(it)
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(2.januar))
            assertEquals(3, it.sykdomshistorikk.size)
            assertEquals(18, it.dagtelling[Sykedag::class])
            assertEquals(6, it.dagtelling[SykHelgedag::class])
        }
        assertTilstander(0,
            START, MOTTATT_NY_SØKNAD, MOTTATT_SENDT_SØKNAD,
            VILKÅRSPRØVING, BEREGN_UTBETALING, TIL_GODKJENNING, TIL_UTBETALING)
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
        assertTrue(inspektør.periodeLogger.hasMessages())
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

    private fun håndterVilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double) {
        assertTrue(observatør.ettersburteBehov(Inntektsberegning))
        assertTrue(observatør.ettersburteBehov(EgenAnsatt))
        assertFalse(observatør.ettersburteBehov(Sykepengehistorikk))
        assertFalse(observatør.ettersburteBehov(Foreldrepenger))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeIndex, INNTEKT))
        assertEndringTeller()
    }

    private fun håndterYtelser(vedtaksperiodeIndex: Int, utbetalinger: List<Triple<LocalDate, LocalDate, Int>>) {
        assertTrue(observatør.ettersburteBehov(Sykepengehistorikk))
        assertTrue(observatør.ettersburteBehov(Foreldrepenger))
        assertFalse(observatør.ettersburteBehov(GodkjenningFraSaksbehandler))
        person.håndter(ytelser(vedtaksperiodeIndex, utbetalinger))
        assertEndringTeller()
    }

    private fun håndterManuelSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean) {
        assertTrue(observatør.ettersburteBehov(GodkjenningFraSaksbehandler))
        person.håndter(manuellSaksbehandling(vedtaksperiodeIndex, utbetalingGodkjent))
        assertEndringTeller()
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): ModelNySøknad {
        hendelselogger = Aktivitetslogger()
        return ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            rapportertdato = rapportertdato,
            sykeperioder = listOf(*sykeperioder),
            aktivitetslogger = hendelselogger
        )
    }

    private fun sendtSøknad(vararg perioder: ModelSendtSøknad.Periode): ModelSendtSøknad {
        hendelselogger = Aktivitetslogger()
        return ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sendtNav = rapportertdato,
            perioder = listOf(*perioder),
            aktivitetslogger = hendelselogger
        )
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): ModelInntektsmelding {
        hendelselogger = Aktivitetslogger()
        return ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            mottattDato = rapportertdato,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            aktivitetslogger = hendelselogger
        )
    }

    private fun vilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double): ModelVilkårsgrunnlag {
        hendelselogger = Aktivitetslogger()
        return ModelVilkårsgrunnlag(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            rapportertDato = rapportertdato,
            inntektsmåneder = (1..12).map { ModelVilkårsgrunnlag.Måned(
                YearMonth.of(2017, it),
                listOf(ModelVilkårsgrunnlag.Inntekt(inntekt))) },
            erEgenAnsatt = false,
            aktivitetslogger = hendelselogger
        )
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ): ModelYtelser {
        hendelselogger = Aktivitetslogger()
        return ModelYtelser(
            hendelseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            sykepengehistorikk = ModelSykepengehistorikk(
                utbetalinger = utbetalinger.map {
                    ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                inntektshistorikk = emptyList(),
                aktivitetslogger = hendelselogger
            ),
            foreldrepenger = ModelForeldrepenger(foreldrepenger, svangerskapspenger, Aktivitetslogger()),
            rapportertdato = rapportertdato,
            aktivitetslogger = hendelselogger
        )
    }

    private fun manuellSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean): ModelManuellSaksbehandling {
        hendelselogger = Aktivitetslogger()
        return ModelManuellSaksbehandling(
            hendelseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = observatør.vedtaksperiodeIder(vedtaksperiodeIndex),
            saksbehandler = "Ola Nordmann",
            utbetalingGodkjent = utbetalingGodkjent,
            rapportertdato = rapportertdato,
            aktivitetslogger = hendelselogger
        )
    }

    private inner class TestObservatør: PersonObserver {
        internal var endreTeller = 0
        private val etterspurteBehov = mutableMapOf<String, Boolean>()
        private var periodeIndek = -1
        private val periodeIndekser = mutableMapOf<UUID, Int>()
        private val vedtaksperiodeIder = mutableMapOf<Int, String>()
        internal val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()

        internal fun ettersburteBehov(key: Behovstype) = etterspurteBehov.getOrDefault(key.name, false)

        internal fun vedtaksperiodeIder(indeks: Int) = vedtaksperiodeIder[indeks] ?: fail("Missing vedtaksperiodeId")

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            endreTeller += 1
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            val indeks = periodeIndekser.getOrPut(event.id, {
                periodeIndek++
                tilstander[periodeIndek] = mutableListOf(START)
                vedtaksperiodeIder[periodeIndek] = event.id.toString()
                periodeIndek
            })
            tilstander[indeks]?.add(event.gjeldendeTilstand) ?: fail("Missing collection initialization")
        }

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            behov.behovType().forEach { etterspurteBehov[it] = true }
        }

    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
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

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            this.arbeidsgiver = arbeidsgiver
        }

        override fun visitArbeidsgiverAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            arbeidsgiverLogger = aktivitetslogger
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            this.inntektshistorikk = inntekthistorikk
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
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
