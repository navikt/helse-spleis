package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.bootstrap.sikkerLogg
import no.nav.helse.opptjening.domain.Kodeverkkode
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsgrunnlag
import no.nav.helse.opptjening.domain.Vilkårsprøving
import no.nav.helse.opptjening.domain.Vilkårsvurdering
import no.nav.helse.opptjening.domain.VurderingId

/**
 * Orkestrerer prøvingen, likt for alle vilkår: slår opp om vi allerede har et svar, sørger for at
 * det ikke startes flere prøvinger på de samme dataene, og tar imot grunnlag når det kommer inn.
 *
 * Alt vilkårsspesifikt — hvordan en prøving startes og hvordan et innkommende svar blir til et
 * grunnlag — ligger hos kallerne (`OpptjeningService`, `MedlemskapService`) og i domenet.
 */
internal class VilkårsprøvingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårsprøvingRepository: VilkårsprøvingRepository
) {

    /**
     * Starter en prøving med mindre vi allerede har et svar, eller en prøving pågår.
     * [startPrøving] kalles kun når det faktisk er noe å starte.
     */
    fun prøv(
        vilkår: Vilkår,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        startPrøving: () -> Vilkårsprøving.Påbegynt
    ): PrøvingResultat {
        vilkårsvurderingRepository.gjeldende(vilkår, fødselsnummer, skjæringstidspunkt)?.let { vurdering ->
            sikkerLogg.info("Har allerede vurdering av $vilkår for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
            return PrøvingResultat.HarVurdering(vurdering)
        }

        vilkårsprøvingRepository.finnSiste(vilkår, fødselsnummer, skjæringstidspunkt)?.takeUnless { it.erAvsluttet }?.let { pågående ->
            sikkerLogg.info("Prøving ${pågående.id} av $vilkår pågår allerede for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Etterspør grunnlaget på nytt.")
            return PrøvingResultat.TrengerGrunnlag(pågående)
        }

        val (prøving, vurdering) = startPrøving()
        vilkårsprøvingRepository.opprett(prøving)

        if (vurdering == null) {
            sikkerLogg.info("Startet prøving ${prøving.id} av $vilkår for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Venter på ${prøving.uteståendeBehov}.")
            return PrøvingResultat.TrengerGrunnlag(prøving)
        }

        vilkårsvurderingRepository.lagre(vurdering)
        sikkerLogg.info("Prøving ${prøving.id} av $vilkår fullført uten innhenting for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
        return PrøvingResultat.HarVurdering(vurdering)
    }

    sealed interface PrøvingResultat {
        data class HarVurdering(val vurdering: Vilkårsvurdering) : PrøvingResultat
        data class TrengerGrunnlag(val prøving: Vilkårsprøving) : PrøvingResultat
    }

    /** Tar imot innhentet grunnlag og fullfører prøvingen det hører til. */
    fun behandleGrunnlag(fødselsnummer: String, skjæringstidspunkt: LocalDate, grunnlag: Vilkårsgrunnlag): GrunnlagResultat {
        val vilkår = grunnlag.vilkår
        val prøving = vilkårsprøvingRepository.finnSiste(vilkår, fødselsnummer, skjæringstidspunkt)

        if (prøving == null) {
            sikkerLogg.error("Mottatt grunnlag for $vilkår for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt, men fant ingen prøving.")
            return GrunnlagResultat.IngenPrøvingFunnet
        }

        if (prøving.erAvsluttet) {
            sikkerLogg.info("Mottatt grunnlag for $vilkår for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt, men prøving ${prøving.id} er allerede avsluttet.")
            return GrunnlagResultat.AlleredeVurdert
        }

        val vurdering = prøving.motta(grunnlag)
        vilkårsvurderingRepository.lagre(vurdering)
        vilkårsprøvingRepository.oppdater(prøving)
        sikkerLogg.info("Prøving ${prøving.id} av $vilkår fullført for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
        return GrunnlagResultat.NyVurderingForetatt(vurdering)
    }

    sealed interface GrunnlagResultat {
        data class NyVurderingForetatt(val vurdering: Vilkårsvurdering) : GrunnlagResultat
        data object AlleredeVurdert : GrunnlagResultat
        data object IngenPrøvingFunnet : GrunnlagResultat
    }

    fun finnVurdering(vilkår: Vilkår, vurderingId: VurderingId): Vilkårsvurdering =
        vilkårsvurderingRepository.finn(vilkår, vurderingId) ?: error("Fant ikke vurdering av $vilkår med id $vurderingId")

    /**
     * Lagrer en manuell saksbehandleroverstyring som ny gjeldende vurdering.
     * Tidligere vurderinger beholdes i historikken; den nyeste er alltid gjeldende.
     */
    fun overstyr(
        vilkår: Vilkår,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        grunnlag: Vilkårsgrunnlag,
        kodeverkkode: Kodeverkkode,
        saksbehandlerIdent: String,
        fritekstbegrunnelse: String,
    ): Vilkårsvurdering {
        val (prøving, vurdering) = Vilkårsprøving.manuellOverstyring(
            vilkår = vilkår,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = grunnlag,
            kodeverkkode = kodeverkkode,
            saksbehandlerIdent = saksbehandlerIdent,
            fritekstbegrunnelse = fritekstbegrunnelse
        )
        vilkårsprøvingRepository.opprett(prøving)
        val fullførtVurdering = requireNotNull(vurdering) { "manuellOverstyring skal alltid produsere en vurdering" }
        vilkårsvurderingRepository.lagre(fullførtVurdering)
        sikkerLogg.info(
            "Manuell overstyring av $vilkår for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. " +
            "VurderingId: ${fullførtVurdering.id}. Saksbehandler: $saksbehandlerIdent."
        )
        return fullførtVurdering
    }
}
