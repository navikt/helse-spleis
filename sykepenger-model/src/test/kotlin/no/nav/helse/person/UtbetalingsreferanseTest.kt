package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
    fun `direkte tilstøtende perioder får samme utbetalingsreferanse`() {
        person.håndter(sykmelding(1.januar, 18.januar))
        person.håndter(sykmelding(19.januar, 31.januar))
        assertEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(1))
    }

    @Test
    fun `tilstøtende perioder over helg får samme utbetalingsreferanse`() {
        person.håndter(sykmelding(1.januar, 19.januar))
        person.håndter(sykmelding(22.januar, 31.januar))
        assertEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(1))
    }

    @Test
    fun `perioder som blir tilstøtende får samme utbetalingsreferanse`() {
        person.håndter(sykmelding(1.januar, 19.januar))
        person.håndter(søknad(1.januar, 19.januar))
        person.håndter(inntektsmelding(1.januar, 17.januar))
        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(0)))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(0)))

        person.håndter(sykmelding(1.februar, 14.februar))

        person.håndter(sykmelding(22.januar, 31.januar))
        person.håndter(søknad(22.januar, 31.januar))

        person.håndter(søknad(1.februar, 14.februar))
        person.håndter(inntektsmelding(1.februar, 14.februar))

        assertEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(1))
        assertNotEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(2))

        person.håndter(simulering(inspektør.vedtaksperiodeId(0)))
        person.håndter(godkjenning(inspektør.vedtaksperiodeId(0), true))
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(0), Utbetaling.Status.FERDIG))

        person.håndter(ytelser(inspektør.vedtaksperiodeId(1)))
        person.håndter(simulering(inspektør.vedtaksperiodeId(1)))
        person.håndter(godkjenning(inspektør.vedtaksperiodeId(1), true))
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(1), Utbetaling.Status.FERDIG))

        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(2)))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(2)))

        assertEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(1))
        assertEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(2))
    }

    @Test
    fun `ikke-tilstøtende perioder får ulik utbetalingsreferanse`() {
        person.håndter(sykmelding(1.januar, 19.januar))
        person.håndter(sykmelding(23.januar, 31.januar))
        assertNotEquals(inspektør.utbetalingsreferanse(0), inspektør.utbetalingsreferanse(1))
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
            perioder = listOf(Søknad.Periode.Sykdom(fom,  tom, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = tom.atStartOfDay()
        )

    private fun inntektsmelding(fom: LocalDate, tom: LocalDate) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(fom, tom)),
            ferieperioder = emptyList()
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

    private fun utbetaling(vedtaksperiodeId: UUID, status: Utbetaling.Status) =
        Utbetaling(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        )
}
