package no.nav.helse.person

import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set

internal class PersonTest {

    private val aktørId = "id"
    private val fødselsnummer = "01017000000"
    private val organisasjonsnummer = "12"

    private val virksomhetsnummer_a = "234567890"
    private val virksomhetsnummer_b = "098765432"

    private lateinit var testObserver: TestPersonObserver
    private lateinit var testPerson: Person
    private val inspektør get() = TestPersonInspektør(testPerson)

    @BeforeEach
    internal fun setup() {
        testObserver = TestPersonObserver()
        testPerson = Person(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
            it.addObserver(this.testObserver)
        }
    }

    @Test
    fun `flere arbeidsgivere`() {
        enPersonMedÉnArbeidsgiver(virksomhetsnummer_a).also {
            it.håndter(nySøknad(orgnummer = virksomhetsnummer_b))
        }
        assertTrue(inspektør.personLogger.hasErrorsOld())
        assertAntallPersonerEndret(1)
        assertAlleVedtaksperiodetilstander(TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny søknad fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        testPerson.also {
            it.håndter(nySøknad())
        }
        assertTrue(inspektør.personLogger.toString().contains("Ny arbeidsgiver"))
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD)
    }

    @Test
    internal fun `påminnelse blir delegert til perioden`() {
        testPerson.also {
            it.håndter(nySøknad())
            it.håndter(påminnelse(tilstandType = MOTTATT_NY_SØKNAD))
        }

        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(TIL_INFOTRYGD)
        assertFalse(testObserver.vedtaksperiodeIkkeFunnet)
    }

    @Test
    internal fun `påminnelse for periode som ikke finnes`() {
        val påminnelse = påminnelse(
            vedtaksperiodeId = UUID.randomUUID(),
            tilstandType = MOTTATT_NY_SØKNAD
        )
        testPerson.also { it.håndter(påminnelse) }

        assertPersonIkkeEndret()
        assertVedtaksperiodeIkkeEndret()
        assertTrue(testObserver.vedtaksperiodeIkkeFunnet)
        assertEquals(
            påminnelse.vedtaksperiodeId,
            testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.vedtaksperiodeId.toString()
        )
        assertEquals(påminnelse.aktørId(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.aktørId)
        assertEquals(
            påminnelse.organisasjonsnummer(),
            testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.organisasjonsnummer
        )
        assertEquals(påminnelse.fødselsnummer(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.fødselsnummer)
    }

    @Test
    internal fun `inntektsmelding med eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(nySøknad())
            it.håndter(inntektsmelding())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD)
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende perioden`() {
        testPerson.håndter(nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100))))
        nySøknad(perioder = listOf(Triple(10.juli, 22.juli, 100))).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrorsOld())
        }
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndter(nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100))))
            it.håndter(
                sendtSøknad(
                    perioder = listOf(
                        SendtSøknad.Periode.Sykdom(
                            fom = 1.juli,
                            tom = 20.juli,
                            grad = 100
                        )
                    )
                )
            )
        }
        sendtSøknad(
            perioder = listOf(
                SendtSøknad.Periode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    grad = 100
                )
            )
        ).also {
            testPerson.håndter(it)
            it.hasErrorsOld()
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(UNDERSØKER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny periode må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testPerson.håndter(nySøknad(perioder = listOf(Triple(1.juli, 9.juli, 100))))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_NY_SØKNAD, inspektør.tilstand(0))
        assertTrue(inspektør.personLogger.hasMessagesOld())
        assertFalse(inspektør.personLogger.hasErrorsOld())
        sendtSøknad(
            perioder = listOf(
                SendtSøknad.Periode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    grad = 100
                )
            )
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrorsOld())
        }
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.tilstand(0))
        assertTrue(inspektør.personLogger.hasErrorsOld())

        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, TIL_INFOTRYGD) // Invalidation of first period
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre inntektsmeldingen`() {
        testPerson.håndter(nySøknad(orgnummer = "12", perioder = listOf(Triple(1.juli, 9.juli, 100))))
            testPerson.håndter(
                inntektsmelding(
                    virksomhetsnummer = "12",
                    førsteFraværsdag = 1.juli,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))
                )
            )
        inntektsmelding(
            virksomhetsnummer = "12",
            førsteFraværsdag = 1.juli,
            arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasWarningsOld())
            assertFalse(it.hasErrorsOld())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, AVVENTER_SENDT_SØKNAD)
    }


    @Test
    internal fun `ny søknad med periode som ikke er 100 %`() {
        nySøknad(
            perioder = listOf(
                Triple(Uke(1).mandag, Uke(1).torsdag, 60),
                Triple(Uke(1).fredag, Uke(1).fredag, 100)
            )
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrorsOld())
        }
    }

    @Test
    internal fun `sendt søknad kan ikke være sendt mer enn 3 måneder etter perioden`() {
        sendtSøknad(
            perioder = listOf(
                SendtSøknad.Periode.Sykdom(fom = Uke(1).mandag, tom = Uke(1).torsdag, grad = 100)
            ),
            sendtNav = Uke(1).mandag.plusMonths(4).atStartOfDay()
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrorsOld())
        }
    }

    @Test
    internal fun `sendt søknad med periode som ikke er 100 % kaster exception`() {
        sendtSøknad(
            perioder = listOf(
                SendtSøknad.Periode.Sykdom(fom = Uke(1).mandag, tom = Uke(1).torsdag, grad = 100),
                SendtSøknad.Periode.Sykdom(
                    fom = Uke(1).fredag,
                    tom = Uke(1).fredag,
                    grad = 100,
                    faktiskGrad = 90.0
                )
            )
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrorsOld())
        }
    }

    @Test
    internal fun `sendt søknad trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(nySøknad())
            it.håndter(sendtSøknad())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(UNDERSØKER_HISTORIKK)
    }

    @Test
    internal fun `ytelser lager ikke ny periode, selv om det ikke finnes noen fra før`() {
        testPerson.also {
            it.håndter(Ytelser(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID().toString(),
                utbetalingshistorikk = Utbetalingshistorikk(emptyList(), emptyList(), emptyList(), Aktivitetslogger(), Aktivitetslogg()),
                foreldrepermisjon = Foreldrepermisjon(null, null, Aktivitetslogger(), Aktivitetslogg()),
                aktivitetslogger = Aktivitetslogger(),
                aktivitetslogg = Aktivitetslogg()
            ))
        }

        assertPersonIkkeEndret()
        assertVedtaksperiodeIkkeEndret()
    }

    private fun vedtaksperiodeIdForPerson() =
        testObserver.tilstandsendringer.keys.first()

    private fun enPersonMedÉnArbeidsgiver(virksomhetsnummer: String) = testPerson.also {
        it.håndter(nySøknad(orgnummer = virksomhetsnummer))
    }

    private fun assertAntallPersonerEndret(antall: Int) {
        assertEquals(antall, this.testObserver.tilstandsendringer.size)
    }

    private fun assertVedtaksperiodetilstand(tilstandType: TilstandType) {
        assertEquals(tilstandType, this.testObserver.gjeldendeVedtaksperiodetilstand)
    }

    private fun assertVedtaksperiodetilstand(
        forrigeTilstandType: TilstandType,
        tilstandType: TilstandType
    ) {
        assertVedtaksperiodetilstand(tilstandType)
        assertEquals(forrigeTilstandType, this.testObserver.forrigeVedtaksperiodetilstand)
    }

    private fun assertAlleVedtaksperiodetilstander(tilstandType: TilstandType) {
        assertTrue(this.testObserver.tilstandsendringer.values.all { it.gjeldendeTilstand == tilstandType })
    }

    private fun assertPersonEndret() {
        assertTrue(this.testObserver.personEndret)
    }

    private fun assertVedtaksperiodeEndret() {
        assertTrue(this.testObserver.vedtaksperiodeEndret)
    }

    private fun assertVedtaksperiodeIkkeEndret() {
        assertFalse(this.testObserver.vedtaksperiodeEndret)
    }

    private fun assertPersonIkkeEndret() {
        assertFalse(this.testObserver.personEndret)
    }

    private fun inntektsmelding(
        virksomhetsnummer: String = organisasjonsnummer,
        arbeidsgiverperioder: List<Periode> = listOf(Periode(10.september, 10.september.plusDays(16))),
        førsteFraværsdag: LocalDate = LocalDate.now()
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0
            ),
            orgnummer = virksomhetsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList()
        )

    private fun nySøknad(
        orgnummer: String = organisasjonsnummer,
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = NySøknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = perioder,
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun sendtSøknad(perioder: List<SendtSøknad.Periode> = listOf(SendtSøknad.Periode.Sykdom(16.september, 5.oktober, 100)), sendtNav: LocalDateTime = LocalDateTime.now()) =
        SendtSøknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = perioder,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            harAndreInntektskilder = false
        )


    private fun påminnelse(vedtaksperiodeId: UUID = vedtaksperiodeIdForPerson(), tilstandType: TilstandType) = Påminnelse(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private class TestPersonObserver : PersonObserver {
        internal val tilstandsendringer: MutableMap<UUID, PersonObserver.VedtaksperiodeEndretTilstandEvent> = mutableMapOf()
        internal val behovsliste: MutableList<Behov> = mutableListOf()
        internal var vedtaksperiodeEndret = false
        internal var personEndret = false
        internal var forrigeVedtaksperiodetilstand: TilstandType? = null
        internal var gjeldendeVedtaksperiodetilstand: TilstandType? = null
        internal var vedtaksperiodeIkkeFunnet = false
        internal var forrigeVedtaksperiodeIkkeFunnetEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent? = null

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            personEndret = true
        }

        override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
            vedtaksperiodeIkkeFunnet = true
            forrigeVedtaksperiodeIkkeFunnetEvent = vedtaksperiodeEvent
        }

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            vedtaksperiodeEndret = true
            forrigeVedtaksperiodetilstand = event.forrigeTilstand
            gjeldendeVedtaksperiodetilstand = event.gjeldendeTilstand

            tilstandsendringer[event.id] = event
        }

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            behovsliste.add(behov)
        }
    }

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {
        private var vedtaksperiodeindeks: Int = -1
        private val tilstander = mutableMapOf<Int, TilstandType>()
        internal lateinit var personLogger: Aktivitetslogger

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {
            personLogger = aktivitetslogger
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            tilstander[vedtaksperiodeindeks] = START
        }

        override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
            tilstander[vedtaksperiodeindeks] = tilstand.type
        }

        internal val vedtaksperiodeTeller get() = tilstander.size

        internal fun tilstand(indeks: Int) = tilstander[indeks]
    }
}
