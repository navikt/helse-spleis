package no.nav.helse.sak

import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.manuellSaksbehandlingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.objectMapper
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.juli
import no.nav.helse.sak.TilstandType.*
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class VedtaksperiodeStateTest : VedtaksperiodeObserver {

    @Test
    fun `motta ny søknad`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(NY_SØKNAD_MOTTATT, START, NySøknadHendelse::class)
    }

    @Test
    fun `motta sendt søknad på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, START, SendtSøknadHendelse::class)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, START, InntektsmeldingHendelse::class)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        assertIngenEndring {
            vedtaksperiode.håndter(
                sykepengehistorikkHendelse(
                    sisteHistoriskeSykedag = LocalDate.now(),
                    vedtaksperiodeId = vedtaksperiodeId
                )
            )
        }
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(SENDT_SØKNAD_MOTTATT, NY_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(INNTEKTSMELDING_MOTTATT, NY_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, NY_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, SENDT_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, SENDT_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, SENDT_SØKNAD_MOTTATT)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, INNTEKTSMELDING_MOTTATT)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, INNTEKTSMELDING_MOTTATT)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, INNTEKTSMELDING_MOTTATT)
    }

    @Test
    fun `når saken er komplett, ber vi om sykepengehistorikk frem til og med dagen før perioden starter`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInMottattInntektsmelding(
                nySøknadHendelse = nySøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sendtSøknadHendelse)

        assertTilstandsendring(KOMPLETT_SYKDOMSTIDSLINJE, INNTEKTSMELDING_MOTTATT)

        assertBehov(BehovsTyper.Sykepengehistorikk)

        finnBehov(BehovsTyper.Sykepengehistorikk).get<LocalDate>("tom").also {
            assertEquals(periodeFom.minusDays(1), it)
        }
    }

    @Test
    fun `motta tom sykepengehistorikk når saken er komplett`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = null,
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
        assertBehov(BehovsTyper.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk når saken er komplett og historikken er utenfor seks måneder`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
        assertBehov(BehovsTyper.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne sakens første sykedag`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(5),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_INFOTRYGD, KOMPLETT_SYKDOMSTIDSLINJE)
        assertIkkeBehov(BehovsTyper.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag nyere enn perioden det søkes for`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                perioder = listOf(
                        SpolePeriode(
                                fom = periodeFom.minusMonths(1),
                                tom = periodeFom.plusMonths(1),
                                grad = "100"
                        )
                ),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_INFOTRYGD, KOMPLETT_SYKDOMSTIDSLINJE)
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
    fun `hele perioden skal utbetales av arbeidsgiver når opphørsdato for refusjon er etter siste dag i utbetaling`(){
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                refusjon = Refusjon(opphoersdato = periodeTom.plusDays(1))
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver skal ikke utbetale hele perioden, så dette må vurderes i Infotrygd`(){
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                refusjon = Refusjon(opphoersdato = periodeTom)
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_INFOTRYGD, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt opphørsdato for refusjon`(){
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                refusjon = Refusjon(opphoersdato = null)
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver enderer refusjonen etter utbetalingsperioden`(){
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                endringerIRefusjoner = listOf(
                        EndringIRefusjon(endringsdato = periodeTom.plusDays(1))
                )
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver enderer ikke refusjonen`(){
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                endringerIRefusjoner = emptyList()
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver enderer refusjonen i utbetalingsperioden, så dette må vurderes i Infotrygd`(){
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                endringerIRefusjoner = listOf(
                        EndringIRefusjon(endringsdato = periodeTom)
                )
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_INFOTRYGD, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt dato for endering av refusjon`(){
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val nySøknadHendelse = nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val sendtSøknadHendelse = sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)), egenmeldinger = emptyList(), fravær = emptyList())
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
                endringerIRefusjoner = listOf(
                        EndringIRefusjon(endringsdato = null)
                )
        )

        val vedtaksperiode = beInKomplettTidslinje(
                nySøknadHendelse = nySøknadHendelse,
                sendtSøknadHendelse = sendtSøknadHendelse,
                inntektsmeldingHendelse = inntektsmeldingHendelse)

        vedtaksperiode.håndter(sykepengehistorikkHendelse(
                sisteHistoriskeSykedag = periodeFom.minusMonths(7),
                vedtaksperiodeId = vedtaksperiodeId
        ))

        assertTilstandsendring(TIL_GODKJENNING, KOMPLETT_SYKDOMSTIDSLINJE)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling godkjent etter klar til utbetaling`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(manuellSaksbehandlingHendelse(
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalingGodkjent = true,
                saksbehandler = "en_saksbehandler_ident"
        ))

        assertTilstandsendring(TIL_UTBETALING, TIL_GODKJENNING, ManuellSaksbehandlingHendelse::class)
        assertMementoHarFelt(vedtaksperiode, "utbetalingsreferanse")
        assertBehov(BehovsTyper.Utbetaling)

        finnBehov(BehovsTyper.Utbetaling).also {
            assertNotNull(it["utbetalingsreferanse"])
        }
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling ikke godkjent etter klar til utbetaling`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(manuellSaksbehandlingHendelse(
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalingGodkjent = false,
                saksbehandler = "en_saksbehandler_ident"
        ))

        assertTilstandsendring(TIL_INFOTRYGD, TIL_GODKJENNING, ManuellSaksbehandlingHendelse::class)
    }

    @Test
    fun `motta sykepengehistorikk etter klar til utbetaling skal ikke endre state`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(
                sykepengehistorikkHendelse(
                    vedtaksperiodeId = vedtaksperiodeId
                )
            )
        }
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

    private fun beInNySøknad(nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInStartTilstand().apply {
                håndter(nySøknadHendelse)
            }

    private fun beInSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInNySøknad(nySøknadHendelse).apply {
                håndter(sendtSøknadHendelse)
            }

    private fun beInMottattInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                           nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInNySøknad(nySøknadHendelse).apply {
                håndter(inntektsmeldingHendelse)
            }

    private fun beInKomplettTidslinje(sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                      inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                      nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInMottattInntektsmelding(inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndter(sendtSøknadHendelse)
            }

    private fun beInTilGodkjenning(sykepengehistorikkHendelse: SykepengehistorikkHendelse = sykepengehistorikkHendelse(vedtaksperiodeId = vedtaksperiodeId, sisteHistoriskeSykedag = LocalDate.now().minusMonths(12)),
                                   sendtSøknadHendelse: SendtSøknadHendelse = sendtSøknadHendelse(),
                                   inntektsmeldingHendelse: InntektsmeldingHendelse = inntektsmeldingHendelse(),
                                   nySøknadHendelse: NySøknadHendelse = nySøknadHendelse()) =
            beInKomplettTidslinje(sendtSøknadHendelse, inntektsmeldingHendelse, nySøknadHendelse).apply {
                håndter(sykepengehistorikkHendelse)
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
    }

    @BeforeEach
    fun `tilbakestill behovliste`() {
        behovsliste.clear()
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        haveObserverBeenCalled = true
        lastStateEvent = event
        vedtaksperiodeEndringer++
    }

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        behovsliste.add(event)
    }

    private fun finnBehov(behovstype: BehovsTyper) =
        behovsliste.first { it.behovType() == behovstype.name }

    private fun harBehov(behovstype: BehovsTyper) =
        behovsliste.any { it.behovType() == behovstype.name }

    private fun assertTilstandsendring(
        gjeldendeTilstandType: TilstandType,
        forrigeTilstandType: TilstandType,
        hendelsetype: KClass<out ArbeidstakerHendelse>? = null
    ) {
        assertEquals(forrigeTilstandType, lastStateEvent.previousState)
        assertEquals(gjeldendeTilstandType, lastStateEvent.currentState)

        hendelsetype?.also {
            assertEquals(it, lastStateEvent.sykdomshendelse::class)
        }
    }

    private fun assertIngenEndring(block: () -> Unit) {
        val endringer = vedtaksperiodeEndringer

        val gjeldendeTilstand = if (endringer > 0) lastStateEvent.currentState else null
        val forrigeTilstand = if (endringer > 0) lastStateEvent.previousState else null

        block()

        assertEquals(vedtaksperiodeEndringer, endringer)

        if (gjeldendeTilstand != null && forrigeTilstand != null) {
            assertTilstandsendring(gjeldendeTilstand, forrigeTilstand)
        }
    }

    private fun assertBehov(behovstype: BehovsTyper) {
        assertTrue(harBehov(behovstype))
    }

    private fun assertIkkeBehov(behovstype: BehovsTyper) {
        assertFalse(harBehov(behovstype))
    }

    private fun assertMementoHarFelt(vedtaksperiode: Vedtaksperiode, feltnavn: String) {
        val jsonNode = objectMapper.readTree(vedtaksperiode.memento().state())
        assertNotNull(jsonNode[feltnavn].takeUnless { it.isNull })
    }
}
