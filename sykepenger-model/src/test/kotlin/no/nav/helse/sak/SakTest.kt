package no.nav.helse.sak

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Uke
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.sak.TilstandType.*
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.FravarDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.FravarstypeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set

internal class SakTest {

    private val aktørId = "id"
    private val fødselsnummer = "01017000000"
    private val organisasjonsnummer = "12"

    private val virksomhetsnummer_a = "234567890"
    private val virksomhetsnummer_b = "098765432"

    private val testSakObserver = TestSakObserver()

    private val testSak get() = Sak(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
        it.addObserver(this.testSakObserver)
    }

    @Test
    fun `uten arbeidsgiver`() {
        assertThrows<UtenforOmfangException> { testSak.håndter(nySøknadHendelse(arbeidsgiver = null)) }
        assertThrows<UtenforOmfangException> { testSak.håndter(sendtSøknadHendelse(arbeidsgiver = null)) }
        assertThrows<UtenforOmfangException> { testSak.håndter(inntektsmeldingHendelse(virksomhetsnummer = null)) }
    }

    @Test
    fun `flere arbeidsgivere`() {
        assertThrows<UtenforOmfangException> {
            enSakMedÉnArbeidsgiver(virksomhetsnummer_a).also {
                it.håndter(nySøknadHendelse(virksomhetsnummer = virksomhetsnummer_b))
            }
        }

        assertAntallSakerEndret(1)
        assertAlleVedtaksperiodetilstander(TIL_INFOTRYGD)
    }

    @Test
    internal fun `ny søknad fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        testSak.also {
            it.håndter(nySøknadHendelse())
        }

        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(NY_SØKNAD_MOTTATT)
    }

    @Test
    internal fun `sendt søknad uten sak trigger vedtaksperiode endret-hendelse`() {
        testSak.also {
            it.håndter(sendtSøknadHendelse())
        }
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding uten sak trigger vedtaksperiode endret-hendelse`() {
        testSak.also {
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = "123456789"
                )
            )
        }
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `inntektsmelding med sak trigger vedtaksperiode endret-hendelse`() {
        testSak.also {
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
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(NY_SØKNAD_MOTTATT, INNTEKTSMELDING_MOTTATT)
    }

    @Test
    internal fun `ny sak blir opprettet når en ny søknad som ikke overlapper saken personen har fra før blir sendt inn`() {
        testSak.also {
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
    internal fun `eksisterende sak må behandles i infotrygd når en ny søknad overlapper sykdomstidslinjen i den eksisterende saken`() {
        testSak.also {
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
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre sendte søknaden`() {
        testSak.also {
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
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(SENDT_SØKNAD_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    internal fun `kaster ut sak når ny søknad kommer, som ikke overlapper med eksisterende`() {
        testSak.also {
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
    internal fun `ny sak må behandles i infotrygd når vi mottar den sendte søknaden først`() {
        testSak.also {
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
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `eksisterende sak må behandles i infotrygd når vi mottar den andre inntektsmeldngen`() {
        testSak.also {
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
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(INNTEKTSMELDING_MOTTATT, TIL_INFOTRYGD)
    }


    @Test
    internal fun `ny søknad med periode som ikke er 100 % kaster exception`() {
        testSak.also {
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
        testSak.also {
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
        testSak.also {
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
        testSak.also {
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
        testSak.also {
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
        testSak.also {
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
        assertSakEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(SENDT_SØKNAD_MOTTATT)
    }

    @Test
    internal fun `sykepengehistorikk med arbeidsgiver som ikke finnes i modellen skal kaste exception`() {
        assertThrows<IllegalStateException> {
            testSak.also {
                it.håndter(sykepengehistorikkHendelse(LocalDate.now()))
            }
        }
    }

    @Test
    fun `komplett genererer sykepengehistorikk-needs`() {
        testSak.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )

            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = organisasjonsnummer,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))
                )
            )
        }

        assertVedtaksperiodeEndret()
        assertSakEndret()
        assertVedtaksperiodetilstand(SENDT_SØKNAD_MOTTATT, KOMPLETT_SYKDOMSTIDSLINJE)
        assertBehov(BehovsTyper.Sykepengehistorikk)
    }

    @Test
    fun `sykepengehistorikk eldre enn seks måneder fører saken videre`() {
        testSak.also {
            it.håndter(
                nySøknadHendelse(
                    fødselsnummer = fødselsnummer,
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 30.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    fødselsnummer = fødselsnummer,
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 30.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                inntektsmeldingHendelse(
                    fødselsnummer = fødselsnummer,
                    virksomhetsnummer = organisasjonsnummer,
                    arbeidsgiverperioder = listOf(Periode(30.juni, 5.juli))
                )
            )

            assertEquals(1, this.testSakObserver.sakstilstander.size)
            val saksid = this.testSakObserver.sakstilstander.keys.first()

            it.håndter(
                sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = saksid
                )
            )
        }

        assertVedtaksperiodetilstand(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
        assertBehov(BehovsTyper.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `sykepengehistorikk med feil vedtaksperiodeid skal ikke føre noen saker videre`() {
        testSak.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = organisasjonsnummer,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))
                )
            )

            it.håndter(
                sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = UUID.randomUUID()
                )
            )
        }

        assertVedtaksperiodetilstand(SENDT_SØKNAD_MOTTATT, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `sykepengehistorikk yngre enn seks måneder fører til at saken må behandles i infotrygd`() {
        testSak.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                    søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 9.juli, sykmeldingsgrad = 100)),
                    egenmeldinger = emptyList(),
                    fravær = emptyList()
                )
            )
            it.håndter(
                inntektsmeldingHendelse(
                    virksomhetsnummer = organisasjonsnummer,
                    arbeidsgiverperioder = listOf(Periode(1.juli, 9.juli))
                )
            )

            assertEquals(1, this.testSakObserver.sakstilstander.size)
            val saksid = this.testSakObserver.sakstilstander.keys.first()

            it.håndter(
                sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(5),
                    organisasjonsnummer = organisasjonsnummer,
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = saksid
                )
            )
        }
        assertVedtaksperiodeEndret()
        assertSakEndret()
        assertVedtaksperiodetilstand(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
    }

    @Test
    fun `motta en inntektsmelding som ikke kan behandles etter ny søknad`() {
        testSak.also {
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
            val inntektsmeldingHendelse = InntektsmeldingHendelse(Inntektsmelding(inntektsmeldingJson))

            assertThrows<UtenforOmfangException> {
                it.håndter(inntektsmeldingHendelse)
            }

            assertVedtaksperiodeEndret()
            assertSakEndret()
            assertVedtaksperiodetilstand(NY_SØKNAD_MOTTATT, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `vedtaksperiode uten utbetalingsdager skal ikke sendes til godkjenning`() {
        testSak.also {
            it.håndter(
                nySøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                        navn = "S.Vindel og Sønn",
                        orgnummer = organisasjonsnummer
                    ),
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 30.juli,
                            sykmeldingsgrad = 100
                        )
                    ), egenmeldinger = emptyList(), fravær = emptyList()
                )
            )

            it.håndter(inntektsmeldingHendelse(
                førsteFraværsdag = 1.juli,
                arbeidsgiverperioder = listOf(),
                virksomhetsnummer = organisasjonsnummer
            ))

            it.håndter(
                sendtSøknadHendelse(
                    arbeidsgiver = ArbeidsgiverDTO(
                        navn = "S.Vindel og Sønn",
                        orgnummer = organisasjonsnummer
                    ),
                    søknadsperioder = listOf(
                        SoknadsperiodeDTO(
                            fom = 1.juli,
                            tom = 30.juli,
                            sykmeldingsgrad = 100
                        )
                    ),
                    egenmeldinger = emptyList(),
                    fravær = listOf(
                        FravarDTO(
                            fom = 2.juli,
                            tom = 30.juli,
                            type = FravarstypeDTO.FERIE
                        )
                    )
                )
            )

            val saksid = this.testSakObserver.sakstilstander.keys.first()

            it.håndter(
                sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = 1.juli.minusMonths(7),
                    vedtaksperiodeId = saksid,
                    organisasjonsnummer = organisasjonsnummer
                )
            )
        }
        assertVedtaksperiodetilstand(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
    }

    private fun enSakMedÉnArbeidsgiver(virksomhetsnummer: String) = testSak.also {
        it.håndter(nySøknadHendelse(virksomhetsnummer = virksomhetsnummer))
    }

    private fun nySøknadHendelse(virksomhetsnummer: String) = NySøknadHendelse(
        Sykepengesøknad(
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
            ).toJsonNode()
        )
    )

    private fun assertAntallSakerEndret(antall: Int) {
        assertEquals(antall, this.testSakObserver.sakstilstander.size)
    }

    private fun assertVedtaksperiodetilstand(tilstandType: TilstandType) {
        assertEquals(tilstandType, this.testSakObserver.gjeldendeVedtaksperiodetilstand)
    }

    private fun assertVedtaksperiodetilstand(
        forrigeTilstandType: TilstandType,
        tilstandType: TilstandType
    ) {
        assertVedtaksperiodetilstand(tilstandType)
        assertEquals(forrigeTilstandType, this.testSakObserver.forrigeVedtaksperiodetilstand)
    }

    private fun assertAlleVedtaksperiodetilstander(tilstandType: TilstandType) {
        assertTrue(this.testSakObserver.sakstilstander.values.all { it.currentState == tilstandType })
    }

    private fun assertSakEndret() {
        assertTrue(this.testSakObserver.sakEndret)
    }

    private fun assertVedtaksperiodeEndret() {
        assertTrue(this.testSakObserver.vedtaksperiodeEndret)
    }

    private fun assertVedtaksperiodeIkkeEndret() {
        assertFalse(this.testSakObserver.vedtaksperiodeEndret)
    }

    private fun assertSakIkkeEndret() {
        assertFalse(this.testSakObserver.sakEndret)
    }

    private fun assertBehov(behovtype: BehovsTyper) {
        assertNotNull(this.testSakObserver.needEvent.find { it.behovType() == behovtype.name })
    }

    private class TestSakObserver : SakObserver {
        internal val sakstilstander: MutableMap<UUID, VedtaksperiodeObserver.StateChangeEvent> = mutableMapOf()
        internal val needEvent: MutableList<Behov> = mutableListOf()
        internal var vedtaksperiodeEndret = false
        internal var sakEndret = false
        internal var forrigeVedtaksperiodetilstand: TilstandType? = null
        internal var gjeldendeVedtaksperiodetilstand: TilstandType? = null

        override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
            sakEndret = true
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            vedtaksperiodeEndret = true
            forrigeVedtaksperiodetilstand = event.previousState
            gjeldendeVedtaksperiodetilstand = event.currentState

            sakstilstander[event.id] = event
        }

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            needEvent.add(event)
        }
    }
}
