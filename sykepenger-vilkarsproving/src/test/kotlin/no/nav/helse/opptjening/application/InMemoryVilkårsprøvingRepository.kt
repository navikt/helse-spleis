package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsprøving

internal class InMemoryVilkårsprøvingRepository : VilkårsprøvingRepository {
    private val prøvinger = mutableListOf<Vilkårsprøving>()

    internal val alleProvinger: List<Vilkårsprøving> get() = prøvinger.toList()

    override fun opprett(prøving: Vilkårsprøving) {
        check(prøvinger.none { it.gjelderSammeSom(prøving) && !it.erAvsluttet }) {
            "Det pågår allerede en prøving av ${prøving.vilkår} for fødselsnummer ${prøving.fødselsnummer} med skjæringstidspunkt ${prøving.skjæringstidspunkt}"
        }
        prøvinger.add(prøving)
    }

    override fun oppdater(prøving: Vilkårsprøving) {
        check(prøvinger.any { it.id == prøving.id }) { "Prøving ${prøving.id} er ikke opprettet" }
    }

    override fun finnSiste(vilkår: Vilkår, fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        prøvinger.lastOrNull { it.vilkår == vilkår && it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

    private fun Vilkårsprøving.gjelderSammeSom(annen: Vilkårsprøving) =
        vilkår == annen.vilkår && fødselsnummer == annen.fødselsnummer && skjæringstidspunkt == annen.skjæringstidspunkt
}
