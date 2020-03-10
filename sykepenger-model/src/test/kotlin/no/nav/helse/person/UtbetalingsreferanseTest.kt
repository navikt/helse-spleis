package no.nav.helse.person

import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
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
    private lateinit var hendelse: ArbeidstakerHendelse

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `tilstøtende periode får samme utbetalingsreferanse`() {
        håndterYtelser(fom = 1.januar, tom = 31.januar, sendtInntektsmelding = true, vedtaksperiodeindeks = 0)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(0).toString())
            .also {
                person.håndter(it)
            }
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(0), Utbetaling.Status.FERDIG))
        val utbetalingsreferanse1 = hendelse.etterspurtBehov<String>(inspektør.vedtaksperiodeId(0), Behovtype.Utbetaling, "utbetalingsreferanse")
        assertEquals(utbetalingsreferanse1, inspektør.utbetalingsreferanser[0])

        håndterYtelser(fom = 1.februar, tom = 28.februar, sendtInntektsmelding = false, vedtaksperiodeindeks = 1)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(1).toString())
            .also {
                person.håndter(it)
            }
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(1), Utbetaling.Status.FERDIG))
        val utbetalingsreferanse2 = hendelse.etterspurtBehov<String>(inspektør.vedtaksperiodeId(1), Behovtype.Utbetaling, "utbetalingsreferanse")
        assertEquals(utbetalingsreferanse2, inspektør.utbetalingsreferanser[1])

        assertEquals(utbetalingsreferanse1, utbetalingsreferanse2)
    }

    @Test
    fun `ikke-tilstøtende periode får unik utbetalingsreferanse`() {
        håndterYtelser(fom = 1.januar, tom = 31.januar, sendtInntektsmelding = true, vedtaksperiodeindeks = 0)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(0).toString())
            .also {
                person.håndter(it)
            }
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(0), Utbetaling.Status.FERDIG))
        val utbetalingsreferanse1 = hendelse.etterspurtBehov<String>(inspektør.vedtaksperiodeId(0), Behovtype.Utbetaling, "utbetalingsreferanse")

        håndterYtelser(fom = 2.februar, tom = 28.februar, sendtInntektsmelding = true, vedtaksperiodeindeks = 1)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(1).toString())
            .also {
                person.håndter(it)
            }
        person.håndter(utbetaling(inspektør.vedtaksperiodeId(1), Utbetaling.Status.FERDIG))
        val utbetalingsreferanse2 = hendelse.etterspurtBehov<String>(inspektør.vedtaksperiodeId(1), Behovtype.Utbetaling, "utbetalingsreferanse")

        assertNotEquals(utbetalingsreferanse1, utbetalingsreferanse2)
    }

    private fun håndterYtelser(
        fom: LocalDate,
        tom: LocalDate,
        sendtInntektsmelding: Boolean,
        vedtaksperiodeindeks: Int
    ) {
        person.håndter(sykmelding(fom, tom))
        person.håndter(søknad(fom, tom))
        if (sendtInntektsmelding) person.håndter(inntektsmelding(fom))
        person.håndter(vilkårsgrunnlag(inspektør.vedtaksperiodeId(vedtaksperiodeindeks)))
        person.håndter(ytelser(inspektør.vedtaksperiodeId(vedtaksperiodeindeks)))
    }

    private fun manuellSaksbehandling(godkjent: Boolean, vedtaksperiodeId: String) = ManuellSaksbehandling(
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                ukjentePerioder = emptyList(),
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
        )
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
            rapportertdato = tom.atStartOfDay()
        )

    private fun inntektsmelding(fom: LocalDate) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(16))),
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
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(
                listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            )
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

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {

        private var vedtaksperiodeindeks: Int = -1
        private val vedtaksperiodeIder = mutableSetOf<UUID>()
        internal val utbetalingsreferanser = mutableMapOf<Int, String>()

        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            vedtaksperiodeIder.add(id)
        }

        override fun visitUtbetalingsreferanse(utbetalingsreferanse: String) {
            this.utbetalingsreferanser[vedtaksperiodeindeks] = utbetalingsreferanse
        }

        internal fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)

    }
}
