package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.readResource
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractPersonTest {

    internal companion object {
        val UNG_PERSON_FNR_2018: Personidentifikator = "12029240045".somPersonidentifikator()
        val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        const val AKTØRID = "42"
        val ORGNUMMER: String = "987654321"

        val a1: String = ORGNUMMER
        val a1Hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            aktørId = AKTØRID,
            personidentifikator = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER
        )
        val a2: String = "654321987"
        val a3: String = "321987654"
        val a4: String = "456789123"

        private fun overgangFraInfotrygdPerson(jurist: MaskinellJurist) = SerialisertPerson("/personer/infotrygdforlengelse.json".readResource()).deserialize(
            jurist
        ).also { person ->
            person.håndter(
                Inntektsmelding(
                    meldingsreferanseId = UUID.randomUUID(),
                    refusjon = Inntektsmelding.Refusjon(TestPerson.INNTEKT, null),
                    orgnummer = ORGNUMMER,
                    fødselsnummer = "",
                    aktørId = "",
                    førsteFraværsdag = null,
                    beregnetInntekt = TestPerson.INNTEKT,
                    arbeidsgiverperioder = listOf(1.januar til 16.januar),
                    arbeidsforholdId = null,
                    begrunnelseForReduksjonEllerIkkeUtbetalt = null,
                    harFlereInntektsmeldinger = false,
                    mottatt = LocalDateTime.now()
                )
            )
            person.håndter(
                Utbetalingshistorikk(
                    UUID.randomUUID(), "", "", ORGNUMMER, UUID.randomUUID().toString(),
                    InfotrygdhistorikkElement.opprett(
                        LocalDateTime.now(),
                        UUID.randomUUID(),
                        listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, TestPerson.INNTEKT)),
                        listOf(Inntektsopplysning(ORGNUMMER, 1.januar, TestPerson.INNTEKT, true)),
                        emptyMap(),
                        emptyList(),
                        false
                    ),
                ),
            )
        }
        private fun pingPongPerson(jurist: MaskinellJurist) = SerialisertPerson("/personer/pingpong.json".readResource()).deserialize(
            jurist
        ).also { person ->
            person.håndter(
                Utbetalingshistorikk(
                    UUID.randomUUID(), "", "", ORGNUMMER, UUID.randomUUID().toString(),
                    InfotrygdhistorikkElement.opprett(
                        LocalDateTime.now(),
                        UUID.randomUUID(),
                        listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, TestPerson.INNTEKT)),
                        listOf(Inntektsopplysning(ORGNUMMER, 1.februar, TestPerson.INNTEKT, true)),
                        emptyMap(),
                        emptyList(),
                        false
                    ),
                ),
            )
        }
    }

    lateinit var person: Person
    lateinit var observatør: TestObservatør
    lateinit var ugyldigeSituasjonerObservatør: UgyldigeSituasjonerObservatør
    lateinit var jurist: MaskinellJurist
    val inspektør get() = inspektør(ORGNUMMER)

    val Int.vedtaksperiode: IdInnhenter get() = IdInnhenter { orgnummer -> this@vedtaksperiode.vedtaksperiode(orgnummer) }
    fun IdInnhenter.filter(orgnummer: String = ORGNUMMER) = AktivitetsloggFilter.vedtaksperiode(this, orgnummer)

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO)
    }

    @AfterEach
    fun verify() {
        ugyldigeSituasjonerObservatør.bekreftIngenUgyldigeSituasjoner()
    }

    private fun regler(maksSykedager: Int): ArbeidsgiverRegler = object: ArbeidsgiverRegler {
        override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) = oppholdsdagerBrukt >= 16
        override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) = arbeidsgiverperiodedagerBrukt >= 16
        override fun dekningsgrad() = 1.0
        override fun maksSykepengedager() = maksSykedager
        override fun maksSykepengedagerOver67() = maksSykedager
    }
    protected fun createKorttidsPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate, maksSykedager: Int) = createTestPerson { jurist ->
        Person(AKTØRID, personidentifikator, fødseldato.alder, jurist, regler(maksSykedager))
    }
    protected fun createTestPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate) = createTestPerson { jurist ->
        Person(AKTØRID, personidentifikator, fødseldato.alder, jurist)
    }
    protected fun createPingPongPerson() = createTestPerson { jurist -> pingPongPerson(jurist) }
    protected fun createOvergangFraInfotrygdPerson() = createTestPerson { jurist -> overgangFraInfotrygdPerson(jurist) }

    protected fun createTestPerson(block: (jurist: MaskinellJurist) -> Person) : Person {
        jurist = MaskinellJurist()
        person = block(jurist)
        observatør = TestObservatør(person)
        ugyldigeSituasjonerObservatør = UgyldigeSituasjonerObservatør(person)
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
