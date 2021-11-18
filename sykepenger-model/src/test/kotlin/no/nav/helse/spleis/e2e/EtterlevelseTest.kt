package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Ledd.LEDD_1
import no.nav.helse.person.Ledd.LEDD_2
import no.nav.helse.person.Paragraf.*
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mai
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterlevelseTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggle.Etterlevelse.enable()
    }

    @AfterEach
    fun teardown() {
        Toggle.Etterlevelse.pop()
    }

    @Test
    fun `Sykmelding med gradering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        val etterlevelseInspektør = EtterlevelseInspektør(inspektør.personLogg)
        assertEquals(4, etterlevelseInspektør.size)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_2, LEDD_1).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_3, LEDD_2).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_12, LEDD_1).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_30, LEDD_2).single().oppfylt)
    }

    @Test
    fun `Sykmelding med gradering over 67`() {
        val eldrePersonFnr = "21023701901"
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent), fnr = eldrePersonFnr)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent), fnr = eldrePersonFnr)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode, fnr = eldrePersonFnr)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, fnr = eldrePersonFnr)
        håndterYtelser(1.vedtaksperiode, fnr = eldrePersonFnr)

        val etterlevelseInspektør = EtterlevelseInspektør(inspektør.personLogg)
        assertEquals(4, etterlevelseInspektør.size)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_2, LEDD_1).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_51, LEDD_2).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_12, LEDD_1).single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat(PARAGRAF_8_30, LEDD_2).single().oppfylt)
    }

    @Test
    fun `§8-12 ledd 1 - Brukt færre enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        val resultat = EtterlevelseInspektør(inspektør.personLogg).resultat(PARAGRAF_8_12, LEDD_1).single()
        assertEquals("§8-12 ledd 1", resultat.melding)
        assertTrue(resultat.oppfylt)
        assertEquals(21.mai(2021), resultat.versjon)
        assertEquals(19.januar, resultat.inputdata["fom"])
        assertEquals(26.januar, resultat.inputdata["tom"])

        val spleistidslinje = resultat.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().first().first()
        assertEquals(19.januar, spleistidslinje["fom"])
        assertEquals(26.januar, spleistidslinje["tom"])
        assertEquals("NAVDAG", spleistidslinje["dagtype"])

        val infotrygdtidslinje = resultat.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().last()
        assertTrue(infotrygdtidslinje.isEmpty())

        val beregnetTidslinje = resultat.inputdata["beregnetTidslinje"].castAsList<Map<String, Any>>().first()
        assertEquals(19.januar, beregnetTidslinje["fom"])
        assertEquals(26.januar, beregnetTidslinje["tom"])
        assertEquals("NAVDAG", beregnetTidslinje["dagtype"])

        assertEquals(242, resultat.outputdata["gjenståendeSykedager"])
        assertEquals(6, resultat.outputdata["forbrukteSykedager"])
        assertEquals(1.januar(2019), resultat.outputdata["maksdato"])
        assertTrue(resultat.outputdata["avvisteDager"].castAsList<Any>().isEmpty())
    }

    @Test
    fun `§8-12 ledd 1 - Brukt flere enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar(2018), 11.januar(2019), 50.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Søknad.Søknadsperiode.Sykdom(3.januar(2018), 11.januar(2019), 50.prosent, 50.prosent),
            sendtTilNav = 3.januar(2018)
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar(2018), 18.januar(2018))))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        val resultat1 = EtterlevelseInspektør(inspektør.personLogg).resultat(PARAGRAF_8_12, LEDD_1).first()
        assertEquals("§8-12 ledd 1", resultat1.melding)
        assertTrue(resultat1.oppfylt)
        assertEquals(21.mai(2021), resultat1.versjon)
        assertEquals(19.januar(2018), resultat1.inputdata["fom"])
        assertEquals(1.januar(2019), resultat1.inputdata["tom"])

        val spleistidslinje1 = resultat1.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().first().first()
        assertEquals(19.januar(2018), spleistidslinje1["fom"])
        assertEquals(11.januar(2019), spleistidslinje1["tom"])
        assertEquals("NAVDAG", spleistidslinje1["dagtype"])

        val infotrygdtidslinje1 = resultat1.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().last()
        assertTrue(infotrygdtidslinje1.isEmpty())

        val beregnetTidslinje1 = resultat1.inputdata["beregnetTidslinje"].castAsList<Map<String, Any>>().first()
        assertEquals(19.januar(2018), beregnetTidslinje1["fom"])
        assertEquals(11.januar(2019), beregnetTidslinje1["tom"])
        assertEquals("NAVDAG", beregnetTidslinje1["dagtype"])

        assertEquals(0, resultat1.outputdata["gjenståendeSykedager"])
        assertEquals(248, resultat1.outputdata["forbrukteSykedager"])
        assertEquals(1.januar(2019), resultat1.outputdata["maksdato"])
        assertTrue(resultat1.outputdata["avvisteDager"].castAsList<Any>().isEmpty())

        val resultat2 = EtterlevelseInspektør(inspektør.personLogg).resultat(PARAGRAF_8_12, LEDD_1).last()
        assertEquals("§8-12 ledd 1", resultat2.melding)
        assertFalse(resultat2.oppfylt)
        assertEquals(21.mai(2021), resultat2.versjon)
        assertEquals(2.januar(2019), resultat2.inputdata["fom"])
        assertEquals(11.januar(2019), resultat2.inputdata["tom"])

        val spleistidslinje2 = resultat2.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().first().first()
        assertEquals(19.januar(2018), spleistidslinje2["fom"])
        assertEquals(11.januar(2019), spleistidslinje2["tom"])
        assertEquals("NAVDAG", spleistidslinje2["dagtype"])

        val infotrygdtidslinje2 = resultat2.inputdata["tidslinjegrunnlag"].castAsList<List<Map<String, Any>>>().last()
        assertTrue(infotrygdtidslinje2.isEmpty())

        val beregnetTidslinje2 = resultat2.inputdata["beregnetTidslinje"].castAsList<Map<String, Any>>().first()
        assertEquals(19.januar(2018), beregnetTidslinje2["fom"])
        assertEquals(11.januar(2019), beregnetTidslinje2["tom"])
        assertEquals("NAVDAG", beregnetTidslinje2["dagtype"])

        assertEquals(0, resultat2.outputdata["gjenståendeSykedager"])
        assertEquals(248, resultat2.outputdata["forbrukteSykedager"])
        assertEquals(1.januar(2019), resultat2.outputdata["maksdato"])
        assertEquals(
            listOf(2.januar(2019) til 4.januar(2019), 7.januar(2019) til 11.januar(2019)),
            resultat2.outputdata["avvisteDager"].castAsList<Periode>()
        )
    }

    private class EtterlevelseInspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        private val resultater = mutableListOf<Resultat>()

        val size get() = resultater.size
        fun resultat(paragraf: Paragraf, ledd: Ledd) = resultater.filter { it.paragraf == paragraf && it.ledd == ledd }

        init {
            aktivitetslogg.accept(this)
        }

        private lateinit var melding: String

        override fun preVisitEtterlevelse(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Etterlevelse,
            melding: String,
            vurderingsresultat: Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat,
            tidsstempel: String
        ) {
            this.melding = melding
        }

        override fun visitVurderingsresultat(
            oppfylt: Boolean,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd,
            punktum: Punktum,
            inputdata: Map<Any, Any?>,
            outputdata: Map<Any, Any?>
        ) {
            resultater.add(Resultat(melding, oppfylt, versjon, paragraf, ledd, punktum, inputdata, outputdata))
        }
    }

    private class Resultat(
        val melding: String,
        val oppfylt: Boolean,
        val versjon: LocalDate,
        val paragraf: Paragraf,
        val ledd: Ledd,
        val punktum: Punktum,
        val inputdata: Map<Any, Any?>,
        val outputdata: Map<Any, Any?>
    )
}
