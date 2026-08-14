package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.Opptjeningsvurdering
import no.nav.helse.opptjening.domain.VurderingId

internal class InMemoryVilkårsvurderingRepository : VilkårsvurderingRepository {
    private val vurderinger = mutableListOf<Opptjeningsvurdering>()

    internal val alleVurderinger: List<Opptjeningsvurdering> get() = vurderinger.toList()
    internal val antallLagringer get() = vurderinger.size

    override fun lagre(vurdering: Opptjeningsvurdering) {
        check(vurderinger.none { it.id == vurdering.id }) { "Vurdering ${vurdering.id} er allerede lagret. Vurderinger er immutable." }
        vurderinger.add(vurdering)
    }

    override fun gjeldende(fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        vurderinger.lastOrNull { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

    override fun finn(vurderingId: VurderingId) = vurderinger.firstOrNull { it.id == vurderingId }
}
