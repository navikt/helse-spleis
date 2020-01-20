package no.nav.helse

import no.nav.helse.behov.Behov
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class PersonInStateTestHelper(
    private val aktørId:String = "id",
    private val fødselsnummer:String = "01017000000",
    private val organisasjonsnummer:String = "12") {

    private var testObserver = TestPersonObserver()

    fun finnBehov(type: String) : Behov? {
        return testObserver.behovsliste.findLast { it.behovType().contains(type) }
    }

    fun personInState(ønsketTilstandType: TilstandType = TilstandType.TIL_UTBETALING) : Person {
        testObserver = TestPersonObserver()
        val testPerson = Person(aktørId = aktørId, fødselsnummer = fødselsnummer).also {
            it.addObserver(testObserver)
        }

        testPerson.håndter(
            TestConstants.nySøknadHendelse(
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 30.juli, sykmeldingsgrad = 100)),
                egenmeldinger = emptyList(),
                fravær = emptyList()
            )
        )

        assertEquals(TilstandType.MOTTATT_NY_SØKNAD, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.MOTTATT_NY_SØKNAD) return testPerson

        testPerson.håndter(
            ModelInntektsmelding(
                hendelseId = UUID.randomUUID(),
                refusjon = ModelInntektsmelding.Refusjon(null, 30000.00, emptyList()),
                orgnummer = organisasjonsnummer,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                mottattDato = LocalDateTime.now(),
                førsteFraværsdag = 1.juli,
                beregnetInntekt = 30000.00,
                aktivitetslogger = Aktivitetslogger(),
                originalJson = "{}",
                arbeidsgiverperioder = listOf(1.juli..1.juli.plusDays(16)),
                ferieperioder = emptyList()
            )
        )

        assertEquals(TilstandType.MOTTATT_INNTEKTSMELDING, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.MOTTATT_INNTEKTSMELDING) return testPerson


        val sendtSøknad = ModelSendtSøknad(
            UUID.randomUUID(),
            fødselsnummer,
            aktørId,
            organisasjonsnummer,
            LocalDateTime.now(),
            listOf(ModelSendtSøknad.Periode.Sykdom(1.juli, 30.juli, 100)),
            Aktivitetslogger(),
            SykepengesoknadDTO(
                id = "123",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.SENDT,
                aktorId = aktørId,
                fnr = fødselsnummer,
                sykmeldingId = UUID.randomUUID().toString(),
                arbeidsgiver = ArbeidsgiverDTO(
                    "Hello world",
                    organisasjonsnummer
                ),
                fom = 10.januar,
                tom = 12.januar,
                opprettet = LocalDateTime.now(),
                sendtNav = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                soknadsperioder = listOf(
                    SoknadsperiodeDTO(1.juli, 30.juli, 100)
                ),
                fravar = emptyList()
            ).toJsonNode().toString()
        )
        testPerson.håndter(sendtSøknad)

        assertEquals(TilstandType.VILKÅRSPRØVING, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.VILKÅRSPRØVING) return testPerson

        val inntektsBehov = finnBehov("Inntektsberegning")!!

        val vedtaksperiodeId = inntektsBehov.vedtaksperiodeId()
        val vilkårsgrunnlag = Vilkårsgrunnlag.Builder().build(
            inntektsBehov.løsBehov(
                mapOf(
                    "EgenAnsatt" to false,
                    "Inntektsberegning" to (1.rangeTo(12)).map {
                        Vilkårsgrunnlag.Måned(
                            årMåned = YearMonth.of(2018, it),
                            inntektsliste = listOf(
                                Vilkårsgrunnlag.Inntekt(
                                    beløp = 30050.0,
                                    inntektstype = Vilkårsgrunnlag.Inntektstype.LOENNSINNTEKT,
                                    orgnummer = organisasjonsnummer
                                )
                            )
                        )
                    }
                )
            ).toJson()
        )!!
        testPerson.håndter(vilkårsgrunnlag)

        assertEquals(TilstandType.BEREGN_UTBETALING, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.BEREGN_UTBETALING) return testPerson

        val ytelser: ModelYtelser = TestConstants.ytelser(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = UUID.fromString(vedtaksperiodeId),
            sykepengehistorikk = TestConstants.sykepengehistorikk(
                perioder = listOf()
            )
        )
        testPerson.håndter(ytelser)

        assertEquals(TilstandType.TIL_GODKJENNING, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.TIL_GODKJENNING) return testPerson

        val godkjenningsbehov = finnBehov("GodkjenningFraSaksbehandler")!!

        val godkjenning = ManuellSaksbehandling.Builder().build(
            godkjenningsbehov.apply {
                this["saksbehandlerIdent"] = "Zorro"
            }.løsBehov(
                mapOf(
                    "GodkjenningFraSaksbehandler" to mapOf(
                        "godkjent" to true
                    )
                )
            ).toJson()
        )!!

        testPerson.håndter(godkjenning)

        assertEquals(TilstandType.TIL_UTBETALING, testPerson.tilstand())
        if (ønsketTilstandType == TilstandType.TIL_UTBETALING) return testPerson

        assertEquals("etwas anders", ønsketTilstandType)
        return testPerson
    }

    private fun Person.tilstand() : TilstandType {
        var tilstandType:TilstandType? = null
        this.accept(object : PersonVisitor {
            override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
                tilstandType = tilstand.type
            }
        })
        return tilstandType!!
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
