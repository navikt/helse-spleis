package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class UtbetalingsreferanseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "12345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `utbetalingsreferanse blir generert når noe skal utbetales`() {
        person.håndter(sykmelding(1.januar, 18.januar))
        person.håndter(søknad(1.januar, 18.januar))
        person.håndter(inntektsmelding(1.januar, 16.januar))
        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(0)))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))
        assertNotNull(inspektør.utbetalingsreferanse(0))
        assertTrue(inspektør.utbetalingslinjer(0).isNotEmpty())
    }

    @Test
    fun `utbetalingsreferanse blir ikke generert når ingenting skal utbetales`() {
        person.håndter(sykmelding(1.januar, 26.januar))
        person.håndter(søknad(1.januar, 26.januar))
        person.håndter(inntektsmelding(1.januar, 16.januar, listOf(Periode(17.januar, 26.januar))))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))
        assertNull(inspektør.utbetalingsreferanse(0))
        assertTrue(inspektør.utbetalingslinjer(0).isEmpty())
    }

    @Test
    fun `utbetalingsreferanse blir kopiert fra tilstøtende periode med utbetaling`() {
        person.håndter(sykmelding(1.januar, 26.januar))
        person.håndter(sykmelding(29.januar, 28.februar))
        person.håndter(søknad(1.januar, 26.januar))
        person.håndter(søknad(29.januar, 28.februar))
        person.håndter(inntektsmelding(1.januar, 16.januar))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))
        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(0)))
        person.håndter(simulering(inspektør.vedtaksperiodeId(0)))
        person.håndter(godkjenning(inspektør.vedtaksperiodeId(0)))
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(0), UtbetalingHendelse.Oppdragstatus.AKSEPTERT))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(1)))
        assertEquals(inspektør.utbetalingsreferanse(1), inspektør.utbetalingsreferanse(0))
    }

    @Test
    fun `utbetalingsreferanse blir ikke kopiert fra tilstøtende periode uten utbetaling`() {
        person.håndter(sykmelding(1.januar, 26.januar))
        person.håndter(sykmelding(29.januar, 28.februar))
        person.håndter(søknad(1.januar, 26.januar))
        person.håndter(søknad(29.januar, 28.februar))
        person.håndter(inntektsmelding(1.januar, 16.januar, listOf(Periode(17.januar, 26.januar))))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))
        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(0)))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))
        person.håndter(simulering(inspektør.vedtaksperiodeId(0)))
        person.håndter(godkjenning(inspektør.vedtaksperiodeId(0)))
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(0), UtbetalingHendelse.Oppdragstatus.AKSEPTERT))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(1)))
        assertNotEquals(inspektør.utbetalingsreferanse(1), inspektør.utbetalingsreferanse(0))
    }

    private fun sykmelding(fom: LocalDate, tom: LocalDate) =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(fom, tom, 100))
        )

    private fun søknad(fom: LocalDate, tom: LocalDate) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(fom, tom, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = tom.atStartOfDay()
        )

    private fun inntektsmelding(fom: LocalDate, tom: LocalDate, ferieperioder: List<Periode> = emptyList()) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(fom, tom)),
            ferieperioder = ferieperioder
        )

    private fun vilkårsgrunnlag(vedtaksperiodeId: UUID) =
        Vilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2018, it), listOf(31000.0)
                )
            },
            erEgenAnsatt = false,
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    orgnummer,
                    1.januar(2017)
                )
            )
        )

    private fun ytelser(vedtaksperiodeId: UUID) =
        Ytelser(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            meldingsreferanseId = UUID.randomUUID(),
            utbetalingshistorikk = Utbetalingshistorikk(
                utbetalinger = emptyList(),
                inntektshistorikk = emptyList(),
                graderingsliste = emptyList(),
                maksDato = null,
                aktivitetslogg = Aktivitetslogg()
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = null,
                svangerskapsytelse = null,
                aktivitetslogg = Aktivitetslogg()
            ),
            aktivitetslogg = Aktivitetslogg()
        )

    private fun simulering(vedtaksperiodeId: UUID) =
        Simulering(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        )

    private fun godkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true) =
        ManuellSaksbehandling(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            utbetalingGodkjent = godkjent,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandler = ""
        )

    private fun utbetaling(vedtaksperiodeId: UUID, status: UtbetalingHendelse.Oppdragstatus) =
        UtbetalingHendelse(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        )
}
