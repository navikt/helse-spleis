package no.nav.helse.dsl

import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.februar
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import no.nav.helse.økonomi.Inntekt

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

        internal operator fun String.invoke(testPerson: TestPerson, testblokk: TestArbeidsgiver.() -> Any = { }) =
            testPerson.arbeidsgiver(this).invoke(testblokk)
    }

    private lateinit var forrigeHendelse: IAktivitetslogg

    private val vedtaksperiodesamler = Vedtaksperiodesamler()
    private val person = Person(aktørId, fødselsnummer, alder, jurist).also {
        it.addObserver(vedtaksperiodesamler)
        it.addObserver(observatør)
    }

    private val arbeidsgivere = mutableMapOf<String, TestArbeidsgiver>()

    internal fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = inspektør(person)

    internal fun arbeidsgiver(orgnummer: String, block: TestArbeidsgiver.() -> Any = { }) =
        arbeidsgivere.getOrPut(orgnummer) { TestArbeidsgiver(orgnummer) }(block)

    private fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        forrigeHendelse = this
        person.håndter(this)
        return this
    }

    private inner class Vedtaksperiodesamler : PersonObserver {
        private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()

        internal fun vedtaksperiodeId(orgnummer: String, indeks: Int) =
            vedtaksperioder.getValue(orgnummer).elementAt(indeks)

        override fun vedtaksperiodeEndret(
            hendelseskontekst: Hendelseskontekst,
            event: PersonObserver.VedtaksperiodeEndretEvent
        ) {
            val detaljer = mutableMapOf<String, String>().apply { hendelseskontekst.appendTo(this::put) }
            val orgnr = detaljer.getValue("organisasjonsnummer")
            val vedtaksperiodeId = UUID.fromString(detaljer.getValue("vedtaksperiodeId"))
            vedtaksperioder.getOrPut(orgnr) { mutableSetOf() }.add(vedtaksperiodeId)
        }
    }

    inner class TestArbeidsgiver(private val orgnummer: String) {
        private val fabrikk = Hendelsefabrikk(aktørId, fødselsnummer, orgnummer)

        internal val inspektør get() = TestArbeidsgiverInspektør(person, orgnummer)

        internal val Int.vedtaksperiode get() = vedtaksperiodesamler.vedtaksperiodeId(orgnummer, this - 1)

        internal fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
            fabrikk.lagSykmelding(*sykmeldingsperiode).håndter(Person::håndter)

        internal fun håndterSøknad(vararg perioder: Søknad.Søknadsperiode) =
            fabrikk.lagSøknad(*perioder).håndter(Person::håndter)

        internal fun håndterInntektsmelding(arbeidsgiverperioder: List<Periode>, inntekt: Inntekt) =
            fabrikk.lagInntektsmelding(arbeidsgiverperioder, inntekt).håndter(Person::håndter)

        operator fun invoke(testblokk: TestArbeidsgiver.() -> Any): TestArbeidsgiver {
            testblokk(this)
            return this
        }
    }
}