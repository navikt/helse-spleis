package no.nav.helse.hendelser

import no.nav.helse.hendelser.ModelVilkårsgrunnlag.*
import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class ModelVilkårsgrunnlagTest {
    private val aktivitetslogger = Aktivitetslogger()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val aktørId = "123"
    private val fødselsnummer = "234"
    private val orgnummer = "345"
    private val vedtaksperiodeMediator = object : VedtaksperiodeMediator {}
    private val arbeidsgiver = Arbeidsgiver(vedtaksperiodeMediator, orgnummer)
    private val person = Person(aktørId, fødselsnummer)

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1000.0))) })

        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1000.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.01))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(749.99))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(750.00))

    }

    @Test
    internal fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1000.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        assertEquals(0.0, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(12000.0, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    internal fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0.20, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(15000.00, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.erEgenAnsatt)
        assertEquals(28, dataForVilkårsvurdering(vedtaksperiode)!!.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
    }

    @Test
    internal fun `27 dager opptjening fører til manuell saksbehandling`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) },
            listOf(Arbeidsforhold("orgnummer", 5.desember(2017)))
        )

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(27, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)

    }
    @Test
    internal fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) },
            listOf(Arbeidsforhold("orgnummer", 4.desember(2017)))
        )

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(28, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand(vedtaksperiode)?.type)
    }

    @Test
    internal fun `arbeidsforhold kun for andre orgnr gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) },
            listOf(Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)
    }

    @Test
    internal fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) },
            emptyList()
        )
        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)
    }

    private fun dataForVilkårsvurdering(vedtaksperiode: Vedtaksperiode): Grunnlagsdata? {
        var _dataForVilkårsvurdering: Grunnlagsdata? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Grunnlagsdata?) {
                _dataForVilkårsvurdering = dataForVilkårsvurdering
            }
        })
        return _dataForVilkårsvurdering
    }

    private fun hentTilstand(vedtaksperiode: Vedtaksperiode): Vedtaksperiodetilstand? {
        var _tilstand: Vedtaksperiodetilstand? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitTilstand(tilstand: Vedtaksperiodetilstand) {
                _tilstand = tilstand
            }
        })
        return _tilstand
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<Måned>,
        arbeidsforhold: List<Arbeidsforhold> = listOf(
            Arbeidsforhold(
                "orgnummer",
                4.desember(2017)
            )
        )
    ) = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        aktørId = "987654321",
        fødselsnummer = "12345678901",
        orgnummer = "orgnummer",
        rapportertDato = LocalDateTime.now(),
        inntektsmåneder = inntektsmåneder,
        arbeidsforhold = arbeidsforhold,
        erEgenAnsatt = false,
        aktivitetslogger = aktivitetslogger
    )

    private fun vedtaksperiode() =
        Vedtaksperiode(
            director = vedtaksperiodeMediator,
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer
        ).also {
            it.håndter(nySøknad())
            it.håndter(sendtSøknad(), arbeidsgiver, person)
            it.håndter(inntektsmelding())
        }

    private fun nySøknad() = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger
    )

    private fun sendtSøknad() = ModelSendtSøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sendtNav = LocalDateTime.now(),
        perioder = listOf(ModelSendtSøknad.Periode.Sykdom(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger,
        harAndreInntektskilder = false
    )

    private fun inntektsmelding() =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(16.januar, 1000.0),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            mottattDato = 20.januar.atTime(12, 30),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.0,
            aktivitetslogger = aktivitetslogger,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf()
        )
}
