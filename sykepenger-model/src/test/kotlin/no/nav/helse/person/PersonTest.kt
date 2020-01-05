package no.nav.helse.person

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.påminnelseHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.juli
import no.nav.helse.person.TilstandType.*
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    private val testPerson
        get() = Person(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
            testObserver = TestPersonObserver()
            it.addObserver(this.testObserver)
        }

    @Test
    fun `uten arbeidsgiver`() {
        assertThrows<UtenforOmfangException> { testPerson.håndter(nySøknadHendelse(arbeidsgiver = null)) }
        assertThrows<UtenforOmfangException> { testPerson.håndter(sendtSøknadHendelse(arbeidsgiver = null)) }
        assertThrows<UtenforOmfangException> { testPerson.håndter(inntektsmeldingHendelse(virksomhetsnummer = null)) }
    }

    @Test
    fun `flere arbeidsgivere`() {
        assertThrows<UtenforOmfangException> {
            enPersonMedÉnArbeidsgiver(virksomhetsnummer_a).also {
                it.håndter(nySøknadHendelse(virksomhetsnummer = virksomhetsnummer_b))
            }
        }

        assertAntallPersonerEndret(1)
        assertAlleVedtaksperiodetilstander(TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny søknad fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        testPerson.also {
            it.håndter(nySøknadHendelse())
        }

        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD)
    }

    @Test
    internal fun `påminnelse blir delegert til perioden`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer)
                )
            )
            it.håndter(
                påminnelseHendelse(
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeIdForPerson(),
                    tilstand = MOTTATT_NY_SØKNAD
                )
            )
        }

        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(TIL_INFOTRYGD)
        assertFalse(testObserver.vedtaksperiodeIkkeFunnet)
    }

    @Test
    internal fun `påminnelse for periode som ikke finnes`() {
        val påminnelse = påminnelseHendelse(
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = UUID.randomUUID(),
            tilstand = MOTTATT_NY_SØKNAD
        )
        testPerson.also { it.håndter(påminnelse) }

        assertPersonIkkeEndret()
        assertVedtaksperiodeIkkeEndret()
        assertTrue(testObserver.vedtaksperiodeIkkeFunnet)
        assertEquals(påminnelse.vedtaksperiodeId(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.vedtaksperiodeId.toString())
        assertEquals(påminnelse.aktørId(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.aktørId)
        assertEquals(påminnelse.organisasjonsnummer(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.organisasjonsnummer)
        assertEquals(påminnelse.fødselsnummer(), testObserver.forrigeVedtaksperiodeIkkeFunnetEvent?.fødselsnummer)
    }

    @Test
    internal fun `sendt søknad uten eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(sendtSøknadHendelse())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding uten en eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = "123456789"
                )
            )
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding med eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = organisasjonsnummer
                    )
                )
            )

            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = organisasjonsnummer
                )
            )
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, MOTTATT_INNTEKTSMELDING)
    }

    @Test
    internal fun `ny periode blir opprettet når en ny søknad som ikke overlapper perioden personen har fra før blir sendt inn`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 20.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )

            assertThrows<UtenforOmfangException> {
                it.håndter(
                    nySøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(
                                fom = 21.juli,
                                tom = 28.juli,
                                sykmeldingsgrad = 100
                            )
                        ), egenmeldinger = emptyList(), fravær = emptyList()
                    )
                )
            }
        }
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende perioden`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 20.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )

            assertThrows<UtenforOmfangException> {
                it.håndter(
                    nySøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(
                                fom = 10.juli,
                                tom = 22.juli,
                                sykmeldingsgrad = 100
                            )
                        ), egenmeldinger = emptyList(), fravær = emptyList()
                    )
                )
            }
        }
    }


    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 20.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 20.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 10.juli,
                            tom = 30.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SENDT_SØKNAD, TIL_INFOTRYGD)
    }

    @Test
    internal fun `kaster ut periode når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 20.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    nySøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(
                                fom = 21.juli,
                                tom = 30.juli,
                                sykmeldingsgrad = 100
                            )
                        ), egenmeldinger = emptyList(), fravær = emptyList()
                    )
                )
            }
        }
    }

    @Test
    internal fun `ny periode må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 9.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 10.juli,
                            tom = 30.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når vi mottar den andre inntektsmeldngen`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = "12"),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = "12",
                    førsteFraværsdag = 1.juli,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))
                )
            )
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = "12",
                    førsteFraværsdag = 1.juli,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 1.juli.plusDays(16)))
                )
            )
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_INNTEKTSMELDING, TIL_INFOTRYGD)
    }


    @Test
    internal fun `ny søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    nySøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 60),
                            SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
                        )
                    )
                )
            }
        }
    }

    @Test
    internal fun `sendt søknad kan ikke være sendt mer enn 3 måneder etter perioden`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    sendtSøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100)
                        ),
                        sendtNav = Uke(1).mandag.plusMonths(4).atStartOfDay()
                    )
                )
            }
        }
    }

    @Test
    internal fun `sendt søknad med periode som ikke er 100 % kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    sendtSøknadHendelse(
                        søknadsperioder = listOf(
                            SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                            SoknadsperiodeDTO(
                                fom = Uke(1).fredag,
                                tom = Uke(1).fredag,
                                sykmeldingsgrad = 100,
                                faktiskGrad = 90
                            )
                        )
                    )
                )
            }
        }
    }

    @Test
    internal fun `ny søknad uten organisasjonsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    nySøknadHendelse(
                        arbeidsgiver = ArbeidsgiverDTO(
                            navn = "En arbeidsgiver",
                            orgnummer = null
                        )
                    )
                )
            }
        }
    }

    @Test
    internal fun `sendt søknad uten organisasjonsnummer kaster exception`() {
        testPerson.also {
            assertThrows<UtenforOmfangException> {
                it.håndter(
                    sendtSøknadHendelse(
                        arbeidsgiver = ArbeidsgiverDTO(
                            navn = "En arbeidsgiver",
                            orgnummer = null
                        )
                    )
                )
            }
        }
    }

    @Test
    internal fun `sendt søknad trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = organisasjonsnummer
                    )
                )
            )

            it.håndter(
                sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                        orgnummer = organisasjonsnummer
                    )
                )
            )
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

    @Test
    fun `motta en inntektsmelding som ikke kan behandles etter ny søknad`() {
        testPerson.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )

            val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
                (it as ObjectNode).remove("virksomhetsnummer")
            }
            val inntektsmeldingHendelse = requireNotNull(Inntektsmelding.Builder().build(inntektsmeldingJson.toString()))

            assertThrows<UtenforOmfangException> {
                it.håndter(inntektsmeldingHendelse)
            }

            assertVedtaksperiodeEndret()
            assertPersonEndret()
            assertVedtaksperiodetilstand(MOTTATT_NY_SØKNAD, TIL_INFOTRYGD)
        }
    }

    private fun vedtaksperiodeIdForPerson() =
        testObserver.tilstandsendringer.keys.first()

    private fun enPersonMedÉnArbeidsgiver(virksomhetsnummer: String) = testPerson.also {
        it.håndter(nySøknadHendelse(virksomhetsnummer = virksomhetsnummer))
    }

    private fun nySøknadHendelse(virksomhetsnummer: String) = NySøknad.Builder().build(
        søknadDTO(
            id = UUID.randomUUID().toString(),
            status = SoknadsstatusDTO.NY,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(
                orgnummer = virksomhetsnummer,
                navn = "en_arbeidsgiver"
            ),
            sendtNav = LocalDateTime.now()
        ).toJsonNode().toString())!!

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

    private fun assertBehov(vararg behovtype: Behovtype) {
        assertTrue(behovtype.all { behov -> testObserver.behovsliste.any { it.behovType().contains(behov.name) } })
    }

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
