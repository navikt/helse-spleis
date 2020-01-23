package no.nav.helse.person

import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelPåminnelse
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @BeforeEach
    internal fun setup() {
        testObserver = TestPersonObserver()
        testPerson = Person(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
            it.addObserver(this.testObserver)
        }
    }

    @Test
    fun `flere arbeidsgivere`() {
        assertThrows<Aktivitetslogger.AktivitetException> {
            enPersonMedÉnArbeidsgiver(virksomhetsnummer_a).also {
                it.håndter(nySøknad(orgnummer = virksomhetsnummer_b))
            }
        }

        assertAntallPersonerEndret(1)
        assertAlleVedtaksperiodetilstander(TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny søknad fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        testPerson.also {
            it.håndter(nySøknad())
        }

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
            påminnelse.vedtaksperiodeId(),
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
    internal fun `sendt søknad uten eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        sendtSøknad().also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding uten en eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        inntektsmelding().also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding med eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(nySøknad())
            it.håndter(inntektsmelding())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, MOTTATT_INNTEKTSMELDING)
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende perioden`() {
        testPerson.håndter(nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100))))
        nySøknad(perioder = listOf(Triple(10.juli, 22.juli, 100))).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndter(nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100))))
            it.håndter(
                sendtSøknad(
                    perioder = listOf(
                        Periode.Sykdom(
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
                Periode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    grad = 100
                )
            )
        ).also {
            testPerson.håndter(it)
            it.hasErrors()
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SENDT_SØKNAD, TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny periode må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testPerson.håndter(nySøknad(perioder = listOf(Triple(1.juli, 9.juli, 100))))
        sendtSøknad(
            perioder = listOf(
                Periode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    grad = 100
                )
            )
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre inntektsmeldingen`() {
        testPerson.håndter(nySøknad(orgnummer = "12", perioder = listOf(Triple(1.juli, 9.juli, 100))))
            testPerson.håndter(
                inntektsmelding(
                    virksomhetsnummer = "12",
                    førsteFraværsdag = 1.juli,
                    arbeidsgiverperioder = listOf(1.juli..1.juli.plusDays(16))
                )
            )
        inntektsmelding(
            virksomhetsnummer = "12",
            førsteFraværsdag = 1.juli,
            arbeidsgiverperioder = listOf(1.juli..1.juli.plusDays(16))
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_INNTEKTSMELDING, TIL_INFOTRYGD)
    }


    @Test
    internal fun `ny søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<Aktivitetslogger.AktivitetException> {
                it.håndter(
                    nySøknad(
                        perioder = listOf(
                            Triple(Uke(1).mandag, Uke(1).torsdag, 60),
                            Triple(Uke(1).fredag, Uke(1).fredag, 100)
                        )
                    )
                )
            }
        }
    }

    @Test
    internal fun `sendt søknad kan ikke være sendt mer enn 3 måneder etter perioden`() {
        sendtSøknad(
            perioder = listOf(
                Periode.Sykdom(fom = Uke(1).mandag, tom = Uke(1).torsdag, grad = 100)
            ),
            rapportertDato = Uke(1).mandag.plusMonths(4).atStartOfDay()
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasErrors())
        }
    }

    @Test
    internal fun `sendt søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<Aktivitetslogger.AktivitetException> {
                it.håndter(
                    sendtSøknad(
                        perioder = listOf(
                            Periode.Sykdom(fom = Uke(1).mandag, tom = Uke(1).torsdag, grad = 100),
                            Periode.Sykdom(
                                fom = Uke(1).fredag,
                                tom = Uke(1).fredag,
                                grad = 100,
                                faktiskGrad = 90.0
                            )
                        )
                    )
                )
            }
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
        assertVedtaksperiodetilstand(MOTTATT_SENDT_SØKNAD)
    }

    @Test
    internal fun `ytelser lager ikke ny periode, selv om det ikke finnes noen fra før`() {
        testPerson.also {
            it.håndter(ytelser(sykepengehistorikk = sykepengehistorikk()))
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
        arbeidsgiverperioder: List<ClosedRange<LocalDate>> = listOf(10.september..10.september.plusDays(16)),
        førsteFraværsdag: LocalDate = LocalDate.now()
    ) =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = null
            ),
            orgnummer = virksomhetsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList()
        )

    private fun nySøknad(
        orgnummer: String = organisasjonsnummer,
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = ModelNySøknad(
        UUID.randomUUID(),
        fødselsnummer,
        aktørId,
        orgnummer,
        LocalDateTime.now(),
        perioder,
        Aktivitetslogger(),
        "{}"
    )

    private fun sendtSøknad(perioder: List<Periode> = listOf(Periode.Sykdom(16.september, 5.oktober, 100)), rapportertDato: LocalDateTime = LocalDateTime.now()) =
        ModelSendtSøknad(
            UUID.randomUUID(),
            fødselsnummer,
            aktørId,
            organisasjonsnummer,
            rapportertDato,
            perioder,
            Aktivitetslogger(),
            "{}"
        )


    private fun påminnelse(vedtaksperiodeId: UUID = vedtaksperiodeIdForPerson(), tilstandType: TilstandType) = ModelPåminnelse(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    )

    private class TestPersonObserver : PersonObserver {
        internal val tilstandsendringer: MutableMap<UUID, VedtaksperiodeObserver.StateChangeEvent> = mutableMapOf()
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

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            vedtaksperiodeEndret = true
            forrigeVedtaksperiodetilstand = event.forrigeTilstand
            gjeldendeVedtaksperiodetilstand = event.gjeldendeTilstand

            tilstandsendringer[event.id] = event
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            behovsliste.add(event)
        }
    }
}
