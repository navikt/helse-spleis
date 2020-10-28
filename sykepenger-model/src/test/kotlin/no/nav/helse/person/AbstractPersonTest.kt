package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*

internal abstract class AbstractPersonTest {

    protected companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = inspektør(ORGNUMMER)

    protected val Int.vedtaksperiode get() = this.vedtaksperiode(ORGNUMMER)

    @BeforeEach
    internal fun createTestPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    protected fun Int.vedtaksperiode(orgnummer: String) = vedtaksperiodeId(this - 1, orgnummer)
    protected fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    protected fun vedtaksperiodeId(indeks: Int, orgnummer: String = ORGNUMMER) = observatør.vedtaksperiode(orgnummer, indeks)
}
