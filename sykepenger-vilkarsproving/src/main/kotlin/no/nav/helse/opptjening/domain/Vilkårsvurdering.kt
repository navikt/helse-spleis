package no.nav.helse.opptjening.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Resultatet av en fullført vilkårsprøving.
 *
 * En vilkårsvurdering er immutabel og finnes aldri i en delvis tilstand: er den først konstruert,
 * er den komplett. Prosessen som ledet fram til den er modellert separat, se [Vilkårsprøving].
 *
 * Variasjonen mellom vilkår ligger i [grunnlag] og [Vilkårsregel] — ikke i resultattypen. Et resultat
 * ser likt ut uansett vilkår, og det er nettopp det som gjør at vi kan spørre likt på tvers av dem.
 */
internal class Vilkårsvurdering private constructor(
    val id: VurderingId,
    /** Prøvingen som produserte vurderingen. */
    val prøvingId: PrøvingId,
    val vilkår: Vilkår,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val grunnlag: Vilkårsgrunnlag,
    val kodeverkkode: Kodeverkkode,
    val kilde: Kilde,
    val vurdertTidspunkt: Instant
) {
    val utfall: Utfall get() = kodeverkkode.utfall

    init {
        check(grunnlag.vilkår == vilkår) { "Grunnlag for ${grunnlag.vilkår} kan ikke brukes i en vurdering av $vilkår" }
        check(kodeverkkode.vilkår == vilkår) { "Kodeverkkode $kodeverkkode hører ikke til vilkåret $vilkår" }
    }

    companion object {
        fun automatisk(
            prøvingId: PrøvingId,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            grunnlag: Vilkårsgrunnlag,
            vurdertTidspunkt: Instant
        ): Vilkårsvurdering {
            val regel = grunnlag.vilkår.regel
            return Vilkårsvurdering(
                id = VurderingId.ny(),
                prøvingId = prøvingId,
                vilkår = grunnlag.vilkår,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnlag = grunnlag,
                kodeverkkode = regel.vurder(skjæringstidspunkt, grunnlag),
                kilde = Kilde.Automatisk(regel.versjon),
                vurdertTidspunkt = vurdertTidspunkt
            )
        }

        fun manuell(
            prøvingId: PrøvingId,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            grunnlag: Vilkårsgrunnlag,
            kodeverkkode: Kodeverkkode,
            saksbehandlerIdent: String,
            fritekstbegrunnelse: String,
            vurdertTidspunkt: Instant
        ) = Vilkårsvurdering(
            id = VurderingId.ny(),
            prøvingId = prøvingId,
            vilkår = grunnlag.vilkår,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = grunnlag,
            kodeverkkode = kodeverkkode,
            kilde = Kilde.Manuell(saksbehandlerIdent, fritekstbegrunnelse),
            vurdertTidspunkt = vurdertTidspunkt
        )
    }
}

/** Hvem som vurderte, og på hvilket regelverk. Manuell vurdering er en kilde, ikke en egen resultattype. */
internal sealed interface Kilde {
    data class Automatisk(val regelversjon: String) : Kilde
    data class Manuell(val saksbehandlerIdent: String, val fritekstbegrunnelse: String) : Kilde
}
