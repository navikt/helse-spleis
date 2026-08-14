package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID
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
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjening
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering.ForArbeidstaker
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OpptjeningServiceTest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val service = OpptjeningService(repository)

    // ---------------------------------------------------------------------
    // vurderOpptjening
    // ---------------------------------------------------------------------

    // For arbeidstakere kan vi ikke vurdere noe før vi har hentet arbeidsforhold fra aareg,
    // men vi oppretter en pending vurdering slik at grunnlagriveren kan finne den igjen
    @Test
    fun `arbeidstaker uten eksisterende vurdering trenger arbeidsforhold`() {
        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER), resultat)
        assertEquals(1, repository.antallLagringer) // pending vurdering lagres
        val lagret = repository.alleVurderinger.single() as AutomatiskVurdering
        assertEquals(FØDSELSNUMMER, lagret.fødselsnummer)
        assertEquals(1.februar, lagret.skjæringstidspunkt)
        assertFalse(lagret.erKomplett)
    }

    // Selvstendig næringsdrivende har alltid oppfylt opptjening, så vurderingen kan
    // fullføres uten å hente noe som helst
    @Test
    fun `selvstendig næringsdrivende vurderes umiddelbart`() {
        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)

        val harVurdering = assertInstanceOf(HarVurdering::class.java, resultat)
        assertEquals(FØDSELSNUMMER, harVurdering.fødselsnummer)
        assertEquals(1.februar, harVurdering.skjæringstidspunkt)

        assertEquals(1, repository.antallLagringer)
        val lagret = repository.finn<AutomatiskVurdering>(harVurdering.vurderingId)!!
        assertTrue(lagret.erKomplett)
        assertEquals(OPPTJENING_MINST_4_UKER, lagret.kodeverkkode)
        assertSame(ForSelvstendigNæringsdrivende, lagret.grunnlagForAutomatiskVurdering)
    }

    // Har vi allerede en komplett vurdering skal vi gjenbruke den i stedet for å vurdere på nytt
    @Test
    fun `eksisterende komplett vurdering gjenbrukes`() {
        val eksisterende = komplettVurdering(1.februar)
        repository.lagre(eksisterende)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(HarVurdering(FØDSELSNUMMER, 1.februar, eksisterende.id), resultat)
        assertEquals(1, repository.antallLagringer) // kun lagringen testen selv gjorde
    }

    // Gjelder også for selvstendig næringsdrivende – ingen ny vurdering skal opprettes
    @Test
    fun `eksisterende komplett vurdering gjenbrukes også for selvstendig næringsdrivende`() {
        val eksisterende = komplettVurdering(1.februar)
        repository.lagre(eksisterende)

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)

        assertEquals(HarVurdering(FØDSELSNUMMER, 1.februar, eksisterende.id), resultat)
        assertEquals(1, repository.antallLagringer)
    }

    // En ufullstendig vurdering betyr at vi venter på arbeidsforhold; da ber vi om dem på nytt
    @Test
    fun `eksisterende ufullstendig vurdering fører til nytt behov om arbeidsforhold`() {
        repository.lagre(ufullstendigVurdering(1.februar))

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER), resultat)
        assertEquals(1, repository.antallLagringer)
    }

    // Vurderinger er knyttet til ett skjæringstidspunkt; en vurdering på et annet
    // skjæringstidspunkt skal ikke gjenbrukes
    @Test
    fun `vurdering på et annet skjæringstidspunkt gjenbrukes ikke`() {
        repository.lagre(komplettVurdering(1.januar))

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER), resultat)
    }

    // ... og heller ikke en vurdering som tilhører en annen person
    @Test
    fun `vurdering for en annen person gjenbrukes ikke`() {
        repository.lagre(komplettVurdering(1.februar, fødselsnummer = ET_ANNET_FØDSELSNUMMER))

        val resultat = service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertEquals(TrengerArbeidsforhold(FØDSELSNUMMER), resultat)
    }

    // vurderOpptjening(Arbeidstaker) oppretter en pending vurdering som
    // behandleGrunnlag... kan fullføre når arbeidsforholdene kommer tilbake.
    // Uten denne koblingen ville arbeidsforholdløsningen aldri treffe noe,
    // og arbeidstakerflyt ville vært brutt.
    @Test
    fun `pending vurdering fra vurderOpptjening kan fullføres av behandleGrunnlag`() {
        service.vurderOpptjening(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        val nyVurdering = assertInstanceOf(BehandleGrunnlagResultat.NyVurderingForetatt::class.java, resultat)
        val lagret = repository.finn<AutomatiskVurdering>(nyVurdering.vurderingId)!!
        assertTrue(lagret.erKomplett)
        assertEquals(OPPTJENING_MINST_4_UKER, lagret.kodeverkkode)
    }

    // ---------------------------------------------------------------------
    // behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering
    // ---------------------------------------------------------------------

    // Kommer det grunnlag uten at vi har en påbegynt vurdering har vi ingenting å fullføre
    @Test
    fun `grunnlag uten påbegynt vurdering gir ingen vurdering funnet`() {
        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(BehandleGrunnlagResultat.IngenVurderingFunnet, resultat)
        assertEquals(0, repository.antallLagringer)
    }

    // Normalflyten: en påbegynt vurdering fullføres med arbeidsforholdene vi fikk inn
    @Test
    fun `grunnlag fullfører påbegynt vurdering`() {
        val påbegynt = ufullstendigVurdering(1.februar)
        repository.lagre(påbegynt)

        val arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar))
        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = arbeidsforhold,
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(
            BehandleGrunnlagResultat.NyVurderingForetatt(FØDSELSNUMMER, 1.februar, påbegynt.id),
            resultat
        )
        assertTrue(påbegynt.erKomplett)
        assertEquals(OPPTJENING_MINST_4_UKER, påbegynt.kodeverkkode)
        assertEquals(arbeidsforhold, (påbegynt.grunnlagForAutomatiskVurdering as ForArbeidstaker).arbeidsforhold)
        assertEquals(2, repository.antallLagringer) // testens egen lagring + servicens
    }

    // Kodeverkkoden utledes av arbeidsforholdene: for kort opptjening gir avslagskode
    @Test
    fun `grunnlag med for kort opptjening gir ikke oppfylt`() {
        val påbegynt = ufullstendigVurdering(1.februar)
        repository.lagre(påbegynt)

        service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(5.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, påbegynt.kodeverkkode)
    }

    // Uten arbeidsforhold i det hele tatt er opptjeningen ikke oppfylt, men vurderingen
    // skal likevel fullføres slik at behandlingen kommer videre
    @Test
    fun `grunnlag uten arbeidsforhold fullfører vurderingen`() {
        val påbegynt = ufullstendigVurdering(1.februar)
        repository.lagre(påbegynt)

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = emptyList(),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertInstanceOf(BehandleGrunnlagResultat.NyVurderingForetatt::class.java, resultat)
        assertTrue(påbegynt.erKomplett)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, påbegynt.kodeverkkode)
    }

    // Duplikate svar på behovet skal ikke overskrive en allerede fullført vurdering
    @Test
    fun `grunnlag på allerede komplett vurdering gjør ingenting`() {
        val komplett = komplettVurdering(1.februar)
        repository.lagre(komplett)
        val opprinneligGrunnlag = komplett.grunnlagForAutomatiskVurdering

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar
        )

        assertEquals(BehandleGrunnlagResultat.AlleredeVurdert, resultat)
        assertSame(opprinneligGrunnlag, komplett.grunnlagForAutomatiskVurdering)
        assertEquals(1, repository.antallLagringer)
    }

    // Grunnlag som gjelder et annet skjæringstidspunkt skal ikke treffe vurderingen vår
    @Test
    fun `grunnlag for et annet skjæringstidspunkt treffer ikke vurderingen`() {
        val påbegynt = ufullstendigVurdering(1.februar)
        repository.lagre(påbegynt)

        val resultat = service.behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
            arbeidsforhold = listOf(arbeidsforhold(1.januar til 31.januar)),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.mars
        )

        assertEquals(BehandleGrunnlagResultat.IngenVurderingFunnet, resultat)
        assertFalse(påbegynt.erKomplett)
    }

    // ---------------------------------------------------------------------
    // finnOpptjeningsvurderingResultat
    // ---------------------------------------------------------------------

    @Test
    fun `finner lagret opptjeningsvurdering`() {
        val vurdering = komplettVurdering(1.februar)
        repository.lagre(vurdering)

        assertSame(vurdering, service.finnOpptjeningsvurderingResultat(vurdering.id))
    }

    @Test
    fun `ukjent opptjeningsvurdering gir feil`() {
        val ukjentId = UUID.randomUUID()

        val feil = assertThrows<IllegalStateException> { service.finnOpptjeningsvurderingResultat(ukjentId) }
        assertEquals("Fant ikke opptjeningsvurdering med id $ukjentId", feil.message)
    }

    // Vurderinger som ikke er fullført har verken kodeverkkode eller grunnlag ennå
    @Test
    fun `ufullstendig vurdering har ingen kodeverkkode`() {
        val påbegynt = ufullstendigVurdering(1.februar)
        repository.lagre(påbegynt)

        val funnet = service.finnOpptjeningsvurderingResultat(påbegynt.id) as AutomatiskVurdering
        assertFalse(funnet.erKomplett)
        assertNull(funnet.kodeverkkode)
        assertNull(funnet.grunnlagForAutomatiskVurdering)
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ET_ANNET_FØDSELSNUMMER = "12029240046"
        const val ORGNUMMER = "987654321"

        fun arbeidsforhold(ansettelseperiode: Periode, orgnummer: String = ORGNUMMER) =
            Arbeidsforhold(orgnummer = orgnummer, ansettelseperiode = ansettelseperiode, type = ORDINÆRT)

        fun ufullstendigVurdering(skjæringstidspunkt: LocalDate, fødselsnummer: String = FØDSELSNUMMER): AutomatiskVurdering =
            AutomatiskVurdering.nyAutomatiskVurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                versjonAvKildekode = ""
            )

        fun komplettVurdering(skjæringstidspunkt: LocalDate, fødselsnummer: String = FØDSELSNUMMER): AutomatiskVurdering =
            ufullstendigVurdering(skjæringstidspunkt, fødselsnummer)
                .also { it.fullfør(ForArbeidstaker(listOf(arbeidsforhold(1.januar til 31.januar)))) }
    }
}
