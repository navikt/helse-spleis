package no.nav.helse.person

import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.BeforeEach
import java.util.*

internal abstract class AbstractPersonTest {

    internal companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = inspektør(ORGNUMMER)
    protected val String.inspektør get() = inspektør(this)

    protected val Int.vedtaksperiode: IdInnhenter get() = { orgnummer -> this.vedtaksperiode(orgnummer) }

    @BeforeEach
    internal fun createTestPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018.somFødselsnummer())
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    private fun Int.vedtaksperiode(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)
    protected fun Int.utbetaling(orgnummer: String) = inspektør(orgnummer).utbetalingId(this - 1)
    protected fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    protected fun inspektør(orgnummer: String, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)
}

internal typealias IdInnhenter = (orgnummer: String) -> UUID
