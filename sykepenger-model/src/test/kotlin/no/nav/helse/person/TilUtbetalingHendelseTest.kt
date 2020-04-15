package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class TilUtbetalingHendelseTest {
    companion object {
        private const val aktørId = "aktørId"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    private var utbetaltEvent: PersonObserver.UtbetaltEvent? = null
    private val utbetalingObserver = object : PersonObserver {
        override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
            utbetaltEvent = event
        }
    }

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(utbetalingObserver)
        utbetaltEvent = null
    }

    @Test
    fun `utbetaling er godkjent`() {
        håndterGodkjenning()
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AKSEPTERT))
        assertTilstand(TilstandType.AVSLUTTET)

        assertEquals(UNG_PERSON_FNR_2018, utbetaltEvent?.fødselsnummer)
        assertEquals(førsteSykedag, utbetaltEvent?.førsteFraværsdag)
        assertEquals(1, utbetaltEvent?.utbetalingslinjer?.size)
        assertEquals(1, utbetaltEvent?.utbetalingslinjer?.get(0)?.utbetalingslinjer?.size)
        assertEquals(17.januar, utbetaltEvent?.utbetalingslinjer?.get(0)?.utbetalingslinjer?.get(0)?.fom)
        assertEquals(31.januar, utbetaltEvent?.utbetalingslinjer?.get(0)?.utbetalingslinjer?.get(0)?.tom)
        assertEquals(1431, utbetaltEvent?.utbetalingslinjer?.get(0)?.utbetalingslinjer?.get(0)?.dagsats)
        assertEquals(100.0, utbetaltEvent?.utbetalingslinjer?.get(0)?.utbetalingslinjer?.get(0)?.grad)
    }

    @Test
    fun `utbetaling ikke godkjent`() {
        håndterGodkjenning()
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AVVIST))
        assertTilstand(TilstandType.UTBETALING_FEILET)
        assertNull(utbetaltEvent)
    }

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.sisteTilstand(0)
        )
    }

    private fun håndterGodkjenning() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(simulering())
        person.håndter(manuellSaksbehandling(true))
    }

    private fun utbetaling(status: UtbetalingHendelse.Oppdragstatus) =
        UtbetalingHendelse(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        ).apply {
            hendelse = this
        }

    private fun manuellSaksbehandling(godkjent: Boolean) = ManuellSaksbehandling(
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        vedtaksperiodeId: UUID = inspektør.vedtaksperiodeId(0),
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                utbetalinger = utbetalinger,
                inntektshistorikk = emptyList(),
                graderingsliste = emptyList(),
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
                aktivitetslogg = it
            ),
            aktivitetslogg = it
        ).apply {
            hendelse = this
        }
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(førsteSykedag, sisteSykedag, 100))
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(førsteSykedag, sisteSykedag, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = Vilkårsgrunnlag.Inntektsvurdering((1..12)
                .map { YearMonth.of(2018, it) to 31000.0 }
                .groupBy({ it.first }) { it.second }),
            erEgenAnsatt = false,
            opptjeningvurdering = Vilkårsgrunnlag.Opptjeningvurdering(listOf(Vilkårsgrunnlag.Opptjeningvurdering.Arbeidsforhold(orgnummer, 1.januar(2017)))),
            dagpenger = Vilkårsgrunnlag.Dagpenger(emptyList()),
            arbeidsavklaringspenger = Vilkårsgrunnlag.Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelse = this
        }

    private fun simulering() =
        Simulering(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        ).apply {
            hendelse = this
        }
}
