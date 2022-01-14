package no.nav.helse.person

import no.nav.helse.Fødselsnummer
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.somFødselsnummer
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.BeforeEach
import java.util.*

internal abstract class AbstractPersonTest {

    internal companion object {
        val UNG_PERSON_FNR_2018: Fødselsnummer = "12029240045".somFødselsnummer()
        const val AKTØRID = "42"
        val ORGNUMMER: Organisasjonsnummer = "987654321".somOrganisasjonsnummer()

        val a1: Organisasjonsnummer = ORGNUMMER
        val a2: Organisasjonsnummer = "654321987".somOrganisasjonsnummer()
        val a3: Organisasjonsnummer = "321987654".somOrganisasjonsnummer()
        val a4: Organisasjonsnummer = "456789123".somOrganisasjonsnummer()
    }

    lateinit var person: Person
    lateinit var observatør: TestObservatør
    val inspektør get() = inspektør(ORGNUMMER)
    val Organisasjonsnummer.inspektør get() = inspektør(this)

    val Int.vedtaksperiode: IdInnhenter get() = IdInnhenter { orgnummer -> this@vedtaksperiode.vedtaksperiode(orgnummer) }

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018)
    }

    protected fun createTestPerson(fødselsnummer: Fødselsnummer) {
        person = Person(AKTØRID, fødselsnummer)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    private fun Int.vedtaksperiode(orgnummer: Organisasjonsnummer) = observatør.vedtaksperiode(orgnummer, this - 1)
    fun Int.utbetaling(orgnummer: Organisasjonsnummer) = inspektør(orgnummer).utbetalingId(this - 1)
    fun inspektør(orgnummer: Organisasjonsnummer) = TestArbeidsgiverInspektør(person, orgnummer)
    fun inspektør(orgnummer: Organisasjonsnummer, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)
}

internal fun interface IdInnhenter {
    fun id(orgnummer: Organisasjonsnummer): UUID
}
