package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterlevelseTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup(){
        Toggles.Etterlevelse.enable()
    }

    @AfterEach
    fun teardown(){
        Toggles.Etterlevelse.pop()
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
        assertTrue(etterlevelseInspektør.resultat("8-2", "1").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-3", "2").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-12", "1").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-30", "2").single().oppfylt)
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
        assertTrue(etterlevelseInspektør.resultat("8-2", "1").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-51", "2").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-12", "1").single().oppfylt)
        assertTrue(etterlevelseInspektør.resultat("8-30", "2").single().oppfylt)
    }

    private class EtterlevelseInspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        private val resultater = mutableListOf<Resultat>()

        val size get() = resultater.size
        fun resultat(paragraf: String, ledd: String) = resultater.filter { it.paragraf == paragraf && it.ledd == ledd }

        init {
            aktivitetslogg.accept(this)
        }

        override fun visitVurderingsresultat(oppfylt: Boolean, versjon: LocalDate, paragraf: String, ledd: String, inputdata: Map<Any, Any?>, outputdata: Map<Any, Any?>) {
            resultater.add(Resultat(oppfylt, versjon, paragraf, ledd, inputdata, outputdata))
        }
    }

    private class Resultat(
        val oppfylt: Boolean,
        val versjon: LocalDate,
        val paragraf: String,
        val ledd: String,
        val inputdata: Any?,
        val outputdata: Any?
    )
}
