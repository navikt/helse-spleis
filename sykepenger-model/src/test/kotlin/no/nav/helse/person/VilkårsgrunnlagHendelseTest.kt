package no.nav.helse.person

import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.TestPersonInspektør
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `egen ansatt`() {
        håndterVilkårsgrunnlag(
            egenAnsatt = true,
            inntekter = tolvMånederMedInntekt(1000.0),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ingen inntekt`() {
        håndterVilkårsgrunnlag(egenAnsatt = false, inntekter = emptyMap(), arbeidsforhold = ansattSidenStart2017())
        assertTrue(person.aktivitetslogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `avvik i inntekt`() {
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            inntekter = tolvMånederMedInntekt(1.0),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ikke egen ansatt og ingen avvik i inntekt`() {
        val månedslønn = 1000.0
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = månedslønn,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_HISTORIKK, inspektør.sisteTilstand(0))
        val historikkFom = inspektør.sykdomstidslinje(0).førsteDag().minusYears(4)
        val historikkTom = inspektør.sykdomstidslinje(0).sisteDag()
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(0)
        assertEquals(
            historikkFom.toString(),
            hendelse.etterspurtBehov(vedtaksperiodeId, Behovtype.Sykepengehistorikk, "historikkFom")
        )
        assertEquals(
            historikkTom.toString(),
            hendelse.etterspurtBehov(vedtaksperiodeId, Behovtype.Sykepengehistorikk, "historikkTom")
        )
    }

    @Test
    fun `benytter forrige måned som utgangspunkt for inntektsberegning`() {
        person.håndter(sykmelding(perioder = listOf(Triple(8.januar, 31.januar, 100))))
        person.håndter(søknad(perioder = listOf(Søknad.Søknadsperiode.Sykdom(8.januar,  31.januar, 100))))
        person.håndter(inntektsmelding(beregnetInntekt = 30000.0, arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar))))
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(0)

        val inntektsberegningStart = hendelse.etterspurtBehov<String>(vedtaksperiodeId, Behovtype.Inntektsberegning, "beregningStart")
        val inntektsberegningSlutt = hendelse.etterspurtBehov<String>(vedtaksperiodeId, Behovtype.Inntektsberegning, "beregningSlutt")
        assertEquals("2017-01", inntektsberegningStart)
        assertEquals("2017-12", inntektsberegningSlutt)
    }

    @Test
    fun `ikke egen ansatt og mer enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0
        val `25 % mer` = månedslønn * 1.25 + 1
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mer`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ikke egen ansatt og mindre enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0
        val `25 % mindre` = månedslønn * 0.75 - 1
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mindre`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    private fun ansattSidenStart2017() =
        listOf(Opptjeningvurdering.Arbeidsforhold(ORGNR, 1.januar(2017)))


    private fun tolvMånederMedInntekt(beregnetInntekt: Double) =
        (1..12).map { YearMonth.of(2018, it) to (ORGNR to beregnetInntekt) }
            .groupBy({ it.first }) { it.second }

    private fun håndterVilkårsgrunnlag(
        egenAnsatt: Boolean,
        beregnetInntekt: Double = 1000.0,
        inntekter: Map<YearMonth, List<Pair<String?, Double>>>,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold>
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(beregnetInntekt = beregnetInntekt))
        person.håndter(vilkårsgrunnlag(egenAnsatt = egenAnsatt, inntekter = inntekter, arbeidsforhold = arbeidsforhold))
    }

    private fun sykmelding(
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(1.januar, 31.januar, 100))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "aktørId",
        orgnummer = ORGNR,
        sykeperioder = perioder
    ).apply {
        hendelse = this
    }

    private fun søknad(
        perioder: List<Søknad.Søknadsperiode> = listOf(Søknad.Søknadsperiode.Sykdom(1.januar,  31.januar, 100))
    ) = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            perioder = perioder,
            harAndreInntektskilder = false,
            sendtTilNAV = 31.januar.atStartOfDay(),
            permittert = false
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding(
        beregnetInntekt: Double,
        arbeidsgiverperioder: List<Periode> = listOf(Periode(1.januar, 16.januar))
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, beregnetInntekt, emptyList()),
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(
        egenAnsatt: Boolean,
        inntekter: Map<YearMonth, List<Pair<String?, Double>>>,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold>
    ) =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            inntektsvurdering = Inntektsvurdering(inntekter),
            erEgenAnsatt = egenAnsatt,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelse = this
        }
}
