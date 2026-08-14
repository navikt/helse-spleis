package no.nav.helse.opptjening.domain

import java.time.Instant
import java.time.LocalDate

/**
 * En ferdig vurdering av opptjeningsvilkåret. Kan bare konstrueres med et komplett grunnlag,
 * og endres aldri etterpå. Skal en vurdering gjøres om, kjøres en ny prøving som gir en ny vurdering.
 */
internal class Opptjeningsvurdering(
    override val id: VurderingId,
    override val prøvingId: PrøvingId,
    override val fødselsnummer: String,
    override val skjæringstidspunkt: LocalDate,
    val grunnlag: Opptjeningsgrunnlag,
    override val kodeverkkode: Kodeverkkode,
    override val kilde: Kilde,
    override val vurdertTidspunkt: Instant
) : Vilkårsvurdering {

    companion object {
        fun automatisk(
            prøvingId: PrøvingId,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            grunnlag: Opptjeningsgrunnlag,
            vurdertTidspunkt: Instant
        ) = Opptjeningsvurdering(
            id = VurderingId.ny(),
            prøvingId = prøvingId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = grunnlag,
            kodeverkkode = Opptjeningsregel.vurder(skjæringstidspunkt, grunnlag),
            kilde = Kilde.Automatisk(Opptjeningsregel.VERSJON),
            vurdertTidspunkt = vurdertTidspunkt
        )

        fun manuell(
            prøvingId: PrøvingId,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            grunnlag: Opptjeningsgrunnlag,
            kodeverkkode: Kodeverkkode,
            saksbehandlerIdent: String,
            fritekstbegrunnelse: String,
            vurdertTidspunkt: Instant
        ) = Opptjeningsvurdering(
            id = VurderingId.ny(),
            prøvingId = prøvingId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = grunnlag,
            kodeverkkode = kodeverkkode,
            kilde = Kilde.Manuell(saksbehandlerIdent, fritekstbegrunnelse),
            vurdertTidspunkt = vurdertTidspunkt
        )
    }
}
