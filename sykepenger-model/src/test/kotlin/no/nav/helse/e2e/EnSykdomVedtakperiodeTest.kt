package no.nav.helse.e2e

import no.nav.helse.behov.Behov
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelNySøknadTest
import no.nav.helse.hendelser.ModelSendtSøknad.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Sykdom
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
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
    }

    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private lateinit var vedtaksperiodeId: String
    private var forventetEndringTeller = 0

    @BeforeEach internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also {person.addObserver(it)}
    }

    @Test internal fun `ingen historie`() {
        håndterNySøknad(Triple(1.januar, 26.januar, 100))
        håndterSendtSøknad(Sykdom(1.januar, 26.januar, 100))
        håndterInntektsmelding(listOf(1.januar..26.januar))
        håndterVilkårsgrunnlag()
        håndterYtelser()
        håndterManuelSaksbehandling()
        println(aktivitetslogger)
    }

    private fun håndterNySøknad(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(nySøknad(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterSendtSøknad(vararg perioder: Periode) {
        person.håndter(sendtSøknad(*perioder))
        assertEndringTeller()
    }

    private fun håndterInntektsmelding(arbeidsgiverperioder: List<ClosedRange<LocalDate>>) {
        person.håndter(inntektsmelding(arbeidsgiverperioder))
        assertEndringTeller()
    }

    private fun håndterVilkårsgrunnlag() {
        person.håndter(vilkårsgrunnlag(INNTEKT))
        assertEndringTeller()
    }

    private fun håndterYtelser() {
    }

    private fun håndterManuelSaksbehandling() {
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
            rapportertdato = LocalDateTime.now(),
            sykeperioder = listOf(*sykeperioder),
            originalJson = "{}",
            aktivitetslogger = aktivitetslogger
        )

    private fun sendtSøknad(vararg perioder: Periode) = ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            rapportertdato = 31.januar.atStartOfDay(),
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
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) = ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            mottattDato = 1.februar.atStartOfDay(),
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
        rapportertDato = LocalDateTime.now(),
        inntektsmåneder = (1..12).map { ModelVilkårsgrunnlag.Måned(
            YearMonth.of(2017, it),
            listOf(ModelVilkårsgrunnlag.Inntekt(inntekt))) },
        erEgenAnsatt = false,
        aktivitetslogger = aktivitetslogger,
        originalJson = "{}"
    )

    private inner class TestObservatør: PersonObserver {
        internal var endreTeller = 0

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            endreTeller += 1
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            vedtaksperiodeId = event.vedtaksperiodeId()
        }

    }
}
