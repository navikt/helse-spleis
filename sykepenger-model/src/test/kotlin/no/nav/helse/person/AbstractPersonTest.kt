package no.nav.helse.person

import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.BeforeEach
import java.util.*

internal abstract class AbstractPersonTest {

    internal companion object {
        const val UNG_PERSON_FNR_2018 = "12029240045"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"

        const val a1 = ORGNUMMER
        const val a2 = "654321987"
        const val a3 = "321987654"
        const val a4 = "456789123"
    }

    lateinit var person: Person
    lateinit var observatør: TestObservatør
    val inspektør get() = inspektør(ORGNUMMER)
    val String.inspektør get() = inspektør(this)

    val Int.vedtaksperiode: IdInnhenter get() = { orgnummer -> this.vedtaksperiode(orgnummer) }

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018)
    }

    protected fun createTestPerson(fødselsnummer: String) {
        person = Person(AKTØRID, fødselsnummer.somFødselsnummer())
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    private fun Int.vedtaksperiode(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)
    fun Int.utbetaling(orgnummer: String) = inspektør(orgnummer).utbetalingId(this - 1)
    fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    fun inspektør(orgnummer: String, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)
}

internal typealias IdInnhenter = (orgnummer: String) -> UUID
