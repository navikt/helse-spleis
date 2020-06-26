package no.nav.helse.hendelser

import no.nav.helse.hendelser.Vilkårsgrunnlag.Grunnlagsdata
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagTest {
    private companion object {
        private const val aktørId = "123"
        private const val fødselsnummer = "234"
        private const val orgnummer = "345"
        private const val INNTEKT = 1000.0
    }

    private lateinit var person: Person

    @BeforeEach
    fun setup() {
        person = Person(aktørId, fødselsnummer).apply {
            håndter(sykmelding())
            håndter(søknad())
            håndter(inntektsmelding())
        }
    }

    @Test
    internal fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag()
        person.håndter(vilkårsgrunnlag)
        assertEquals(0.0, dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(12000.0, dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    internal fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { YearMonth.of(2017, it) to (orgnummer to 1250.0) }.groupBy({ it.first }) { it.second }
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0.20, dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(15000.00, dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(false, dataForVilkårsvurdering()?.erEgenAnsatt)
        assertEquals(28, dataForVilkårsvurdering()!!.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
    }

    @Test
    internal fun `27 dager opptjening fører til at vilkårsvurdering feiler`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, 5.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(27, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    internal fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, førsteFraværsdag().plusDays(1)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    internal fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
    }

    @Test
    internal fun `arbeidsforhold kun for andre orgnr gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    internal fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = emptyList()
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    private fun førsteFraværsdag(): LocalDate {
        var _førsteFraværsdag: LocalDate? = null
        person.accept(object : PersonVisitor {
            override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
                _førsteFraværsdag = førsteFraværsdag
            }
        })
        return requireNotNull(_førsteFraværsdag)
    }


    private fun dataForVilkårsvurdering(): Grunnlagsdata? {
        var _dataForVilkårsvurdering: Grunnlagsdata? = null
        person.accept(object : PersonVisitor {
            override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Grunnlagsdata?) {
                _dataForVilkårsvurdering = dataForVilkårsvurdering
            }
        })
        return _dataForVilkårsvurdering
    }

    private fun hentTilstand(): Vedtaksperiodetilstand? {
        var _tilstand: Vedtaksperiodetilstand? = null
        person.accept(object : PersonVisitor {
            override fun visitTilstand(tilstand: Vedtaksperiodetilstand) {
                _tilstand = tilstand
            }
        })
        return _tilstand
    }

    private fun vedtaksperiodeId(): String {
        lateinit var _id: UUID
        person.accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                periode: Periode,
                hendelseIder: List<UUID>
            ) {
                _id = id
            }
        })
        return _id.toString()
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: Map<YearMonth, List<Pair<String?, Double>>> = (1..12).map {
            YearMonth.of(2017, it) to (orgnummer to INNTEKT)
        }.groupBy({ it.first }) { it.second },
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = listOf(
            Opptjeningvurdering.Arbeidsforhold(
                orgnummer,
                4.desember(2017)
            )
        ),
        dagpenger: List<Periode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList()

    ) = Vilkårsgrunnlag(
        vedtaksperiodeId = vedtaksperiodeId(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(inntektsmåneder),
        opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
        erEgenAnsatt = false,
        dagpenger = Dagpenger(dagpenger),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(
            arbeidsavklaringspenger
        )
    )

    private fun sykmelding() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(16.januar, 30.januar, 100)),
        mottatt = 1.april.atStartOfDay()
    )

    private fun søknad() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(Søknad.Søknadsperiode.Sykdom(16.januar, 30.januar, 100)),
        harAndreInntektskilder = false,
        sendtTilNAV = 30.januar.atStartOfDay(),
        permittert = false
    )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, INNTEKT, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.0,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
}
