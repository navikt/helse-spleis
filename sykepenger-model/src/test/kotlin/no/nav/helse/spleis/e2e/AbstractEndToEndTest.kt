package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.UNG_PERSON_FØDSELSDATO
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør
import no.nav.helse.dsl.Varslersamler
import no.nav.helse.dsl.a1
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.februar
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.gjenopprettFraJSONtekst
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag

@Tag("e2e")
internal abstract class AbstractEndToEndTest {
    internal companion object {
        val a1Hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            organisasjonsnummer = a1,
                behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1)
        )

        private fun overgangFraInfotrygdPerson(regelverkslogg: Regelverkslogg) = gjenopprettFraJSON("/personer/infotrygdforlengelse.json", regelverkslogg)
        private fun pingPongPerson(regelverkslogg: Regelverkslogg) = gjenopprettFraJSON("/personer/pingpong.json", regelverkslogg)
        private fun toVedtakMedSammeFagsystemId(regelverkslogg: Regelverkslogg) = gjenopprettFraJSON("/personer/to_vedtak_samme_fagsystem_id.json", regelverkslogg)
        private fun treVedtakMedSammeFagsystemId(regelverkslogg: Regelverkslogg) = gjenopprettFraJSON("/personer/tre_vedtak_samme_fagsystem_id.json", regelverkslogg)
        private fun treVedtakMedSammeFagsystemIdOgAuuPåFørste(regelverkslogg: Regelverkslogg) = gjenopprettFraJSON("/personer/tre_vedtak_samme_fagsystem_id_forste_periode_AUU.json", regelverkslogg)
    }

    internal val assertetVarsler = Varslersamler.AssertetVarsler()
    lateinit var personlogg: Aktivitetslogg
        private set
    lateinit var person: Person
        private set
    lateinit var observatør: TestObservatør
        private set
    lateinit var ugyldigeSituasjoner: UgyldigeSituasjonerObservatør
        private set
    lateinit var regelverkslogg: SubsumsjonsListLog
        private set
    val inspektør get() = inspektør(a1)
    internal val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

    internal lateinit var forrigeHendelse: Hendelse
        private set
    internal lateinit var hendelselogg: Aktivitetslogg
        private set

    internal val inntektsmeldinger = mutableMapOf<UUID, InnsendtInntektsmelding>()

    val Int.vedtaksperiode: IdInnhenter get() = IdInnhenter { orgnummer -> this@vedtaksperiode.vedtaksperiodeId(orgnummer) }
    fun Int.vedtaksperiode(orgnummer: String): IdInnhenter = IdInnhenter { this@vedtaksperiode.vedtaksperiodeId(orgnummer) }
    fun IdInnhenter.filter(orgnummer: String = a1) = AktivitetsloggFilter.vedtaksperiode(this, orgnummer)
    private fun Int.vedtaksperiodeId(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)
    fun Int.utbetaling(orgnummer: String) = inspektør(orgnummer).utbetalingId(this - 1)
    fun inspektør(orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    fun inspektør(orgnummer: String, block: TestArbeidsgiverInspektør.() -> Unit) = inspektør(orgnummer).run(block)

    @BeforeEach
    internal fun createTestPerson() {
        createTestPerson(UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO)
    }

    @AfterEach
    fun alleVarslerAssertet() {
        val varslersamler = Varslersamler()
        varslersamler.registrerVarsler(personlogg.varsel)
        varslersamler.bekreftVarslerAssertet(assertetVarsler)
        ugyldigeSituasjoner.bekreftVarselHarKnytningTilVedtaksperiode(personlogg.varsel)
        ikkeBesvarteBehov.clear()
        inntektsmeldinger.clear()
    }

    private fun regler(maksSykedager: Int): ArbeidsgiverRegler = object : ArbeidsgiverRegler {
        override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) = oppholdsdagerBrukt >= 16
        override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) = arbeidsgiverperiodedagerBrukt >= 16
        override fun maksSykepengedager() = maksSykedager
        override fun maksSykepengedagerOver67() = maksSykedager
    }

    protected fun createKorttidsPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate, maksSykedager: Int) = createTestPerson { regelverkslogg ->
        Person(personidentifikator, fødseldato.alder, regelverkslogg, regler(maksSykedager))
    }

    protected fun createTestPerson(personidentifikator: Personidentifikator, fødseldato: LocalDate, dødsdato: LocalDate? = null) = createTestPerson { regelverkslogg ->
        Person(personidentifikator, Alder(fødseldato, dødsdato), regelverkslogg)
    }

    protected fun createPersonMedToVedtakPåSammeFagsystemId() = createTestPerson { regelverkslogg -> toVedtakMedSammeFagsystemId(regelverkslogg) }
    protected fun createPersonMedTreVedtakPåSammeFagsystemId() = createTestPerson { regelverkslogg -> treVedtakMedSammeFagsystemId(regelverkslogg) }
    protected fun createPersonMedTreVedtakPåSammeFagsystemIdOgAuuPåFørste() = createTestPerson { regelverkslogg -> treVedtakMedSammeFagsystemIdOgAuuPåFørste(regelverkslogg) }

    protected fun createPingPongPerson() = createTestPerson { regelverkslogg -> pingPongPerson(regelverkslogg) }.also {
        Utbetalingshistorikk(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
            vedtaksperiodeId = UUID.randomUUID(),
            element = InfotrygdhistorikkElement.opprett(
                LocalDateTime.now(),
                MeldingsreferanseId(UUID.randomUUID()),
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar))
            ),
            besvart = LocalDateTime.now()
        ).håndter(Person::håndterUtbetalingshistorikk)
    }
    protected fun createOvergangFraInfotrygdPerson() = createTestPerson { regelverkslogg -> overgangFraInfotrygdPerson(regelverkslogg) }.also {
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            element = InfotrygdhistorikkElement.opprett(
                LocalDateTime.now(),
                MeldingsreferanseId(UUID.randomUUID()),
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            ),
            besvart = LocalDateTime.now()
        ).håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)
    }

    protected fun createTestPerson(block: (regelverkslogg: Regelverkslogg) -> Person): Person {
        regelverkslogg = SubsumsjonsListLog()
        person = block(regelverkslogg)
        observatør = TestObservatør(person)
        personlogg = Aktivitetslogg()
        ugyldigeSituasjoner = UgyldigeSituasjonerObservatør(person)
        return person
    }

    internal fun <T : Hendelse> T.håndter(håndter: Person.(T, IAktivitetslogg) -> Unit): T {
        hendelselogg = Aktivitetslogg(personlogg)
        forrigeHendelse = this
        person.håndter(this, hendelselogg)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(hendelselogg.behov)
        return this
    }

    internal fun reserialiser() {
        createTestPerson {
            gjenopprettFraJSONtekst(person.dto().tilPersonData().tilSerialisertPerson().json)
        }
    }

    internal fun TestArbeidsgiverInspektør.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
        assertTilstander(
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            tilstander = tilstander,
                orgnummer = yrkesaktivitet.organisasjonsnummer(),
            inspektør = this
        )
    }

    inner class Hendelser(private val hendelser: () -> Unit) {
        infix fun førerTil(postCondition: TilstandType) = førerTil(listOf(postCondition))
        infix fun førerTil(postCondition: List<TilstandType>): Hendelser {
            hendelser()
            postCondition.forEachIndexed { index, tilstand ->
                assertTilstand((index + 1).vedtaksperiode, tilstand)
            }
            return this
        }

        infix fun somEtterfulgtAv(f: () -> Unit) = Hendelser(f)
    }

    fun hendelsene(f: () -> Unit) = Hendelser(f)

    data class InnsendtInntektsmelding(
        val tidspunkt: LocalDateTime,
        val generator: () -> Inntektsmelding,
        val inntektsmeldingkontrakt: no.nav.inntektsmeldingkontrakt.Inntektsmelding
    )
}

internal fun AbstractEndToEndTest.nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()

internal fun interface IdInnhenter {
    fun id(orgnummer: String): UUID
}
