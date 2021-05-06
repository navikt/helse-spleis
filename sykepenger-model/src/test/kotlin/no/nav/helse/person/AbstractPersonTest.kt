package no.nav.helse.person

import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractPersonTest {

    internal companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = inspektør(ORGNUMMER)

    protected val Int.vedtaksperiode get() = this.vedtaksperiode(ORGNUMMER)
    protected val Int.utbetaling get() = this.utbetaling(ORGNUMMER)

    @BeforeEach
    internal fun createTestPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    protected fun Int.vedtaksperiode(orgnummer: String) = vedtaksperiodeId(this - 1, orgnummer)
    protected fun Int.utbetaling(orgnummer: String) = utbetalingId(this - 1, orgnummer)
    protected fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    protected fun vedtaksperiodeId(indeks: Int, orgnummer: String = ORGNUMMER) = observatør.vedtaksperiode(orgnummer, indeks)
    protected fun utbetalingId(indeks: Int, orgnummer: String = ORGNUMMER) = inspektør(orgnummer).utbetalingId(indeks)
}
