package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TestPersonTest {
    private companion object {
        private val a1 = "a1"
        private val a2 = "a2"
        private val INNTEKT = 31000.00.månedlig
        private val personInspektør = { person: Person -> PersonInspektør(person) }
        private val agInspektør = { orgnummer: String -> { person: Person -> TestArbeidsgiverInspektør(person, orgnummer) } }
    }
    private lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson

    private val Int.vedtaksperiode get() = testperson.arbeidsgiver(a1) { vedtaksperiode }

    private val String.inspektør get() = inspektør(this)

    private fun inspektør(orgnummer: String) = testperson.inspiser(agInspektør(orgnummer))

    private operator fun String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> Any) =
        testperson.arbeidsgiver(this, testblokk)

    private fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
        a1 { håndterSykmelding(*sykmeldingsperiode) }

    private fun håndterSøknad(vararg perioder: Søknad.Søknadsperiode) =
        a1 { håndterSøknad(*perioder) }

    private fun håndterInntektsmelding(arbeidsgiverperioder: List<Periode>, inntekt: Inntekt = INNTEKT) =
        a1 { håndterInntektsmelding(arbeidsgiverperioder, inntekt) }

    private fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        a1 { assertTilstander(id, *tilstander) }
    }

    @BeforeEach
    fun setup() {
        observatør = TestObservatør()
        testperson = TestPerson(observatør)
    }

    @Test
    fun `oppretter standardperson`() {
        val inspektør = testperson.inspiser(personInspektør)
        assertEquals(TestPerson.UNG_PERSON_FNR_2018, inspektør.fødselsnummer)
        assertEquals(TestPerson.UNG_PERSON_FDATO_2018, inspektør.fødselsdato)
        assertEquals(TestPerson.AKTØRID, inspektør.aktørId)
        assertNull(inspektør.dødsdato)
    }

    @Test
    fun `kan sende sykmelding til arbeidsgiver`() {
        testperson.arbeidsgiver(a1).håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        assertEquals(1, inspektør(a1).sykmeldingsperioder().size)
    }

    @Test
    fun `kan teste utenfor arbeidsgiver-kontekst`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), INNTEKT)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `kan sende sykmelding via testblokk`() {
        testperson.arbeidsgiver(a1) {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        }
        assertEquals(1, a1.inspektør.sykmeldingsperioder().size)
    }

    @Test
    fun `kan sende inntektsmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
            håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), INNTEKT)

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK
            )
        }
    }

    internal fun TestPerson.TestArbeidsgiver.assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertFalse(inspektør.periodeErForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}:\n${testperson.inspiser(personInspektør).aktivitetslogg}" }
        assertTrue(inspektør.periodeErIkkeForkastet(id)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}\n${testperson.inspiser(personInspektør).aktivitetslogg}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id])
    }
}