package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.opptjening.application.MedlemskapService.BehandleGrunnlagResultat
import no.nav.helse.opptjening.application.VurderMedlemskapResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderMedlemskapResultat.TrengerMedlemskap
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Grunnlagsbehov
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_MEDLEM_I_FOLKETRYGDEN
import no.nav.helse.opptjening.domain.Kodeverkkode.MEDLEM_I_FOLKETRYGDEN
import no.nav.helse.opptjening.domain.Medlemskapsgrunnlag
import no.nav.helse.opptjening.domain.Medlemskapsprøving
import no.nav.helse.opptjening.domain.Medlemskapssvar
import no.nav.helse.opptjening.domain.Opptjeningsprøving
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsprøving
import no.nav.helse.opptjening.domain.VurderingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MedlemskapServiceTest {

    private val vurderinger = InMemoryVilkårsvurderingRepository()
    private val prøvinger = InMemoryVilkårsprøvingRepository()
    private val service = MedlemskapService(vurderinger, prøvinger)

    // ---------------------------------------------------------------------
    // vurderMedlemskap
    // ---------------------------------------------------------------------

    @Test
    fun `uten eksisterende vurdering startes en prøving som venter på medlemskap`() {
        val resultat = service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        assertEquals(TrengerMedlemskap(FØDSELSNUMMER, 1.februar), resultat)
        assertEquals(0, vurderinger.antallLagringer)

        val prøving = prøvinger.alleProvinger.single()
        assertEquals(Vilkår.Medlemskap, prøving.vilkår)
        assertEquals(FØDSELSNUMMER, prøving.fødselsnummer)
        assertEquals(1.februar, prøving.skjæringstidspunkt)
        assertFalse(prøving.erAvsluttet)
        assertEquals(Grunnlagsbehov.Medlemskap, prøving.uteståendeBehov)
    }

    @Test
    fun `eksisterende vurdering gjenbrukes`() {
        val eksisterende = fullførtPrøving(1.februar)

        val resultat = service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        assertEquals(HarVurdering(FØDSELSNUMMER, 1.februar, eksisterende), resultat)
        assertEquals(1, vurderinger.antallLagringer)
        assertEquals(1, prøvinger.alleProvinger.size)
    }

    @Test
    fun `pågående prøving fører til nytt behov, ikke ny prøving`() {
        service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        val resultat = service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        assertEquals(TrengerMedlemskap(FØDSELSNUMMER, 1.februar), resultat)
        assertEquals(1, prøvinger.alleProvinger.size)
    }

    @Test
    fun `lageret nekter to aktive prøvinger på samme grunnlag`() {
        prøvinger.opprett(Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar).prøving)

        assertThrows<IllegalStateException> {
            prøvinger.opprett(Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar).prøving)
        }
    }

    // Vilkåret er en del av nøkkelen: opptjening og medlemskap prøves uavhengig av hverandre
    @Test
    fun `pågående opptjeningsprøving hindrer ikke medlemskapsprøving`() {
        prøvinger.opprett(Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker).prøving)

        val resultat = service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        assertEquals(TrengerMedlemskap(FØDSELSNUMMER, 1.februar), resultat)
        assertEquals(2, prøvinger.alleProvinger.size)
    }

    @Test
    fun `vurdering på et annet skjæringstidspunkt gjenbrukes ikke`() {
        fullførtPrøving(1.januar)

        assertEquals(TrengerMedlemskap(FØDSELSNUMMER, 1.februar), service.vurderMedlemskap(FØDSELSNUMMER, 1.februar))
    }

    @Test
    fun `vurdering for en annen person gjenbrukes ikke`() {
        fullførtPrøving(1.februar, fødselsnummer = ET_ANNET_FØDSELSNUMMER)

        assertEquals(TrengerMedlemskap(FØDSELSNUMMER, 1.februar), service.vurderMedlemskap(FØDSELSNUMMER, 1.februar))
    }

    // ---------------------------------------------------------------------
    // behandleGrunnlagForMedlemskapsvurdering
    // ---------------------------------------------------------------------

    @Test
    fun `grunnlag uten påbegynt prøving gir ingen prøving funnet`() {
        val resultat = service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Ja, FØDSELSNUMMER, 1.februar)

        assertEquals(BehandleGrunnlagResultat.IngenPrøvingFunnet, resultat)
        assertEquals(0, vurderinger.antallLagringer)
    }

    @Test
    fun `grunnlag fullfører påbegynt prøving og produserer vurderingen`() {
        service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        val resultat = service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Ja, FØDSELSNUMMER, 1.februar)

        val nyVurdering = assertInstanceOf(BehandleGrunnlagResultat.NyVurderingForetatt::class.java, resultat)
        val prøving = prøvinger.alleProvinger.single()
        assertEquals(Vilkårsprøving.Tilstand.Fullført(nyVurdering.vurderingId), prøving.tilstand)

        val vurdering = vurderinger.finn(Vilkår.Medlemskap, nyVurdering.vurderingId)!!
        assertEquals(prøving.id, vurdering.prøvingId)
        assertEquals(MEDLEM_I_FOLKETRYGDEN, vurdering.kodeverkkode)
        assertEquals(Medlemskapsgrunnlag(Medlemskapssvar.Ja), vurdering.grunnlag)
    }

    @Test
    fun `nei gir en vurdering som ikke er oppfylt`() {
        service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Nei, FØDSELSNUMMER, 1.februar)

        assertEquals(IKKE_MEDLEM_I_FOLKETRYGDEN, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    @Test
    fun `grunnlag på allerede fullført prøving gjør ingenting`() {
        service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)
        service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Ja, FØDSELSNUMMER, 1.februar)
        val opprinneligVurdering = vurderinger.alleVurderinger.single()

        val resultat = service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Nei, FØDSELSNUMMER, 1.februar)

        assertEquals(BehandleGrunnlagResultat.AlleredeVurdert, resultat)
        assertSame(opprinneligVurdering, vurderinger.alleVurderinger.single())
    }

    @Test
    fun `grunnlag for et annet skjæringstidspunkt treffer ikke prøvingen`() {
        service.vurderMedlemskap(FØDSELSNUMMER, 1.februar)

        val resultat = service.behandleGrunnlagForMedlemskapsvurdering(Medlemskapssvar.Ja, FØDSELSNUMMER, 1.mars)

        assertEquals(BehandleGrunnlagResultat.IngenPrøvingFunnet, resultat)
        assertFalse(prøvinger.alleProvinger.single().erAvsluttet)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // ---------------------------------------------------------------------
    // finnMedlemskapsvurdering
    // ---------------------------------------------------------------------

    @Test
    fun `finner lagret medlemskapsvurdering`() {
        val vurderingId = fullførtPrøving(1.februar)

        assertEquals(vurderingId, service.finnMedlemskapsvurdering(vurderingId).id)
    }

    @Test
    fun `ukjent medlemskapsvurdering gir feil`() {
        val ukjentId = VurderingId.ny()

        val feil = assertThrows<IllegalStateException> { service.finnMedlemskapsvurdering(ukjentId) }
        assertEquals("Fant ikke vurdering av Medlemskap med id $ukjentId", feil.message)
    }

    // En opptjeningsvurdering skal ikke kunne slås opp som en medlemskapsvurdering
    @Test
    fun `vurdering av et annet vilkår finnes ikke`() {
        val opptjening = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)
        vurderinger.lagre(opptjening.vurdering!!)

        assertThrows<IllegalStateException> { service.finnMedlemskapsvurdering(opptjening.vurdering.id) }
    }

    private fun fullførtPrøving(skjæringstidspunkt: LocalDate, fødselsnummer: String = FØDSELSNUMMER): VurderingId {
        val prøving = Medlemskapsprøving.start(fødselsnummer, skjæringstidspunkt).prøving
        val vurdering = prøving.motta(Medlemskapsgrunnlag(Medlemskapssvar.Ja))
        prøvinger.opprett(prøving)
        vurderinger.lagre(vurdering)
        return vurdering.id
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ET_ANNET_FØDSELSNUMMER = "12029240046"
    }
}
