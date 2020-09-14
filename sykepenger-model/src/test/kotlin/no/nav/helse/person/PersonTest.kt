package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.torsdag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
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
    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)

    @BeforeEach
    fun setup() {
        testObserver = TestPersonObserver()
        person = Person(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
            it.addObserver(this.testObserver)
        }
    }

    @Test
    fun `sykmelding fører til at vedtaksperiode trigger en vedtaksperiode endret hendelse`() {
        person.also {
            it.håndter(sykmelding())
        }
        assertTrue(inspektør.personLogg.toString().contains("Ny arbeidsgiver"))
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `påminnelse for periode som ikke finnes`() {
        val påminnelse = påminnelse(
            vedtaksperiodeId = UUID.randomUUID(),
            tilstandType = MOTTATT_SYKMELDING_FERDIG_GAP
        )

        person.also { it.håndter(påminnelse) }
        assertTrue(påminnelse.hasErrors())

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
    fun `inntektsmelding med eksisterende periode trigger vedtaksperiode endret-hendelse`() {
        person.also {
            it.håndter(sykmelding())
            it.håndter(inntektsmelding())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    fun `eksisterende periode må behandles i infotrygd når en sykmelding overlapper sykdomstidslinjen i den eksisterende perioden`() {
        person.håndter(sykmelding(perioder = listOf(Sykmeldingsperiode(1.juli, 20.juli, 100))))
        sykmelding(perioder = listOf(Sykmeldingsperiode(10.juli, 22.juli, 100))).also {
            person.håndter(it)
            assertTrue(it.hasWarnings())
            assertFalse(it.hasErrors())
        }
    }

    @Test
    fun `ny periode må behandles i infotrygd når vi mottar søknaden før sykmelding`() {
        person.håndter(sykmelding(perioder = listOf(Sykmeldingsperiode(1.juli, 9.juli, 100))))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))
        assertTrue(inspektør.personLogg.hasMessages())
        assertFalse(inspektør.personLogg.hasErrors())
        søknad(
            perioder = listOf(
                Søknad.Søknadsperiode.Sykdom(
                    fom = 10.juli,
                    tom = 30.juli,
                    gradFraSykmelding = 100,
                    faktiskSykdomsgrad = null
                )
            )
        ).also {
            person.håndter(it)
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
    fun `sykmelding med periode som ikke er 100 %`() {
        sykmelding(
            perioder = listOf(
                Sykmeldingsperiode(1.mandag, 1.torsdag, 60),
                Sykmeldingsperiode(1.fredag, 1.fredag, 100)
            )
        ).also {
            person.håndter(it)
            assertFalse(it.hasErrors())
        }
    }

    @Test
    fun `søknad trigger vedtaksperiode endret-hendelse`() {
        person.also {
            it.håndter(sykmelding())
            it.håndter(søknad())
        }
        assertPersonEndret()
        assertVedtaksperiodeEndret()
        assertVedtaksperiodetilstand(AVVENTER_GAP)
    }

    @Test
    fun `ytelser lager ikke ny periode, selv om det ikke finnes noen fra før`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            person.also {
                val meldingsreferanseId = UUID.randomUUID()
                it.håndter(
                    Ytelser(
                        meldingsreferanseId = meldingsreferanseId,
                        aktørId = aktørId,
                        fødselsnummer = fødselsnummer,
                        organisasjonsnummer = organisasjonsnummer,
                        vedtaksperiodeId = UUID.randomUUID().toString(),
                        utbetalingshistorikk = Utbetalingshistorikk(
                            meldingsreferanseId = meldingsreferanseId,
                            aktørId = aktørId,
                            fødselsnummer = fødselsnummer,
                            organisasjonsnummer = organisasjonsnummer,
                            vedtaksperiodeId = UUID.randomUUID().toString(),
                            utbetalinger = emptyList(),
                            inntektshistorikk = emptyList(),
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

    private fun enPersonMedÉnArbeidsgiver(virksomhetsnummer: String) = person.also {
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
                opphørsdato = null,
                inntekt = 1000.månedlig
            ),
            orgnummer = virksomhetsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.månedlig,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun sykmelding(
        orgnummer: String = organisasjonsnummer,
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(16.september, 5.oktober, 100))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = perioder,
        mottatt = perioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
    )

    private fun søknad(
        perioder: List<Søknad.Søknadsperiode> = listOf(
            Søknad.Søknadsperiode.Sykdom(16.september, 5.oktober, 100)
        ),
        sendtTilNAV: LocalDateTime = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay()
    ) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = perioder,
            harAndreInntektskilder = false,
            sendtTilNAV = sendtTilNAV,
            permittert = false
        )


    private fun påminnelse(vedtaksperiodeId: UUID = vedtaksperiodeIdForPerson(), tilstandType: TilstandType) =
        Påminnelse(
            meldingsreferanseId = UUID.randomUUID(),
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
        internal val tilstandsendringer: MutableMap<UUID, PersonObserver.VedtaksperiodeEndretTilstandEvent> =
            mutableMapOf()
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
