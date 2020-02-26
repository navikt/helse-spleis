package no.nav.helse.person

import no.nav.helse.behov.BehovType
import no.nav.helse.behov.partisjoner
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
    fun `tilstøtende periode får samme utbetalingsreferanse`() {
        val utbetalingObserver = UtbetalingObserver()

        håndterYtelser(fom = 1.januar, tom = 31.januar, sendtInntektsmelding = true, vedtaksperiodeindeks = 0)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(0).toString())
            .also {
                it.addObserver(utbetalingObserver)
                person.håndter(it)
            }

        håndterYtelser(fom = 1.februar, tom = 28.februar, sendtInntektsmelding = false, vedtaksperiodeindeks = 1)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(1).toString())
            .also {
                it.addObserver(utbetalingObserver)
                person.håndter(it)
            }

        assertEquals(2, utbetalingObserver.referanser.size)
        assertEquals(
            utbetalingObserver.referanser[inspektør.vedtaksperiodeId(0)],
            utbetalingObserver.referanser[inspektør.vedtaksperiodeId(1)]
        )
    }

    @Test
    fun `ikke-tilstøtende periode får unik utbetalingsreferanse`() {
        val utbetalingObserver = UtbetalingObserver()

        håndterYtelser(fom = 1.januar, tom = 31.januar, sendtInntektsmelding = true, vedtaksperiodeindeks = 0)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(0).toString())
            .also {
                it.addObserver(utbetalingObserver)
                person.håndter(it)
            }

        håndterYtelser(fom = 2.februar, tom = 28.februar, sendtInntektsmelding = true, vedtaksperiodeindeks = 1)
        manuellSaksbehandling(true, inspektør.vedtaksperiodeId(1).toString())
            .also {
                it.addObserver(utbetalingObserver)
                person.håndter(it)
            }

        assertEquals(2, utbetalingObserver.referanser.size)
        assertNotEquals(
            utbetalingObserver.referanser[inspektør.vedtaksperiodeId(0)],
            utbetalingObserver.referanser[inspektør.vedtaksperiodeId(1)]
        )
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
        aktivitetslogg = Aktivitetslogg()
    )

    private fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        utbetalingshistorikk = Utbetalingshistorikk(
            ukjentePerioder = emptyList(),
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            aktivitetslogg = Aktivitetslogg()
        ),
        foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = foreldrepengeYtelse,
            svangerskapsytelse = svangerskapYtelse,
            aktivitetslogg = Aktivitetslogg()
        ),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun sykmelding(fom: LocalDate, tom: LocalDate) =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(fom, tom, 100)),
            aktivitetslogg = Aktivitetslogg()
        )

    private fun søknad(fom: LocalDate, tom: LocalDate) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(fom, tom, 100)),
            aktivitetslogg = Aktivitetslogg(),
            harAndreInntektskilder = false
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
            ferieperioder = emptyList(),
            aktivitetslogg = Aktivitetslogg()
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
            aktivitetslogg = Aktivitetslogg(),
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(
                listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            )
        )

    private inner class TestPersonInspektør(person: Person) : PersonVisitor {

        private var vedtaksperiodeindeks: Int = -1
        private val vedtaksperiodeIder = mutableSetOf<UUID>()
        init {
            person.accept(this)
        }

        override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
            vedtaksperiodeindeks += 1
            vedtaksperiodeIder.add(id)
        }

        internal fun vedtaksperiodeId(vedtaksperiodeindeks: Int) = vedtaksperiodeIder.elementAt(vedtaksperiodeindeks)

    }

    private class UtbetalingObserver : HendelseObserver {
        internal val referanser = mutableMapOf<UUID, String>()

        override fun onBehov(behov: BehovType) {
            if (behov !is BehovType.Utbetaling) return
            listOf(behov).partisjoner().first().also {
                referanser[it.getValue("vedtaksperiodeId") as UUID] = it.getValue("utbetalingsreferanse") as String
            }
        }
    }
}
