package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.*
import no.nav.helse.TestConstants.foreldrepenger
import no.nav.helse.TestConstants.foreldrepengeytelse
import no.nav.helse.TestConstants.sykepengehistorikk
import no.nav.helse.TestConstants.ytelser
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.fixtures.mai
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal class VedtaksperiodeStateTest : VedtaksperiodeObserver {

    @Test
    fun `motta ny søknad`() {
        val vedtaksperiode = beInStartTilstand()
        vedtaksperiode.håndter(nySøknad())
        assertTilstandsendring(MOTTATT_NY_SØKNAD, ModelNySøknad::class)
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
        sendtSøknad().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD, ModelSendtSøknad::class)
    }

    @Test
    fun `motta inntektsmelding på feil tidspunkt`() {
        val vedtaksperiode = beInStartTilstand()
        inntektsmelding().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD, ModelInntektsmelding::class)
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
        vedtaksperiode.håndter(påminnelse(tilstandType = START))
        assertTilstandsendring(TIL_INFOTRYGD, ModelPåminnelse::class)
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn starttilstand`() {
        val vedtaksperiode = beInStartTilstand()
        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_NY_SØKNAD))
        }
    }

    @Test
    fun `motta sendt søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()
        vedtaksperiode.håndter(sendtSøknad())
        assertTilstandsendring(MOTTATT_SENDT_SØKNAD)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta inntektsmelding etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()
        vedtaksperiode.håndter(inntektsmelding())
        assertTilstandsendring(MOTTATT_INNTEKTSMELDING)
        assertPåminnelse(Duration.ofDays(30))
    }

    @Test
    fun `motta ny søknad etter ny søknad`() {
        val vedtaksperiode = beInNySøknad()
        nySøknad().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattNySøknad, gå TilInfotrygd`() {
        val vedtaksperiode = beInNySøknad()
        vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_NY_SØKNAD))
        assertTilstandsendring(TIL_INFOTRYGD, ModelPåminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn MottattNySøknad`() {
        val vedtaksperiode = beInNySøknad()
        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_INNTEKTSMELDING))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta ny søknad etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()
        nySøknad().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter sendt søknad`() {
        val vedtaksperiode = beInSendtSøknad()
        vedtaksperiode.håndter(inntektsmelding())
        assertTilstandsendring(VILKÅRSPRØVING)
        assertPåminnelse(Duration.ofHours(1))
    }

    @Test
    fun `motta ny søknad etter søknad`() {
        val vedtaksperiode = beInSendtSøknad()
        nySøknad().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattSendtSøknad, gå TilInfotrygd`() {
        val vedtaksperiode = beInSendtSøknad()
        vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_SENDT_SØKNAD))
        assertTilstandsendring(TIL_INFOTRYGD, ModelPåminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn SENDT_SØKNAD_MOTTATT`() {
        val vedtaksperiode = beInSendtSøknad()
        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_INNTEKTSMELDING))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta sendt søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()
        vedtaksperiode.håndter(sendtSøknad())
        assertTilstandsendring(VILKÅRSPRØVING)
    }

    @Test
    fun `motta ny søknad etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()
        nySøknad().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta inntektsmelding etter inntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()
        inntektsmelding().also {
            vedtaksperiode.håndter(it)
            assertTrue(it.hasErrors())
        }
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `motta påminnelse fra MottattInntektsmelding, gå TilInfotrygd`() {
        val vedtaksperiode = beInMottattInntektsmelding()
        vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_INNTEKTSMELDING))
        assertTilstandsendring(TIL_INFOTRYGD, ModelPåminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn MottattInntektsmelding`() {
        val vedtaksperiode = beInMottattInntektsmelding()
        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_NY_SØKNAD))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `ber om vilkårsprøving etter at vi har mottatt søknad og inntektsmelding`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli
        val sendtSøknadHendelse = sendtSøknad(
            perioder = listOf(Periode.Sykdom(fom = periodeFom, tom = periodeTom, grad = 100))
        )
        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16))
        )
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
        vedtaksperiode.håndter(
            ModelVilkårsgrunnlag(
                hendelseId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                rapportertDato = LocalDateTime.now(),
                inntektsmåneder = (1.rangeTo(12)).map {
                    ModelVilkårsgrunnlag.Måned(
                        årMåned = YearMonth.of(2018, it),
                        inntektsliste = listOf(
                            ModelVilkårsgrunnlag.Inntekt(
                                beløp = 1000.0
                            )
                        )
                    )
                },
                erEgenAnsatt = false,
                aktivitetslogger = Aktivitetslogger(),
                originalJson = "{}"
            )
        )
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
        vedtaksperiode.håndter(
            ModelVilkårsgrunnlag(
                hendelseId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiode.toString(),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                rapportertDato = LocalDateTime.now(),
                inntektsmåneder = emptyList(),
                erEgenAnsatt = true,
                aktivitetslogger = Aktivitetslogger(),
                originalJson = "{}"
            )
        )
        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `skal gå til infotrygd hvis det er avvik mellom inntektsmelding og inntekt fra inntektskomponenten`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli
        val vedtaksperiode = beInVilkårsprøving(
            tidslinje = tidslinje(
                fom = periodeFom,
                tom = periodeTom
            )
        )
        vedtaksperiode.håndter(
            ModelVilkårsgrunnlag(
                UUID.randomUUID(),
                vedtaksperiodeId.toString(),
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                LocalDateTime.now(),
                (1.rangeTo(12)).map {
                    ModelVilkårsgrunnlag.Måned(
                        årMåned = YearMonth.of(2018, it),
                        inntektsliste = listOf(
                            ModelVilkårsgrunnlag.Inntekt(
                                beløp = 532.7
                            )
                        )
                    )
                },
                false,
                Aktivitetslogger(),
                "{}"
            )
        )

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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk()
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                )
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(sisteHistoriskeSykedag = sisteHistoriskeSykedag)
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    perioder = listOf(
                        SpolePeriode(
                            fom = periodeFom.minusMonths(1),
                            tom = periodeFom.plusMonths(1),
                            grad = "100"
                        )
                    )
                )
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `dersom en person har foreldrepenger i perioden behandles saken i infotrygd`() {
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 30.mai,
            foreldrepengerTom = 14.juli,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 2.juli,
            foreldrepengerTom = 21.juli,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 30.mai,
            foreldrepengerTom = 21.juli,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
        assertTilstandsendring(TIL_INFOTRYGD)
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 2.juli,
            foreldrepengerTom = 14.juli,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
        assertTilstandsendring(TIL_INFOTRYGD)
    }


    @Test
    fun `dersom en person ikke har foreldrepenger i perioden kan saken behandles`() {
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 1.mai,
            foreldrepengerTom = 30.mai,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
        assertTilstandsendring(TIL_GODKJENNING)
        vedtaksperiodeMedForeldrepenger(
            foreldrepengerFom = 21.juli,
            foreldrepengerTom = 30.juli,
            sykeperiodeFom = 1.juli,
            sykeperiodeTom = 20.juli
        )
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    perioder = listOf()
                ),

                foreldrepenger = foreldrepenger(
                    foreldrepengeytelse = foreldrepengeytelse(
                        fom = foreldrepengerFom,
                        tom = foreldrepengerTom
                    ),
                    svangerskapsytelse = null
                )
            )
        )
    }


    @Test
    fun `gitt tilstand BeregnUtbetaling, når vi mottar svar på saksbehandler-behov vi ikke trenger, skal ingenting skje`() {
        val vedtaksperiode = beInBeregnUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(manuellSaksbehandling())
        }
    }

    @Test
    fun `motta påminnelse fra BeregnUtbetaling, fører til at behov sendes på nytt`() {
        val vedtaksperiode = beInBeregnUtbetaling()

        assertIngenEndringITilstand {
            vedtaksperiode.håndter(påminnelse(tilstandType = BEREGN_UTBETALING))
        }

        assertBehov(Behovstype.Sykepengehistorikk)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn BeregnUtbetaling`() {
        val vedtaksperiode = beInBeregnUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = MOTTATT_INNTEKTSMELDING))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `hele perioden skal utbetales av arbeidsgiver når opphørsdato for refusjon er etter siste dag i utbetaling`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli
        val sisteHistoriskeSykedag = periodeFom.minusMonths(7)

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16)),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = periodeTom.plusDays(1),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = null
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = sisteHistoriskeSykedag
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver skal ikke utbetale hele perioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val sisteHistoriskeSykedag = periodeFom.minusMonths(7)

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16)),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = periodeTom,
                beløpPrMåned = 1000.0,
                endringerIRefusjon = null
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = sisteHistoriskeSykedag
                )
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver endrer refusjonen etter utbetalingsperioden`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16)),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = listOf(
                    periodeTom.plusDays(1)
                )
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    private fun tidslinje(
        fom: LocalDate,
        tom: LocalDate,
        sendtSøknadTidslinje: ConcreteSykdomstidslinje? = sendtSøknad(
            perioder = listOf(Periode.Sykdom(fom, tom, 100))
        ).sykdomstidslinje(),
        inntektsmeldingTidslinje: ConcreteSykdomstidslinje = inntektsmelding(
            arbeidsgiverperioder = listOf(fom..fom.plusDays(16))
        ).sykdomstidslinje()
    ): ConcreteSykdomstidslinje {
        return nySøknad(perioder = listOf(Triple(fom, tom, 100)))
            .sykdomstidslinje().plus(sendtSøknadTidslinje) + inntektsmeldingTidslinje
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver endrer refusjonen i utbetalingsperioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16)),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = listOf(periodeTom)
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                )
            )
        )

        assertTilstandsendring(TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver har ikke oppgitt dato for endering av refusjon`() {
        val periodeFom = 1.juli
        val periodeTom = 20.juli

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(periodeFom..periodeFom.plusDays(16)),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = null
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
                vedtaksperiodeId = vedtaksperiodeId,
                sykepengehistorikk = sykepengehistorikk(
                    sisteHistoriskeSykedag = periodeFom.minusMonths(7)
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `motta manuell saksbehandling med utbetaling godkjent etter klar til utbetaling`() {
        val vedtaksperiode = beInTilGodkjenning()

        vedtaksperiode.håndter(manuellSaksbehandling())

        assertTilstandsendring(TIL_UTBETALING, ModelManuellSaksbehandling::class)
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

        vedtaksperiode.håndter(manuellSaksbehandling(utbetalingGodkjent = false))

        assertTilstandsendring(TIL_INFOTRYGD, ModelManuellSaksbehandling::class)
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

        vedtaksperiode.håndter(påminnelse(tilstandType = TIL_GODKJENNING))
        assertTilstandsendring(TIL_INFOTRYGD, ModelPåminnelse::class)
        assertEquals(vedtaksperiodeId.toString(), forrigePåminnelse?.vedtaksperiodeId())
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn TIL_GODKJENNING`() {
        val vedtaksperiode = beInTilGodkjenning()

        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = BEREGN_UTBETALING))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `motta påminnelse fra TilUtbetaling, fører ikke til noen endring fordi Spenn svarer ikke med status ennå`() {
        val vedtaksperiode = beInTilUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(påminnelse(tilstandType = TIL_UTBETALING))
        }
        assertNull(forrigePåminnelse)
    }

    @Test
    fun `ignorer påminnelse for en annen tilstand enn TIL_UTBETALING`() {
        val vedtaksperiode = beInTilUtbetaling()

        assertIngenEndring {
            vedtaksperiode.håndter(
                påminnelse(tilstandType = TIL_GODKJENNING)
            )
        }
        assertNull(forrigePåminnelse)
    }

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
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = aktørId,
            fnr = fødselsnummer,
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                orgnummer
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = perioder.map { SoknadsperiodeDTO(it.first, it.second, it.third) }
        ).toJsonNode().toString()
    )

    private fun sendtSøknad(
        perioder: List<Periode> = listOf(
            Periode.Sykdom(
                16.september,
                5.oktober,
                100
            )
        ), rapportertDato: LocalDateTime = LocalDateTime.now()
    ) =
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

    private fun inntektsmelding(
        arbeidsgiverperioder: List<ClosedRange<LocalDate>> = listOf(10.september..10.september.plusDays(16)),
        refusjon: ModelInntektsmelding.Refusjon = ModelInntektsmelding.Refusjon(
            opphørsdato = LocalDate.now(),
            beløpPrMåned = 1000.0,
            endringerIRefusjon = emptyList()
        )
    ) =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = LocalDate.now(),
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList()
        )

    private fun manuellSaksbehandling(utbetalingGodkjent: Boolean = true): ModelManuellSaksbehandling {
        return ModelManuellSaksbehandling(
            hendelseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingGodkjent = utbetalingGodkjent,
            saksbehandler = "en_saksbehandler_ident",
            rapportertdato = LocalDateTime.now()
        )
    }

    private fun påminnelse(tilstandType: TilstandType) = ModelPåminnelse(
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


    private fun beInStartTilstand(nySøknad: ModelNySøknad = nySøknad()): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(nySøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }
    }

    private fun beInStartTilstand(sendtSøknad: ModelSendtSøknad): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(sendtSøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }
    }

    private fun beInTilInfotrygd(sendtSøknad: ModelSendtSøknad = sendtSøknad()) =
        beInStartTilstand(sendtSøknad).apply {
            sendtSøknad.also {
                håndter(it)
                it.hasErrors()
            }
        }

    private fun beInNySøknad(nySøknad: ModelNySøknad = nySøknad()) =
        beInStartTilstand(nySøknad).apply {
            håndter(nySøknad)
        }

    private fun beInSendtSøknad(
        sendtSøknad: ModelSendtSøknad = sendtSøknad(),
        nySøknad: ModelNySøknad = nySøknad()
    ) =
        beInNySøknad(nySøknad).apply {
            håndter(sendtSøknad)
        }

    private fun beInMottattInntektsmelding(
        tidslinje: ConcreteSykdomstidslinje = nySøknad().sykdomstidslinje() + inntektsmelding().sykdomstidslinje()
    ) =
        beIn(Vedtaksperiode.MottattInntektsmelding, tidslinje)

    private fun beInVilkårsprøving(tidslinje: ConcreteSykdomstidslinje = nySøknad().sykdomstidslinje() + inntektsmelding().sykdomstidslinje()) =
        beIn(Vedtaksperiode.Vilkårsprøving, tidslinje)

    private fun beInBeregnUtbetaling(
        tidslinje: ConcreteSykdomstidslinje = nySøknad().sykdomstidslinje() + sendtSøknad().sykdomstidslinje() + inntektsmelding().sykdomstidslinje()
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
        ytelser: ModelYtelser = ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
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
        sendtSøknad: ModelSendtSøknad = sendtSøknad(),
        inntektsmelding: ModelInntektsmelding = ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = emptyList()
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = LocalDate.now(),
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = listOf(
                10.september..10.september.plusDays(16)
            ),
            ferieperioder = emptyList()
        ),
        nySøknad: ModelNySøknad = nySøknad()
    ) =
        beInBeregnUtbetaling(sendtSøknad.sykdomstidslinje() + inntektsmelding.sykdomstidslinje() + nySøknad.sykdomstidslinje()).apply {
            håndter(
                Person(aktørId, fødselsnummer),
                Arbeidsgiver(organisasjonsnummer),
                ytelser
            )
        }

    private fun beInTilUtbetaling(
        manuellSaksbehandling: ModelManuellSaksbehandling = manuellSaksbehandling()
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
        private var forrigePåminnelse: ModelPåminnelse? = null
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

    override fun vedtaksperiodePåminnet(påminnelse: ModelPåminnelse) {
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

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun assertMementoHarFelt(vedtaksperiode: Vedtaksperiode, feltnavn: String) {
        val jsonNode = objectMapper.readTree(vedtaksperiode.memento().state())
        assertNotNull(jsonNode[feltnavn].takeUnless { it.isNull })
    }
}
