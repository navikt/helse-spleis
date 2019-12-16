package no.nav.helse.sak

import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.manuellSaksbehandlingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.objectMapper
import no.nav.helse.TestConstants.påminnelseHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovtype
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.ytelser.Ytelser
import no.nav.helse.juli
import no.nav.helse.sak.TilstandType.*
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class VedtaksperiodeStateTest : VedtaksperiodeObserver {

    @Test
    fun `motta ny søknad`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(START, NY_SØKNAD_MOTTATT, NySøknadHendelse::class)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `skal ikke påminnes hvis "TilInfotrygd"`() {
        beInTilInfotrygd()
        assertPåminnelse(Duration.ZERO)
    }

    @Test
    fun `motta sendt søknad på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(START, TIL_INFOTRYGD, SendtSøknadHendelse::class)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(START, TIL_INFOTRYGD, InntektsmeldingHendelse::class)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        assertIngenEndring {
            vedtaksperiode.håndter(
                Sak(aktørId, fødselsnummer),
                Arbeidsgiver(organisasjonsnummer),
                ytelser(
                    vedtaksperiodeId = vedtaksperiodeId,
                    sykepengehistorikk = sykepengehistorikk(
                        sisteHistoriskeSykedag = LocalDate.now()
                        )
                )
            )
        }
    }

    @Test
    fun `motta påminnelse fra starttilstand, gå TilInfotrygd`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = START
            )
        )
        assertTilstandsendring(START, TIL_INFOTRYGD, Påminnelse::class)
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn starttilstand`() {
        val vedtaksperiode = beInStartTilstand()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = NY_SØKNAD_MOTTATT
                )
            )
        }
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(NY_SØKNAD_MOTTATT, SENDT_SØKNAD_MOTTATT)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(NY_SØKNAD_MOTTATT, INNTEKTSMELDING_MOTTATT)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(NY_SØKNAD_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra NySøknadMottatt, gå TilInfotrygd`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = NY_SØKNAD_MOTTATT
            )
        )
        assertTilstandsendring(NY_SØKNAD_MOTTATT, TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn NY_SØKNAD_MOTTATT`() {
        val vedtaksperiode = beInNySøknad()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = INNTEKTSMELDING_MOTTATT
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(SENDT_SØKNAD_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(SENDT_SØKNAD_MOTTATT, KOMPLETT_SYKDOMSTIDSLINJE)
        assertPåminnelse(Duration.ofHours(1))
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(SENDT_SØKNAD_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra SendtSøknadMottatt, gå TilInfotrygd`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = SENDT_SØKNAD_MOTTATT
            )
        )
        assertTilstandsendring(SENDT_SØKNAD_MOTTATT, TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn SENDT_SØKNAD_MOTTATT`() {
        val vedtaksperiode = beInSendtSøknad()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = INNTEKTSMELDING_MOTTATT
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra InntektsmeldingMottatt, gå TilInfotrygd`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = INNTEKTSMELDING_MOTTATT
            )
        )
        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn INNTEKTSMELDING_MOTTATT`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = NY_SØKNAD_MOTTATT
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk frem til og med dagen før perioden starter`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInMottattInntektsmelding(
            nySøknadHendelse = nySøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(sendtSøknadHendelse)

        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, KOMPLETT_SYKDOMSTIDSLINJE)

        assertBehov(Behovtype.Sykepengehistorikk)

        finnBehov(Behovtype.Sykepengehistorikk).get<LocalDate>("utgangspunktForBeregningAvYtelse").also {
            assertEquals(periodeFom.minusDays(1), it)
        }
    }

    @Test
    fun `motta tom sykepengehistorikk når saken er komplett`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
        assertPåminnelse(Duration.ofDays(7))
        assertBehov(Behovtype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett og historikken er utenfor seks måneder`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
        assertBehov(Behovtype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(5)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
        assertIkkeBehov(Behovtype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag nyere enn perioden det søkes for`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    perioder = listOf(
                        SpolePeriode(
                            fom = periodeFom.minusMonths(1),
                            tom = periodeFom.plusMonths(1),
                            grad = "100"
                        )
                    )
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
    }

    @Test
    fun `gitt en komplett tidslinje, når vi mottar svar på saksbehandler-behov vi ikke trenger, skal ingenting skje`() {
        val vedtaksperiode = beInKomplettTidslinje()

        assertIngenEndring {
            vedtaksperiode.håndter(
                manuellSaksbehandlingHendelse(
                    vedtaksperiodeId = vedtaksperiodeId.toString(),
                    utbetalingGodkjent = true,
                    saksbehandler = "en_saksbehandler_ident"
                )
            )
        }
    }

    @Test
    fun `motta påminnelse fra KomplettTidslinje, fører til at behov sendes på nytt`() {
        val vedtaksperiode = beInKomplettTidslinje()

        assertIngenEndringITilstand {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = KOMPLETT_SYKDOMSTIDSLINJE
                )
            )
        }

        assertBehov(Behovtype.Sykepengehistorikk)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn KOMPLETT_SYKDOMSTIDSLINJE`() {
        val vedtaksperiode = beInKomplettTidslinje()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = INNTEKTSMELDING_MOTTATT
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `hele perioden skal utbetales av arbeidsgiver når opphørsdato for refusjon er etter siste dag i utbetaling`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = periodeTom.plusDays(1))
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver skal ikke utbetale hele perioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = periodeTom)
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt opphørsdato for refusjon`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = null)
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver enderer refusjonen etter utbetalingsperioden`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = periodeTom.plusDays(1))
            )
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver enderer ikke refusjonen`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = emptyList()
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver enderer refusjonen i utbetalingsperioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = periodeTom)
            )
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt dato for endering av refusjon`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = null)
            )
        )

        val vedtaksperiode = beInKomplettTidslinje(
            nySøknadHendelse = nySøknadHendelse,
            sendtSøknadHendelse = sendtSøknadHendelse,
            inntektsmeldingHendelse = inntektsmeldingHendelse
        )

        vedtaksperiode.håndter(
            Sak(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, TIL_GODKJENNING)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling godkjent etter klar til utbetaling`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(
            manuellSaksbehandlingHendelse(
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalingGodkjent = true,
                saksbehandler = "en_saksbehandler_ident"
            )
        )

        assertTilstandsendring(TIL_GODKJENNING, TIL_UTBETALING, ManuellSaksbehandlingHendelse::class)
        assertPåminnelse(Duration.ofDays(7))
        assertMementoHarFelt(vedtaksperiode, "utbetalingsreferanse")
        assertBehov(Behovtype.Utbetaling)

        finnBehov(Behovtype.Utbetaling).also {
            assertNotNull(it["utbetalingsreferanse"])
        }
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling ikke godkjent etter klar til utbetaling`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(
            manuellSaksbehandlingHendelse(
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalingGodkjent = false,
                saksbehandler = "en_saksbehandler_ident"
            )
        )

        assertTilstandsendring(TIL_GODKJENNING, TIL_INFOTRYGD, ManuellSaksbehandlingHendelse::class)
    }

    @Test
    fun `motta sykepengehistorikk etter klar til utbetaling skal ikke endre state`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(
                Sak(aktørId, fødselsnummer),
                Arbeidsgiver(organisasjonsnummer),
                ytelser(
                    vedtaksperiodeId = vedtaksperiodeId,
                    sykepengehistorikk = sykepengehistorikk()
                )
            )
        }
    }

    @Test
    fun `motta påminnelse fra TilGodkjenning, gå TilInfotrygd`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = TIL_GODKJENNING
            )
        )
        assertTilstandsendring(TIL_GODKJENNING, TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn TIL_GODKJENNING`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = KOMPLETT_SYKDOMSTIDSLINJE
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta påminnelse fra TilUtbetaling, fører ikke til noen endring fordi Spenn svarer ikke med status ennå`() {
        val vedtaksperiode = beInTilUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = TIL_UTBETALING
                )
            )
        }
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn TIL_UTBETALING`() {
        val vedtaksperiode = beInTilUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = TIL_GODKJENNING
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    private fun beInStartTilstand(): Vedtaksperiode {
        return Vedtaksperiode(
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer
        ).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }
    }

    private fun beInTilInfotrygd() =
        beInStartTilstand().apply {
            håndter(sendtSøknadHendelse())
        }

    private fun beInNySøknad(nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
        beInStartTilstand().apply {
            håndter(nySøknadHendelse)
        }

    private fun beInSendtSøknad(
        sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
        nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()
    ) =
        beInNySøknad(nySøknadHendelse).apply {
            håndter(sendtSøknadHendelse)
        }

    private fun beInMottattInntektsmelding(
        inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
        nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()
    ) =
        beInNySøknad(nySøknadHendelse).apply {
            håndter(inntektsmeldingHendelse)
        }

    private fun beInKomplettTidslinje(
        sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
        inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
        nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()
    ) =
        beInMottattInntektsmelding(inntektsmeldingHendelse, nySøknadHendelse).apply {
            håndter(sendtSøknadHendelse)
        }

    private fun beInTilGodkjenning(
        ytelser: Ytelser = ytelser(
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            sykepengehistorikk = sykepengehistorikk(
                perioder = listOf(
                    SpolePeriode(
                        fom = LocalDate.now().minusMonths(12).minusMonths(1),
                        tom = LocalDate.now().minusMonths(12),
                        grad = "100"
                    )
                )
            )
        ),
        sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
        inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
        nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()
    ) =
        beInKomplettTidslinje(sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
            håndter(
                Sak(aktørId, fødselsnummer),
                Arbeidsgiver(organisasjonsnummer),
                ytelser)
        }

    private fun beInTilUtbetaling(
        manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse = manuellSaksbehandlingHendelse(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingGodkjent = true,
            saksbehandler = "en_saksbehandler_ident"
        )
    ) =
        beInTilGodkjenning().apply {
            håndter(manuellSaksbehandlingHendelse)
        }

    private companion object {
        private val aktørId = "1234567891011"
        private val fødselsnummer = "01017045896"
        private val organisasjonsnummer = "123456789"
        private val vedtaksperiodeId = UUID.randomUUID()

        private var haveObserverBeenCalled: Boolean = false
        private var vedtaksperiodeEndringer = 0
        private lateinit var lastStateEvent: VedtaksperiodeObserver.StateChangeEvent
        private val behovsliste: MutableList<Behov> = mutableListOf()
        private var forrigePåminnelse: Påminnelse? = null
    }

    @BeforeEach
    fun `tilbakestill behovliste`() {
        behovsliste.clear()
        forrigePåminnelse = null
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        haveObserverBeenCalled = true
        lastStateEvent = event
        vedtaksperiodeEndringer++
    }

    override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        forrigePåminnelse = påminnelse
    }

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        behovsliste.add(event)
    }

    private fun finnBehov(behovstype: Behovtype) =
        behovsliste.first { it.behovType().contains(behovstype.name) }

    private fun harBehov(behovstype: Behovtype) =
        behovsliste.any { it.behovType().contains(behovstype.name) }

    private fun assertTilstandsendring(
        forrigeTilstandType: TilstandType,
        gjeldendeTilstandType: TilstandType,
        hendelsetype: KClass<out ArbeidstakerHendelse>? = null
    ) {
        assertEquals(forrigeTilstandType, lastStateEvent.forrigeTilstand)
        assertEquals(gjeldendeTilstandType, lastStateEvent.gjeldendeTilstand)

        hendelsetype?.also {
            assertEquals(it, lastStateEvent.sykdomshendelse::class)
        }
    }

    private fun assertPåminnelse(timeout: Duration) {
        assertEquals(timeout, lastStateEvent.timeout)
    }

    private fun assertIngenEndringITilstand(block: () -> Unit) {
        val endringer = vedtaksperiodeEndringer

        val gjeldendeTilstand = if (endringer > 0) lastStateEvent.gjeldendeTilstand else null
        val forrigeTilstand = if (endringer > 0) lastStateEvent.forrigeTilstand else null

        block()

        assertEquals(vedtaksperiodeEndringer, endringer)
        if (gjeldendeTilstand != null && forrigeTilstand != null) {
            assertTilstandsendring(forrigeTilstand, gjeldendeTilstand)
        }
    }

    private fun assertIngenEndring(block: () -> Unit) {
        val antallBehov = behovsliste.size

        assertIngenEndringITilstand {
            block()
        }

        assertEquals(antallBehov, behovsliste.size)
    }

    private fun assertBehov(behovstype: Behovtype) {
        assertTrue(harBehov(behovstype))
    }

    private fun assertIkkeBehov(behovstype: Behovtype) {
        assertFalse(harBehov(behovstype))
    }

    private fun assertMementoHarFelt(vedtaksperiode: Vedtaksperiode, feltnavn: String) {
        val jsonNode = objectMapper.readTree(vedtaksperiode.memento().state())
        assertNotNull(jsonNode[feltnavn].takeUnless { it.isNull })
    }
}
