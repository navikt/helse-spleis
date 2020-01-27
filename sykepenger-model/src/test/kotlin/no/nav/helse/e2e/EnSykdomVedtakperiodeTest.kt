package no.nav.helse.e2e

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.fixtures.desember
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelNySøknadTest
import no.nav.helse.hendelser.ModelSendtSøknad.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Sykdom
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class EnSykdomVedtakperiodeTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 1000.00
        private val rapportertdato = 1.februar.atStartOfDay()
    }

    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var vedtaksperiodeId: String
    private var forventetEndringTeller = 0

    @BeforeEach internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also {person.addObserver(it)}
    }

    @Test internal fun `ingen historie med SendtSøknad først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterSendtSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(3.januar..26.januar))
        håndterVilkårsgrunnlag(INNTEKT)
        håndterYtelser(emptyList())   // No history
        håndterManuelSaksbehandling()
        assertFalse(aktivitetslogger.hasErrors())
        assertFalse(aktivitetslogger.hasWarnings())
        assertTrue(aktivitetslogger.hasMessages())
        assertEquals(6, observatør.tilstander[0]?.size)
    }

    @Test internal fun `ingen historie med Inntektsmelding først`() {
        håndterNySøknad(Triple(3.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(3.januar..26.januar))
        håndterSendtSøknad(Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(INNTEKT)
        håndterYtelser(emptyList())   // No history
        håndterManuelSaksbehandling()
        assertFalse(aktivitetslogger.hasErrors())
        assertFalse(aktivitetslogger.hasWarnings())
        assertTrue(aktivitetslogger.hasMessages())
        assertEquals(6, observatør.tilstander[0]?.size)
    }

    private fun håndterNySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(nySøknad(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterSendtSøknad(vararg perioder: Periode) {
        assertFalse(observatør.ettersburteBehov(Inntektsberegning))
        assertFalse(observatør.ettersburteBehov(EgenAnsatt))
        person.håndter(sendtSøknad(*perioder))
        assertEndringTeller()
    }

    private fun håndterInntektsmelding(arbeidsgiverperioder: List<ClosedRange<LocalDate>>) {
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

    private fun håndterManuelSaksbehandling() {
        assertTrue(observatør.ettersburteBehov(GodkjenningFraSaksbehandler))
    }

    private fun assertEndringTeller() {
        forventetEndringTeller += 1
        assertEquals(forventetEndringTeller, observatør.endreTeller)
    }

    private fun nySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) = ModelNySøknad(
            hendelseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            rapportertdato = rapportertdato,
            sykeperioder = listOf(*sykeperioder),
            originalJson = "{}",
            aktivitetslogger = aktivitetslogger
        )

    private fun sendtSøknad(vararg perioder: Periode) = ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            rapportertdato = rapportertdato,
            perioder = listOf(*perioder),
            originalJson = "{}",
            aktivitetslogger = aktivitetslogger
        )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<ClosedRange<LocalDate>>,
        ferieperioder: List<ClosedRange<LocalDate>> = emptyList(),
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
            aktivitetslogger = aktivitetslogger
        )

    private fun vilkårsgrunnlag(inntekt: Double) = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        rapportertDato = rapportertdato,
        inntektsmåneder = (1..12).map { ModelVilkårsgrunnlag.Måned(
            YearMonth.of(2017, it),
            listOf(ModelVilkårsgrunnlag.Inntekt(inntekt))) },
        erEgenAnsatt = false,
        aktivitetslogger = aktivitetslogger,
        originalJson = "{}"
    )

    private fun ytelser(
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        foreldrepenger: Pair<LocalDate, LocalDate>? = null,
        svangerskapspenger: Pair<LocalDate, LocalDate>? = null
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
            aktivitetslogger = aktivitetslogger
        ),
        foreldrepenger = ModelForeldrepenger(foreldrepenger, svangerskapspenger, aktivitetslogger),
        rapportertdato = rapportertdato,
        originalJson = "{}",
        aktivitetslogger = aktivitetslogger
    )

    private inner class TestObservatør: PersonObserver {
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
                tilstander[periodeIndek] = mutableListOf(TilstandType.START)
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

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = mutableListOf()
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks]?.add(tilstand.type) ?: fail("Missing collection initialization")
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks] ?: fail("Missing collection initialization")
    }
}
