package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.torsdag
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
            it.håndter(sykmelding(orgnummer = virksomhetsnummer_b))
        }
        assertTrue(inspektør.personLogg.hasErrors())
        assertAntallPersonerEndret(1)
        assertAlleVedtaksperiodetilstander(TIL_INFOTRYGD)
    }

    @Test
    internal fun `sykmelding fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        testPerson.also {
            it.håndter(sykmelding())
        }
        assertTrue(inspektør.personLogg.toString().contains("Ny arbeidsgiver"))
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    internal fun `påminnelse blir delegert til perioden`() {
        testPerson.also {
            it.håndter(sykmelding())
            it.håndter(påminnelse(tilstandType = MOTTATT_SYKMELDING_FERDIG_GAP))
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
            tilstandType = MOTTATT_SYKMELDING_FERDIG_GAP
        )

        assertThrows<Aktivitetslogg.AktivitetException> { testPerson.also { it.håndter(påminnelse) } }

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
            it.håndter(sykmelding())
            it.håndter(inntektsmelding())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    internal fun `eksisterende periode må behandles i infotrygd når en sykmelding overlapper sykdomstidslinjen i den eksisterende perioden`() {
        testPerson.håndter(sykmelding(perioder = listOf(Triple(1.juli, 20.juli, 100))))
        sykmelding(perioder = listOf(Triple(10.juli, 22.juli, 100))).also {
            testPerson.håndter(it)
            assertTrue(it.hasWarnings())
            assertFalse(it.hasErrors())
        }
    }

    @Test
    internal fun `ny periode må behandles i infotrygd når vi mottar søknaden før sykmelding`() {
        testPerson.håndter(sykmelding(perioder = listOf(Triple(1.juli, 9.juli, 100))))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        assertTrue(inspektør.personLogg.hasMessages())
        assertFalse(inspektør.personLogg.hasErrors())
        søknad(
            perioder = listOf(
                Søknad.Periode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    gradFraSykmelding = 100,
                    faktiskSykdomsgrad = null
                )
            )
        ).also {
            testPerson.håndter(it)
            assertTrue(it.hasWarnings())
            assertTrue(it.hasErrors())
        }
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())

        assertPersonEndret()
        assertVedtaksperiodeEndret()
    }

    @Test
    internal fun `sykmelding med periode som ikke er 100 %`() {
        sykmelding(
            perioder = listOf(
                Triple(1.mandag, 1.torsdag, 60),
                Triple(1.fredag, 1.fredag, 100)
            )
        ).also {
            testPerson.håndter(it)
            assertFalse(it.hasErrors())
        }
    }

    @Test
    internal fun `søknad trigger vedtaksperiode endret-hendelse`() {
        testPerson.also {
            it.håndter(sykmelding())
            it.håndter(søknad())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(AVVENTER_GAP)
    }

    @Test
    internal fun `ytelser lager ikke ny periode, selv om det ikke finnes noen fra før`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            testPerson.also {
                it.håndter(
                    Ytelser(
                        meldingsreferanseId = UUID.randomUUID(),
                        aktørId = aktørId,
                        fødselsnummer = fødselsnummer,
                        organisasjonsnummer = organisasjonsnummer,
                        vedtaksperiodeId = UUID.randomUUID().toString(),
                        utbetalingshistorikk = Utbetalingshistorikk(
                            utbetalinger = emptyList(),
                            inntektshistorikk = emptyList(),
                            graderingsliste = emptyList(),
                            aktivitetslogg = Aktivitetslogg()
                        ),
                        foreldrepermisjon = Foreldrepermisjon(null, null, Aktivitetslogg()),
                        aktivitetslogg = Aktivitetslogg()
                    )
                )
            }
        }

        assertPersonIkkeEndret()
        assertVedtaksperiodeIkkeEndret()
    }

    private fun vedtaksperiodeIdForPerson() =
        testObserver.tilstandsendringer.keys.first()

    private fun enPersonMedÉnArbeidsgiver(virksomhetsnummer: String) = testPerson.also {
        it.håndter(sykmelding(orgnummer = virksomhetsnummer))
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
        førsteFraværsdag: LocalDate = 10.september
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
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun sykmelding(
        orgnummer: String = organisasjonsnummer,
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = perioder
    )

    private fun søknad(
        perioder: List<Søknad.Periode> = listOf(
            Søknad.Periode.Sykdom(16.september, 5.oktober, 100)
        ),
        sendtTilNAV: LocalDateTime = Søknad.Periode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay()
    ) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = perioder,
            harAndreInntektskilder = false,
            sendtTilNAV = sendtTilNAV
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
        nestePåminnelsestidspunkt = LocalDateTime.now()
    )

    private class TestPersonObserver : PersonObserver {
        internal val tilstandsendringer: MutableMap<UUID, PersonObserver.VedtaksperiodeEndretTilstandEvent> = mutableMapOf()
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

            tilstandsendringer[event.vedtaksperiodeId] = event
        }
    }
}
