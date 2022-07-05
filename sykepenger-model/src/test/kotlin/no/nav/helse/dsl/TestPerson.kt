package no.nav.helse.dsl

import java.time.format.DateTimeFormatter
import no.nav.helse.Fødselsnummer
import no.nav.helse.februar
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder

internal class TestPerson(
    private val observatør: PersonObserver,
    aktørId: String = AKTØRID,
    fødselsnummer: Fødselsnummer = UNG_PERSON_FNR_2018,
    alder: Alder = UNG_PERSON_FDATO_2018.alder,
    private val jurist: MaskinellJurist = MaskinellJurist()
) {
    internal companion object {
        private val fnrformatter = DateTimeFormatter.ofPattern("ddMMyy")
        internal val UNG_PERSON_FDATO_2018 = 12.februar(1992)
        internal val UNG_PERSON_FNR_2018: Fødselsnummer = "${UNG_PERSON_FDATO_2018.format(fnrformatter)}40045".somFødselsnummer()
        internal const val AKTØRID = "42"
    }

    private val person = Person(aktørId, fødselsnummer, alder, jurist).also {
        it.addObserver(observatør)
    }

    internal fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = inspektør(person)
}
