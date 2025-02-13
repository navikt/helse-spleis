package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.UNG_PERSON_FØDSELSDATO
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør
import no.nav.helse.dsl.Varslersamler
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.februar
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.gjenopprettFraJSONtekst
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractPersonTest {

    internal companion object {
        val a1Hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            organisasjonsnummer = a1
        )

        private fun overgangFraInfotrygdPerson(jurist: Subsumsjonslogg) = gjenopprettFraJSON("/personer/infotrygdforlengelse.json", jurist).also { person ->
            person.håndter(
                Utbetalingshistorikk(
                    meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                    organisasjonsnummer = a1,
                    vedtaksperiodeId = UUID.randomUUID().toString(),
                    element = InfotrygdhistorikkElement.opprett(
                        LocalDateTime.now(),
                        MeldingsreferanseId(UUID.randomUUID()),
                        listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT)),
                        listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true)),
                        emptyMap()
                    ),
                    besvart = LocalDateTime.now()
                ),
                Aktivitetslogg()
            )
        }

        private fun pingPongPerson(jurist: Subsumsjonslogg) = gjenopprettFraJSON("/personer/pingpong.json", jurist).also { person ->
            person.håndter(
                Utbetalingshistorikk(
                    meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                    organisasjonsnummer = a1,
                    vedtaksperiodeId = UUID.randomUUID().toString(),
                    element = InfotrygdhistorikkElement.opprett(
                        LocalDateTime.now(),
                        MeldingsreferanseId(UUID.randomUUID()),
                        listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, INNTEKT)),
                        listOf(Inntektsopplysning(a1, 1.februar, INNTEKT, true)),
                        emptyMap()
                    ),
                    besvart = LocalDateTime.now()
                ),
                Aktivitetslogg()
            )
        }
    }

    internal val assertetVarsler = Varslersamler.AssertetVarsler()
    lateinit var person: Person
    lateinit var observatør: TestObservatør
    lateinit var jurist: SubsumsjonsListLog
    val inspektør get() = inspektør(a1)

    val Int.vedtaksperiode: IdInnhenter get() = IdInnhenter { orgnummer -> this@vedtaksperiode.vedtaksperiodeId(orgnummer) }
    fun Int.vedtaksperiode(orgnummer: String): IdInnhenter = IdInnhenter { this@vedtaksperiode.vedtaksperiodeId(orgnummer) }
    fun IdInnhenter.filter(orgnummer: String = a1) = AktivitetsloggFilter.vedtaksperiode(this, orgnummer)

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO)
    }

    @AfterEach
    fun alleVarslerAssertet() {
        val varslersamler = Varslersamler()
        varslersamler.registrerVarsler(person.personLogg.varsel)
        varslersamler.bekreftVarslerAssertet(assertetVarsler)
    }

    private fun regler(maksSykedager: Int): ArbeidsgiverRegler = object : ArbeidsgiverRegler {
        override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) = oppholdsdagerBrukt >= 16
        override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) = arbeidsgiverperiodedagerBrukt >= 16
        override fun dekningsgrad() = 1.0
        override fun maksSykepengedager() = maksSykedager
        override fun maksSykepengedagerOver67() = maksSykedager
    }

    protected fun createKorttidsPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate, maksSykedager: Int) = createTestPerson { jurist ->
        Person(personidentifikator, fødseldato.alder, jurist, regler(maksSykedager))
    }

    protected fun createTestPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate, dødsdato: LocalDate? = null) = createTestPerson { jurist ->
        Person(personidentifikator, Alder(fødseldato, dødsdato), jurist)
    }

    protected fun createPingPongPerson() = createTestPerson { jurist -> pingPongPerson(jurist) }
    protected fun createOvergangFraInfotrygdPerson() = createTestPerson { jurist -> overgangFraInfotrygdPerson(jurist) }

    protected fun createTestPerson(block: (jurist: Subsumsjonslogg) -> Person): Person {
        jurist = SubsumsjonsListLog()
        person = block(jurist)
        observatør = TestObservatør(person)
        UgyldigeSituasjonerObservatør(person)
        return person
    }

    internal fun reserialiser() {
        createTestPerson {
            gjenopprettFraJSONtekst(person.dto().tilPersonData().tilSerialisertPerson().json)
        }
    }

    private fun Int.vedtaksperiodeId(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)
    fun Int.utbetaling(orgnummer: String) = inspektør(orgnummer).utbetalingId(this - 1)
    fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    fun inspektør(orgnummer: String, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)
}

internal fun AbstractPersonTest.nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()

internal fun interface IdInnhenter {
    fun id(orgnummer: String): UUID
}
