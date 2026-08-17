package no.nav.helse.opptjening.infra.db

import java.time.LocalDate
import no.nav.helse.opptjening.application.VilkårsvurderingRepository
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsvurdering
import no.nav.helse.opptjening.domain.VurderingId

internal class InMemoryVilkårsvurderingRepository : VilkårsvurderingRepository {
    private val vurderinger = mutableListOf<Vilkårsvurdering>()

    internal val alleVurderinger: List<Vilkårsvurdering> get() = vurderinger.toList()
    internal val antallLagringer get() = vurderinger.size

    override fun lagre(vurdering: Vilkårsvurdering) {
        check(vurderinger.none { it.id == vurdering.id }) { "Vurdering ${vurdering.id} er allerede lagret. Vurderinger er immutable." }
        vurderinger.add(vurdering)
    }

    override fun gjeldende(vilkår: Vilkår, fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        vurderinger.lastOrNull { it.vilkår == vilkår && it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

    override fun finn(vilkår: Vilkår, vurderingId: VurderingId) =
        vurderinger.firstOrNull { it.vilkår == vilkår && it.id == vurderingId }
}
