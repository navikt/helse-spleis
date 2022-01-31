package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.*
import no.nav.helse.person.Bokstav.BOKSTAV_A
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Ledd.LEDD_2
import no.nav.helse.person.Ledd.LEDD_3
import no.nav.helse.person.Paragraf.*
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.util.*

internal class EtterlevelseTest : AbstractEndToEndTest() {

    @Test
    fun `§8-12 ledd 2 - Bruker har ikke vært arbeidsfør i 26 uker`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)


        håndterSykmelding(Sykmeldingsperiode(16.juli, 31.august, 50.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(16.juli, 31.august, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(16.juli, 31.juli)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertIkkeVurdert(paragraf = PARAGRAF_8_12, ledd = LEDD_2)
    }

    private val Int.resultat: (List<Resultat>) -> Resultat get() = { it[this - 1] }

    private fun assertOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav> = emptyList(),
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver).let(resultatvelger)
        assertTrue(resultat.oppfylt) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertIkkeOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav> = emptyList(),
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver).let(resultatvelger)
        assertFalse(resultat.oppfylt) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstaver: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver, vedtaksperiodeId?.id(organisasjonsnummer))
        assertTrue(resultat.isNotEmpty()) { "Forventet at $paragraf $ledd $punktum er vurdert" }
    }

    private fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstav: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstav, vedtaksperiodeId?.id(organisasjonsnummer))
        assertTrue(resultat.isEmpty()) { "Forventet at $paragraf $ledd $punktum ikke er vurdert" }
    }

    private fun assertResultat(
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultat: Resultat
    ) {
        assertEquals(versjon, resultat.versjon)
        assertEquals(inputdata, resultat.inputdata)
        assertEquals(outputdata, resultat.outputdata)
    }

    private class EtterlevelseInspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        private val resultater = mutableListOf<Resultat>()

        fun resultat(paragraf: Paragraf, ledd: Ledd?, punktum: List<Punktum>?, bokstav: List<Bokstav>?, vedtaksperiodeId: UUID? = null) =
            resultater.filter {
                it.paragraf == paragraf
                    && ledd?.equals(it.ledd) ?: true
                    && punktum?.equals(it.punktum) ?: true
                    && bokstav?.equals(it.bokstaver) ?: true
                    && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraKontekst()) ?: true
            }

        init {
            aktivitetslogg.accept(this)
        }

        private lateinit var melding: String
        private lateinit var kontekster: List<SpesifikkKontekst>

        override fun preVisitEtterlevelse(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Etterlevelse,
            melding: String,
            vurderingsresultat: Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat,
            tidsstempel: String
        ) {
            this.kontekster = kontekster
            this.melding = melding
        }

        override fun visitVurderingsresultat(
            oppfylt: Boolean,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd,
            punktum: List<Punktum>,
            bokstaver: List<Bokstav>,
            outputdata: Map<Any, Any?>,
            inputdata: Map<Any, Any?>
        ) {
            resultater.add(Resultat(melding, oppfylt, versjon, paragraf, ledd, punktum, bokstaver, inputdata, outputdata, kontekster))
        }
    }

    private class Resultat(
        val melding: String,
        val oppfylt: Boolean,
        val versjon: LocalDate,
        val paragraf: Paragraf,
        val ledd: Ledd,
        val punktum: List<Punktum>,
        val bokstaver: List<Bokstav>,
        val inputdata: Map<Any, Any?>,
        val outputdata: Map<Any, Any?>,
        val kontekster: List<SpesifikkKontekst>
    ) {
        fun vedtaksperiodeIdFraKontekst(): UUID = UUID.fromString(kontekster.first { it.kontekstType == "Vedtaksperiode" }.kontekstMap["vedtaksperiodeId"])
    }
}
