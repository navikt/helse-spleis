package no.nav.helse.opptjening.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Prosessen som leder fram til en [Opptjeningsvurdering].
 *
 * Prøvingen eier livssyklusen — hva vi venter på, hvor lenge, og om vi er ferdige — mens selve
 * vurderingen er resultatet den produserer. Det er kun innhentingen av grunnlag som er asynkron;
 * vurderingen i seg selv er en ren funksjon ([Opptjeningsregel]).
 *
 * Prøvingen holder ikke på innhentede fakta. Det er ikke nødvendig så lenge den venter på ett
 * grunnlag om gangen: kommer svaret, konstrueres grunnlaget og vurderingen i samme operasjon.
 * Skal vi senere vente på flere uavhengige svar må vi legge til et arbeidsminne her.
 */
internal class Opptjeningsprøving private constructor(
    val id: PrøvingId,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidssituasjon: Arbeidssituasjon,
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
    fun motta(grunnlag: Opptjeningsgrunnlag): Opptjeningsvurdering {
        val venter = tilstand as? Tilstand.VenterPåGrunnlag
            ?: error("Prøving $id venter ikke på grunnlag, men er i tilstand $tilstand")
        check(grunnlag.besvarer == venter.behov) {
            "Prøving $id venter på ${venter.behov}, men fikk grunnlag som besvarer ${grunnlag.besvarer}"
        }
        return fullfør(grunnlag)
    }

    private fun fullfør(grunnlag: Opptjeningsgrunnlag): Opptjeningsvurdering {
        val vurdering = Opptjeningsvurdering.automatisk(
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
    data class Påbegynt(val prøving: Opptjeningsprøving, val vurdering: Opptjeningsvurdering?)

    companion object {
        fun start(
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            arbeidssituasjon: Arbeidssituasjon
        ): Påbegynt {
            val grunnlagViHarAllerede = when (arbeidssituasjon) {
                Arbeidssituasjon.Arbeidstaker -> null
                Arbeidssituasjon.SelvstendigNæringsdrivende -> Opptjeningsgrunnlag.SelvstendigNæringsdrivende
            }
            val prøving = Opptjeningsprøving(
                id = PrøvingId.ny(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidssituasjon = arbeidssituasjon,
                startet = Instant.now(),
                tilstand = Tilstand.Startet
            )
            val vurdering = when (grunnlagViHarAllerede) {
                null -> {
                    prøving.tilstand = Tilstand.VenterPåGrunnlag(Grunnlagsbehov.Arbeidsforhold)
                    null
                }

                else -> prøving.fullfør(grunnlagViHarAllerede)
            }
            return Påbegynt(prøving, vurdering)
        }

        fun fraLagring(
            id: PrøvingId,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            arbeidssituasjon: Arbeidssituasjon,
            startet: Instant,
            tilstand: Tilstand
        ) = Opptjeningsprøving(id, fødselsnummer, skjæringstidspunkt, arbeidssituasjon, startet, tilstand)
    }
}
