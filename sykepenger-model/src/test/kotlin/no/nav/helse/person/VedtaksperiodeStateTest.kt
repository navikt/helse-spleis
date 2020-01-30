package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class VedtaksperiodeStateTest : VedtaksperiodeObserver {

    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    fun setup() {
        aktivitetslogger = Aktivitetslogger()
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
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = periodeTom.plusDays(1),
                beløpPrMåned = 1000.0
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    @Test
    fun `arbeidsgiver skal ikke utbetale hele perioden, så dette må vurderes i Infotrygd`() {
        val periodeFom = 1.juli
        val periodeTom = 19.juli

        val inntektsmeldingHendelse = inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = periodeTom,
                beløpPrMåned = 1000.0
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
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
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
                )
            )
        )

        assertTilstandsendring(TIL_GODKJENNING)
    }

    private fun tidslinje(
        fom: LocalDate,
        tom: LocalDate,
        sendtSøknadTidslinje: ConcreteSykdomstidslinje? = sendtSøknad(
            perioder = listOf(ModelSendtSøknad.Periode.Sykdom(fom, tom, 100))
        ).sykdomstidslinje(),
        inntektsmeldingTidslinje: ConcreteSykdomstidslinje = inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(16)))
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
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
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
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
            arbeidsgiverperioder = listOf(Periode(periodeFom, periodeFom.plusDays(16))),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0
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
                utbetalinger = listOf(
                    Triple(
                        periodeFom.minusMonths(7).minusDays(10),
                        periodeFom.minusMonths(7),
                        1000
                    )
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
                ytelser()
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
        hendelseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = perioder,
        originalJson = SykepengesoknadDTO(
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
        ).toJsonNode().toString(),
        aktivitetslogger = aktivitetslogger
    )

    private fun sendtSøknad(
        perioder: List<ModelSendtSøknad.Periode> = listOf(
            ModelSendtSøknad.Periode.Sykdom(
                16.september,
                5.oktober,
                100
            )
        ), rapportertDato: LocalDateTime = LocalDateTime.now()
    ) =
        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            rapportertdato = rapportertDato,
            perioder = perioder,
            originalJson = "{}",
            aktivitetslogger = aktivitetslogger
        )

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode> = listOf(Periode(10.september, 10.september.plusDays(16))),
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
            aktivitetslogger = aktivitetslogger,
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
            rapportertdato = LocalDateTime.now(),
            aktivitetslogger = aktivitetslogger
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
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = aktivitetslogger
    )

    private fun ytelser(
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        fordrepengeYtelse: Periode? = null,
        svangerskapsytelse: Periode? = null
    ) = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = utbetalinger.map {
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    it.first,
                    it.second,
                    it.third
                )
            },
            inntektshistorikk = emptyList(),
            aktivitetslogger = aktivitetslogger
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = fordrepengeYtelse,
            svangerskapsytelse = svangerskapsytelse,
            aktivitetslogger = aktivitetslogger
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = aktivitetslogger
    )


    private fun beInStartTilstand(nySøknad: ModelNySøknad = nySøknad()) =
        Vedtaksperiode.nyPeriode(nySøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
        }

    private fun beInStartTilstand(sendtSøknad: ModelSendtSøknad) =
        Vedtaksperiode.nyPeriode(sendtSøknad, vedtaksperiodeId).apply {
            addVedtaksperiodeObserver(this@VedtaksperiodeStateTest)
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
            utbetalinger = listOf(
                Triple(
                    LocalDate.now().minusMonths(12).minusMonths(1),
                    LocalDate.now().minusMonths(12),
                    1000
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
            aktivitetslogger = aktivitetslogger,
            originalJson = "{}",
            arbeidsgiverperioder = listOf(
                Periode(10.september, 10.september.plusDays(16))
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

private typealias Utbetaling = Triple<LocalDate, LocalDate, Int>
