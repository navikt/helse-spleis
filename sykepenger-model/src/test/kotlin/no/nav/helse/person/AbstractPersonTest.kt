package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractPersonTest {

    internal companion object {
        val UNG_PERSON_FNR_2018: Fødselsnummer = "12029240045".somFødselsnummer()
        val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        const val AKTØRID = "42"
        val ORGNUMMER: String = "987654321"


        val a1: String = ORGNUMMER
        val a1Hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            fødselsdato = UNG_PERSON_FØDSELSDATO,
            organisasjonsnummer = ORGNUMMER
        )
        val a2: String = "654321987"
        val a3: String = "321987654"
        val a4: String = "456789123"
    }

    lateinit var person: Person
    lateinit var observatør: TestObservatør
    lateinit var jurist: MaskinellJurist
    val inspektør get() = inspektør(ORGNUMMER)

    val Int.vedtaksperiode: IdInnhenter get() = IdInnhenter { orgnummer -> this@vedtaksperiode.vedtaksperiode(orgnummer) }
    fun IdInnhenter.filter(orgnummer: String = ORGNUMMER) = AktivitetsloggFilter.vedtaksperiode(this, orgnummer)

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO)
    }

    protected fun createTestPerson(fødselsnummer: Fødselsnummer, fødseldato: LocalDate): Person {
        jurist = MaskinellJurist()
        person = Person(AKTØRID, fødselsnummer, fødseldato.alder, jurist)
        observatør = TestObservatør().also { person.addObserver(it) }
        return person
    }

    private fun Int.vedtaksperiode(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)
    fun Int.utbetaling(orgnummer: String) = inspektør(orgnummer).utbetalingId(this - 1)
    fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    fun inspektør(orgnummer: String, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)
}

internal fun AbstractPersonTest.nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()

internal fun interface IdInnhenter {
    fun id(orgnummer: String): UUID
}
