package no.nav.helse.person

import no.nav.helse.SpolePeriode
import no.nav.helse.TestConstants.foreldrepenger
import no.nav.helse.TestConstants.foreldrepengeytelse
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.manuellSaksbehandlingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.objectMapper
import no.nav.helse.TestConstants.påminnelseHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.fixtures.mai
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellSaksbehandling
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.juli
import no.nav.helse.løsBehov
import no.nav.helse.person.TilstandType.BEREGN_UTBETALING
import no.nav.helse.person.TilstandType.MOTTATT_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.MOTTATT_NY_SØKNAD
import no.nav.helse.person.TilstandType.MOTTATT_SENDT_SØKNAD
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.VILKÅRSPRØVING
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

internal class VedtaksperiodeStateTest : VedtaksperiodeObserver {

    @Test
    fun `motta ny søknad`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(MOTTATT_NY_SØKNAD, NySøknad::class)
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

        assertTilstandsendring(TIL_INFOTRYGD, SendtSøknad::class)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(TIL_INFOTRYGD, Inntektsmelding::class)
    }

    @Test
    fun `motta sykdomshistorikk på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()

        assertIngenEndring {
            vedtaksperiode.håndter(
                Person(aktørId, fødselsnummer),
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
        assertTilstandsendring(TIL_INFOTRYGD, Påminnelse::class)
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn starttilstand`() {
        val vedtaksperiode = beInStartTilstand()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = MOTTATT_NY_SØKNAD
                )
            )
        }
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(MOTTATT_SENDT_SØKNAD)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(MOTTATT_INNTEKTSMELDING)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattNySøknad, gå TilInfotrygd`() {
        val vedtaksperiode = beInNySøknad()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = MOTTATT_NY_SØKNAD
            )
        )
        assertTilstandsendring(TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn MottattNySøknad`() {
        val vedtaksperiode = beInNySøknad()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = MOTTATT_INNTEKTSMELDING
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(VILKÅRSPRØVING)
        assertPåminnelse(Duration.ofHours(1))
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattSendtSøknad, gå TilInfotrygd`() {
        val vedtaksperiode = beInSendtSøknad()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = MOTTATT_SENDT_SØKNAD
            )
        )
        assertTilstandsendring(TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn SENDT_SØKNAD_MOTTATT`() {
        val vedtaksperiode = beInSendtSøknad()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = MOTTATT_INNTEKTSMELDING
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(sendtSøknadHendelse())

        assertTilstandsendring(VILKÅRSPRØVING)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(nySøknadHendelse())

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(inntektsmeldingHendelse())

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattInntektsmelding, gå TilInfotrygd`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        vedtaksperiode.håndter(
            påminnelseHendelse(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = MOTTATT_INNTEKTSMELDING
            )
        )
        assertTilstandsendring(TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn MottattInntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = MOTTATT_NY_SØKNAD
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `ber om vilkårsprøving etter at vi har mottatt søknad og inntektsmelding`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val sendtSøknadHendelse = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom = periodeFom, tom = periodeTom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        )

        val inntektsmeldingHendelse =
            inntektsmeldingHendelse(arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))))

        val vedtaksperiode = beInMottattInntektsmelding(
            tidslinje = tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                sendtSøknadTidslinje = null,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(sendtSøknadHendelse)

        assertTilstandsendring(VILKÅRSPRØVING)
        assertBehov(Behovstype.EgenAnsatt)
    }

    @Test
    fun `når vi går inn i BeregnUtbetaling, ber vi om sykepengehistorikk frem til og med dagen før perioden starter`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInVilkårsprøving(
            tidslinje = tidslinje(
                fom = periodeFom,
                tom = periodeTom
            )
        )

        vedtaksperiode.håndter(Vilkårsgrunnlag.Builder()
            .build(generiskBehov().løsBehov(mapOf("EgenAnsatt" to false)).toJson())!!)

        assertTilstandsendring(BEREGN_UTBETALING)

        assertBehov(Behovstype.Sykepengehistorikk)

        finnBehov(Behovstype.Sykepengehistorikk).get<LocalDate>("utgangspunktForBeregningAvYtelse").also {
            assertEquals(periodeFom.minusDays(1), it)
        }
    }

    @Test
    fun `Skal ikke behandle egen ansatt`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInVilkårsprøving(
            tidslinje = tidslinje(
                fom = periodeFom,
                tom = periodeTom
            )
        )

        vedtaksperiode.håndter(Vilkårsgrunnlag.Builder()
            .build(generiskBehov().løsBehov(mapOf("EgenAnsatt" to true)).toJson())!!)

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta tom sykepengehistorikk når tilstand er BeregnUtbetaling`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(periodeFom, periodeTom)
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
        assertPåminnelse(Duration.ofDays(7))
        assertBehov(Behovstype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk når tilstand er BeregnUtbetaling, og historikken er utenfor seks måneder`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(periodeFom, periodeTom)
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
        assertBehov(Behovstype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag innenfor seks måneder av denne periodens første sykedag`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli
        val sisteHistoriskeSykedag = periodeFom.minusMonths(5)

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(periodeFom, periodeTom)
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
        assertIkkeBehov(Behovstype.GodkjenningFraSaksbehandler)
    }

    @Test
    fun `motta sykepengehistorikk med siste sykedag nyere enn perioden det søkes for`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(periodeFom, periodeTom)
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
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

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `dersom en person har foreldrepenger i perioden behandles saken i infotrygd`() {
        vedtaksperiodeMedForeldrepenger( foreldrepengerFom = 30.mai, foreldrepengerTom = 14.juli, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(foreldrepengerFom = 2.juli, foreldrepengerTom = 21.juli, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(foreldrepengerFom = 30.mai, foreldrepengerTom = 21.juli, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(foreldrepengerFom = 2.juli, foreldrepengerTom = 14.juli, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_INFOTRYGD)
    }


    @Test
    fun `dersom en person ikke har foreldrepenger i perioden kan saken behandles`() {
        vedtaksperiodeMedForeldrepenger(foreldrepengerFom = 1.mai, foreldrepengerTom = 30.mai, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_GODKJENNING)
        vedtaksperiodeMedForeldrepenger(foreldrepengerFom = 21.juli, foreldrepengerTom = 30.juli, sykeperiodeFom = 1.juli, sykeperiodeTom = 20.juli)
        assertTilstandsendring(TIL_GODKJENNING)
    }

    private fun vedtaksperiodeMedForeldrepenger(
        foreldrepengerFom: LocalDate,
        foreldrepengerTom: LocalDate,
        sykeperiodeFom: LocalDate,
        sykeperiodeTom: LocalDate
    ) {
        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(sykeperiodeFom, sykeperiodeTom)
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    perioder = listOf()
                ),
                foreldrepenger = foreldrepenger(
                    foreldrepengeytelse = foreldrepengeytelse(
                        fom = foreldrepengerFom,
                        tom = foreldrepengerTom
                    ),
                    svangerskapsytelse = null
                ),

                vedtaksperiodeId = vedtaksperiodeId
            )
        )
    }


    @Test
    fun `gitt tilstand BeregnUtbetaling, når vi mottar svar på saksbehandler-behov vi ikke trenger, skal ingenting skje`() {
        val vedtaksperiode = beInBeregnUtbetaling()

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
    fun `motta påminnelse fra BeregnUtbetaling, fører til at behov sendes på nytt`() {
        val vedtaksperiode = beInBeregnUtbetaling()

        assertIngenEndringITilstand {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = BEREGN_UTBETALING
                )
            )
        }

        assertBehov(Behovstype.Sykepengehistorikk)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn BeregnUtbetaling`() {
        val vedtaksperiode = beInBeregnUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = MOTTATT_INNTEKTSMELDING
                )
            )
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `hele perioden skal utbetales av arbeidsgiver når opphørsdato for refusjon er etter siste dag i utbetaling`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli
        val sisteHistoriskeSykedag = periodeFom.minusMonths(7)

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = periodeTom.plusDays(1))
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = sisteHistoriskeSykedag
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver skal ikke utbetale hele perioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val sisteHistoriskeSykedag = periodeFom.minusMonths(7)

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = periodeTom)
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = sisteHistoriskeSykedag
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt opphørsdato for refusjon`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = Refusjon(opphoersdato = null)
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver endrer refusjonen etter utbetalingsperioden`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = periodeTom.plusDays(1))
            )
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    private fun tidslinje(
        fom: LocalDate,
        tom: LocalDate,
        sendtSøknadTidslinje: ConcreteSykdomstidslinje? = sendtSøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom, tom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        ).sykdomstidslinje(),
        inntektsmeldingTidslinje: ConcreteSykdomstidslinje = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(16))),
            endringerIRefusjoner = emptyList()
        ).sykdomstidslinje()
    ): ConcreteSykdomstidslinje {
        return nySøknadHendelse(
            søknadsperioder = listOf(SoknadsperiodeDTO(fom, tom)),
            egenmeldinger = emptyList(),
            fravær = emptyList()
        ).sykdomstidslinje().plus(sendtSøknadTidslinje) +
            inntektsmeldingTidslinje
    }

    private fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje?): ConcreteSykdomstidslinje {
        if (other == null) return this
        return this + other
    }

    @Test
    fun `arbeidsgiver endrer ikke refusjonen`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver endrer refusjonen i utbetalingsperioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = periodeTom)
            )
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt dato for endering av refusjon`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val inntektsmeldingHendelse = inntektsmeldingHendelse(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            endringerIRefusjoner = listOf(
                EndringIRefusjon(endringsdato = null)

            )
        )

        val vedtaksperiode = beInBeregnUtbetaling(
            tidslinje(
                fom = periodeFom,
                tom = periodeTom,
                inntektsmeldingTidslinje = inntektsmeldingHendelse.sykdomstidslinje()
            )
        )

        vedtaksperiode.håndter(
            Person(aktørId, fødselsnummer),
            Arbeidsgiver(organisasjonsnummer),
            ytelser(
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                ),
                vedtaksperiodeId = vedtaksperiodeId
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
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

        assertTilstandsendring(TIL_UTBETALING, ManuellSaksbehandling::class)
        assertPåminnelse(Duration.ZERO)
        assertMementoHarFelt(vedtaksperiode, "utbetalingsreferanse")
        assertBehov(Behovstype.Utbetaling)

        finnBehov(Behovstype.Utbetaling).also {
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

        assertTilstandsendring(TIL_INFOTRYGD, ManuellSaksbehandling::class)
    }

    @Test
    fun `motta sykepengehistorikk etter klar til utbetaling skal ikke endre state`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(
                Person(aktørId, fødselsnummer),
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
        assertTilstandsendring(TIL_INFOTRYGD, Påminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn TIL_GODKJENNING`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelseHendelse(
                    vedtaksperiodeId = vedtaksperiodeId,
                    tilstand = BEREGN_UTBETALING
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
        assertNull(forrigePåminnelse)
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

    private fun generiskBehov() = Behov.nyttBehov(
        hendelsestype = ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag,
        behov = listOf(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        additionalParams = mapOf()
    )

    private fun beInStartTilstand(
        nySøknad: NySøknad = nySøknadHendelse(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(
                orgnummer = organisasjonsnummer
            )
        )
    ): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(nySøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }
    }

    private fun beInStartTilstand(sendtSøknad: SendtSøknad): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(sendtSøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }
    }

    private fun beInTilInfotrygd(sendtSøknad: SendtSøknad = sendtSøknadHendelse()) =
        beInStartTilstand(sendtSøknad).apply {
            håndter(sendtSøknad)
        }

    private fun beInNySøknad(nySøknad: NySøknad = nySøknadHendelse()) =
        beInStartTilstand(nySøknad).apply {
            håndter(nySøknad)
        }

    private fun beInSendtSøknad(
        sendtSøknad: SendtSøknad = sendtSøknadHendelse(),
        nySøknad: NySøknad = nySøknadHendelse()
    ) =
        beInNySøknad(nySøknad).apply {
            håndter(sendtSøknad)
        }

    private fun beInMottattInntektsmelding(
        tidslinje: ConcreteSykdomstidslinje = nySøknadHendelse().sykdomstidslinje() + inntektsmeldingHendelse().sykdomstidslinje()
    ) =
        beIn(Vedtaksperiode.MottattInntektsmelding, tidslinje)

    private fun beInVilkårsprøving(tidslinje: ConcreteSykdomstidslinje = nySøknadHendelse().sykdomstidslinje() + inntektsmeldingHendelse().sykdomstidslinje()) =
        beIn(Vedtaksperiode.Vilkårsprøving, tidslinje)

    private fun beInBeregnUtbetaling(
        tidslinje: ConcreteSykdomstidslinje = nySøknadHendelse().sykdomstidslinje() + sendtSøknadHendelse().sykdomstidslinje() + inntektsmeldingHendelse().sykdomstidslinje()
    ) =
        beIn(Vedtaksperiode.BeregnUtbetaling, tidslinje)

    private fun beIn(
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        tidslinje: ConcreteSykdomstidslinje
    ) =
        Vedtaksperiode(
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            sykdomstidslinje = tidslinje,
            tilstand = tilstand
        ).also { it.addVedtaksperiodeObserver(this) }

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
        sendtSøknad: SendtSøknad = sendtSøknadHendelse(),
        inntektsmelding: Inntektsmelding = inntektsmeldingHendelse(),
        nySøknad: NySøknad = nySøknadHendelse()
    ) =
        beInBeregnUtbetaling(sendtSøknad.sykdomstidslinje() + inntektsmelding.sykdomstidslinje() + nySøknad.sykdomstidslinje()).apply {
            håndter(
                Person(aktørId, fødselsnummer),
                Arbeidsgiver(organisasjonsnummer),
                ytelser
            )
        }

    private fun beInTilUtbetaling(
        manuellSaksbehandling: ManuellSaksbehandling = manuellSaksbehandlingHendelse(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingGodkjent = true,
            saksbehandler = "en_saksbehandler_ident"
        )
    ) =
        beInTilGodkjenning().apply {
            håndter(manuellSaksbehandling)
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

    private fun finnBehov(behovstype: Behovstype) =
        behovsliste.first { it.behovType().contains(behovstype.name) }

    private fun harBehov(behovstype: Behovstype) =
        behovsliste.any { it.behovType().contains(behovstype.name) }

    private fun assertTilstandsendring(
        gjeldendeTilstandType: TilstandType,
        hendelsetype: KClass<out ArbeidstakerHendelse>? = null
    ) {
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
            assertTilstandsendring(gjeldendeTilstand)
        }
    }

    private fun assertIngenEndring(block: () -> Unit) {
        val antallBehov = behovsliste.size

        assertIngenEndringITilstand {
            block()
        }

        assertEquals(antallBehov, behovsliste.size)
    }

    private fun assertBehov(behovstype: Behovstype) {
        assertTrue(harBehov(behovstype))
    }

    private fun assertIkkeBehov(behovstype: Behovstype) {
        assertFalse(harBehov(behovstype))
    }

    private fun assertMementoHarFelt(vedtaksperiode: Vedtaksperiode, feltnavn: String) {
        val jsonNode = objectMapper.readTree(vedtaksperiode.memento().state())
        assertNotNull(jsonNode[feltnavn].takeUnless { it.isNull })
    }
}
