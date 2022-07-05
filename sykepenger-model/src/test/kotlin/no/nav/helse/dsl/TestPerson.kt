package no.nav.helse.dsl

import java.time.format.DateTimeFormatter
import no.nav.helse.Fødselsnummer
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder

internal class TestPerson(
    private val observatør: PersonObserver,
    private val aktørId: String = AKTØRID,
    private val fødselsnummer: Fødselsnummer = UNG_PERSON_FNR_2018,
    alder: Alder = UNG_PERSON_FDATO_2018.alder,
    private val jurist: MaskinellJurist = MaskinellJurist()
) {
    internal companion object {
        private val fnrformatter = DateTimeFormatter.ofPattern("ddMMyy")
        internal val UNG_PERSON_FDATO_2018 = 12.februar(1992)
        internal val UNG_PERSON_FNR_2018: Fødselsnummer = "${UNG_PERSON_FDATO_2018.format(fnrformatter)}40045".somFødselsnummer()
        internal const val AKTØRID = "42"
    }

    private lateinit var forrigeHendelse: IAktivitetslogg

    private val person = Person(aktørId, fødselsnummer, alder, jurist).also {
        it.addObserver(observatør)
    }

    private val arbeidsgivere = mutableMapOf<String, TestArbeidsgiver>()

    internal fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = inspektør(person)

    internal fun arbeidsgiver(orgnummer: String, block: TestArbeidsgiver.() -> Any = { }) =
        arbeidsgivere.getOrPut(orgnummer) { TestArbeidsgiver(orgnummer) }(block)

    inner class TestArbeidsgiver(private val orgnummer: String) {
        private val fabrikk = Hendelsefabrikk(aktørId, fødselsnummer, orgnummer)

        internal val inspektør get() = TestArbeidsgiverInspektør(person, orgnummer)
        internal fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
            fabrikk.lagSykmelding(*sykmeldingsperiode).also { forrigeHendelse = it }.let { sykmelding ->
                person.håndter(sykmelding)
            }

        operator fun invoke(block: TestArbeidsgiver.() -> Any): TestArbeidsgiver {
            block(this)
            return this
        }
    }
}
