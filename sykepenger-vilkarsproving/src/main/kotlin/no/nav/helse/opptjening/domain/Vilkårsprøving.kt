package no.nav.helse.opptjening.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Prosessen som leder fram til en [Vilkårsvurdering].
 *
 * Prøvingen eier livssyklusen — hva vi venter på, hvor lenge, og om vi er ferdige — mens selve
 * vurderingen er resultatet den produserer. Det er kun innhentingen av grunnlag som er asynkron;
 * vurderingen i seg selv er en ren funksjon ([Vilkårsregel]).
 *
 * Livssyklusen er den samme for alle vilkår, og ligger derfor her. Det vilkårsspesifikke — hvilket
 * grunnlag som må innhentes, og hva som utgjør et utfall — ligger i [Vilkårsgrunnlag] og [Vilkårsregel].
 * Hvert vilkår har en egen startfunksjon, se `Opptjeningsprøving` og `Medlemskapsprøving`.
 *
 * Prøvingen holder ikke på innhentede fakta. Det er ikke nødvendig så lenge den venter på ett
 * grunnlag om gangen: kommer svaret, konstrueres grunnlaget og vurderingen i samme operasjon.
 * Skal et vilkår senere vente på flere uavhengige svar må vi legge til et arbeidsminne her.
 */
internal class Vilkårsprøving private constructor(
    val id: PrøvingId,
    val vilkår: Vilkår,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val startet: Instant,
    tilstand: Tilstand
) {
    var tilstand: Tilstand = tilstand
        private set

    val erAvsluttet get() = tilstand is Tilstand.Fullført

    val uteståendeBehov get() = (tilstand as? Tilstand.VenterPåGrunnlag)?.behov

    sealed interface Tilstand {
        data object Startet : Tilstand
        data class VenterPåGrunnlag(val behov: Grunnlagsbehov) : Tilstand
        data class Fullført(val vurderingId: VurderingId) : Tilstand
    }

    /**
     * Tar imot grunnlaget prøvingen venter på og produserer vurderingen.
     * Vurderingen og den oppdaterte prøvingen må lagres i samme transaksjon.
     */
    fun motta(grunnlag: Vilkårsgrunnlag): Vilkårsvurdering {
        val venter = tilstand as? Tilstand.VenterPåGrunnlag
            ?: error("Prøving $id venter ikke på grunnlag, men er i tilstand $tilstand")
        check(grunnlag.besvarer == venter.behov) {
            "Prøving $id venter på ${venter.behov}, men fikk grunnlag som besvarer ${grunnlag.besvarer}"
        }
        return fullfør(grunnlag)
    }

    private fun fullfør(grunnlag: Vilkårsgrunnlag): Vilkårsvurdering {
        check(grunnlag.vilkår == vilkår) { "Prøving $id gjelder $vilkår, men fikk grunnlag for ${grunnlag.vilkår}" }
        val vurdering = Vilkårsvurdering.automatisk(
            prøvingId = id,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = grunnlag,
            vurdertTidspunkt = Instant.now()
        )
        tilstand = Tilstand.Fullført(vurdering.id)
        return vurdering
    }

    /** En påbegynt prøving. [vurdering] er satt dersom prøvingen kunne fullføres uten å innhente noe. */
    data class Påbegynt(val prøving: Vilkårsprøving, val vurdering: Vilkårsvurdering?)

    companion object {
        /**
         * Starter en prøving. [umiddelbartGrunnlag] settes av vilkår som kan vurderes uten å hente
         * noe utenfra; ellers venter prøvingen på [behov].
         */
        fun start(
            vilkår: Vilkår,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            behov: Grunnlagsbehov,
            umiddelbartGrunnlag: Vilkårsgrunnlag?
        ): Påbegynt {
            val prøving = Vilkårsprøving(
                id = PrøvingId.ny(),
                vilkår = vilkår,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                startet = Instant.now(),
                tilstand = Tilstand.Startet
            )
            val vurdering = when (umiddelbartGrunnlag) {
                null -> {
                    prøving.tilstand = Tilstand.VenterPåGrunnlag(behov)
                    null
                }

                else -> prøving.fullfør(umiddelbartGrunnlag)
            }
            return Påbegynt(prøving, vurdering)
        }

        fun fraLagring(
            id: PrøvingId,
            vilkår: Vilkår,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            startet: Instant,
            tilstand: Tilstand
        ) = Vilkårsprøving(id, vilkår, fødselsnummer, skjæringstidspunkt, startet, tilstand)

        /**
         * Starter en manuell saksbehandlerovertsyring: oppretter en prøving som er fullført umiddelbart,
         * uten innhenting av grunnlag. Saksbehandlerens begrunnelse bæres av vurderingen.
         */
        fun manuellOverstyring(
            vilkår: Vilkår,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            grunnlag: Vilkårsgrunnlag,
            kodeverkkode: Kodeverkkode,
            saksbehandlerIdent: String,
            fritekstbegrunnelse: String,
        ): Påbegynt {
            val prøving = Vilkårsprøving(
                id = PrøvingId.ny(),
                vilkår = vilkår,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                startet = Instant.now(),
                tilstand = Tilstand.Startet
            )
            val vurdering = Vilkårsvurdering.manuell(
                prøvingId = prøving.id,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnlag = grunnlag,
                kodeverkkode = kodeverkkode,
                saksbehandlerIdent = saksbehandlerIdent,
                fritekstbegrunnelse = fritekstbegrunnelse,
                vurdertTidspunkt = Instant.now()
            )
            prøving.tilstand = Tilstand.Fullført(vurdering.id)
            return Påbegynt(prøving, vurdering)
        }
    }
}
