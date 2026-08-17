package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.opptjening.application.OpptjeningService.BehandleGrunnlagResultat
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.TrengerArbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Grunnlagsbehov
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjeningsgrunnlag
import no.nav.helse.opptjening.domain.Opptjeningsprøving
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsprøving
import no.nav.helse.opptjening.domain.VurderingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OpptjeningServiceTest {

    private val vurderinger = InMemoryVilkårsvurderingRepository()
    private val prøvinger = InMemoryVilkårsprøvingRepository()
    private val service = OpptjeningService(vurderinger, prøvinger)

    // ---------------------------------------------------------------------
    // vurderOpptjening
    // ---------------------------------------------------------------------

    // For arbeidstakere kan vi ikke vurdere noe før vi har hentet arbeidsforhold fra aareg.
    // Vi starter en prøving som venter, men det finnes ingen vurdering ennå.
    @Test
    fun `arbeidstaker uten eksisterende vurdering starter en prøving som venter på arbeidsforhold`() {
        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER, 1.februar), resultat)
        assertEquals(0, vurderinger.antallLagringer)

        val prøving = prøvinger.alleProvinger.single()
        assertEquals(FØDSELSNUMMER, prøving.fødselsnummer)
        assertEquals(1.februar, prøving.skjæringstidspunkt)
        assertFalse(prøving.erAvsluttet)
        assertEquals(Grunnlagsbehov.Arbeidsforhold, prøving.uteståendeBehov)
    }

    // Selvstendig næringsdrivende har alltid oppfylt opptjening, så prøvingen fullføres
    // uten å hente noe som helst, og gir en vurdering med en gang
    @Test
    fun `selvstendig næringsdrivende vurderes umiddelbart`() {
        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)

        val harVurdering = assertInstanceOf(HarVurdering::class.java, resultat)
        assertEquals(FØDSELSNUMMER, harVurdering.fødselsnummer)
        assertEquals(1.februar, harVurdering.skjæringstidspunkt)

        val vurdering = vurderinger.finn(Vilkår.Opptjening, harVurdering.vurderingId)!!
        assertEquals(OPPTJENING_MINST_4_UKER, vurdering.kodeverkkode)
        assertEquals(Opptjeningsgrunnlag.SelvstendigNæringsdrivende, vurdering.grunnlag)
        assertTrue(prøvinger.alleProvinger.single().erAvsluttet)
    }

    // Har vi allerede en vurdering skal vi gjenbruke den i stedet for å prøve på nytt
    @Test
    fun `eksisterende vurdering gjenbrukes`() {
        val eksisterende = fullførtPrøving(1.februar)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(HarVurdering(FØDSELSNUMMER, 1.februar, eksisterende), resultat)
        assertEquals(1, vurderinger.antallLagringer)
        assertEquals(1, prøvinger.alleProvinger.size)
    }

    // Gjelder også for selvstendig næringsdrivende – ingen ny prøving skal startes
    @Test
    fun `eksisterende vurdering gjenbrukes også for selvstendig næringsdrivende`() {
        val eksisterende = fullførtPrøving(1.februar)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)

        assertEquals(HarVurdering(FØDSELSNUMMER, 1.februar, eksisterende), resultat)
        assertEquals(1, prøvinger.alleProvinger.size)
    }

    // En pågående prøving betyr at vi venter på arbeidsforhold; da ber vi om dem på nytt
    // i stedet for å starte enda en prøving på de samme dataene
    @Test
    fun `pågående prøving fører til nytt behov om arbeidsforhold, ikke ny prøving`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER, 1.februar), resultat)
        assertEquals(1, prøvinger.alleProvinger.size)
    }

    // Invarianten håndheves av lageret, ikke bare av sjekken i servicen
    @Test
    fun `lageret nekter to aktive prøvinger på samme grunnlag`() {
        val første = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker).prøving
        prøvinger.opprett(første)

        val andre = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker).prøving
        assertThrows<IllegalStateException> { prøvinger.opprett(andre) }
    }

    // Vurderinger er knyttet til ett skjæringstidspunkt; en vurdering på et annet
    // skjæringstidspunkt skal ikke gjenbrukes
    @Test
    fun `vurdering på et annet skjæringstidspunkt gjenbrukes ikke`() {
        fullførtPrøving(1.januar)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER, 1.februar), resultat)
    }

    // ... og heller ikke en vurdering som tilhører en annen person
    @Test
    fun `vurdering for en annen person gjenbrukes ikke`() {
        fullførtPrøving(1.februar, fødselsnummer = ET_ANNET_FØDSELSNUMMER)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER, 1.februar), resultat)
    }

    // ---------------------------------------------------------------------
    // behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering
    // ---------------------------------------------------------------------

    // Kommer det grunnlag uten at vi har startet en prøving har vi ingenting å fullføre
    @Test
    fun `grunnlag uten påbegynt prøving gir ingen prøving funnet`() {
        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(BehandleGrunnlagResultat.IngenPrøvingFunnet, resultat)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // Normalflyten: den pågående prøvingen fullføres med arbeidsforholdene vi fikk inn,
    // og først da oppstår vurderingen
    @Test
    fun `grunnlag fullfører påbegynt prøving og produserer vurderingen`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)
        val arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar))

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = arbeidsforhold,
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        val nyVurdering = assertInstanceOf(BehandleGrunnlagResultat.NyVurderingForetatt::class.java, resultat)
        val prøving = prøvinger.alleProvinger.single()
        assertEquals(Vilkårsprøving.Tilstand.Fullført(nyVurdering.vurderingId), prøving.tilstand)

        val vurdering = vurderinger.finn(Vilkår.Opptjening, nyVurdering.vurderingId)!!
        assertEquals(prøving.id, vurdering.prøvingId)
        assertEquals(OPPTJENING_MINST_4_UKER, vurdering.kodeverkkode)
        assertEquals(arbeidsforhold, (vurdering.grunnlag as Opptjeningsgrunnlag.Arbeidstaker).arbeidsforhold)
    }
    // Kodeverkkoden utledes av arbeidsforholdene: for kort opptjening gir avslagskode
    @Test
    fun `grunnlag med for kort opptjening gir ikke oppfylt`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(5.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Uten arbeidsforhold i det hele tatt er opptjeningen ikke oppfylt, men prøvingen
    // skal likevel fullføres slik at behandlingen kommer videre
    @Test
    fun `grunnlag uten arbeidsforhold fullfører prøvingen`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = emptyList(),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertInstanceOf(BehandleGrunnlagResultat.NyVurderingForetatt::class.java, resultat)
        assertTrue(prøvinger.alleProvinger.single().erAvsluttet)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Duplikate svar på behovet skal ikke gi en ny vurdering
    @Test
    fun `grunnlag på allerede fullført prøving gjør ingenting`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)
        service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )
        val opprinneligVurdering = vurderinger.alleVurderinger.single()

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(5.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(BehandleGrunnlagResultat.AlleredeVurdert, resultat)
        assertSame(opprinneligVurdering, vurderinger.alleVurderinger.single())
    }

    // Grunnlag som gjelder et annet skjæringstidspunkt skal ikke treffe prøvingen vår
    @Test
    fun `grunnlag for et annet skjæringstidspunkt treffer ikke prøvingen`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.mars
        )

        assertEquals(BehandleGrunnlagResultat.IngenPrøvingFunnet, resultat)
        assertFalse(prøvinger.alleProvinger.single().erAvsluttet)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // ---------------------------------------------------------------------
    // finnOpptjeningsvurdering
    // ---------------------------------------------------------------------

    @Test
    fun `finner lagret opptjeningsvurdering`() {
        val vurderingId = fullførtPrøving(1.februar)

        assertEquals(vurderingId, service.finnOpptjeningsvurdering(vurderingId).id)
    }

    @Test
    fun `ukjent opptjeningsvurdering gir feil`() {
        val ukjentId = VurderingId.ny()

        val feil = assertThrows<IllegalStateException> { service.finnOpptjeningsvurdering(ukjentId) }
        assertEquals("Fant ikke vurdering av Opptjening med id $ukjentId", feil.message)
    }

    private fun fullførtPrøving(skjæringstidspunkt: LocalDate, fødselsnummer: String = FØDSELSNUMMER): VurderingId {
        val prøving = Opptjeningsprøving.start(fødselsnummer, skjæringstidspunkt, Arbeidssituasjon.Arbeidstaker).prøving
        val vurdering = prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(listOf(arbeidsforhold(1.januar til 31.januar))))
        prøvinger.opprett(prøving)
        vurderinger.lagre(vurdering)
        return vurdering.id
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ET_ANNET_FØDSELSNUMMER = "12029240046"
        const val ORGNUMMER = "987654321"

        fun arbeidsforhold(ansettelseperiode: Periode, orgnummer: String = ORGNUMMER) =
            Arbeidsforhold(orgnummer = orgnummer, ansettelseperiode = ansettelseperiode, type = ORDINÆRT)
    }
}
