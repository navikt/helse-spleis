package no.nav.helse.opptjening.infra.db

import java.time.LocalDate
import no.nav.helse.opptjening.application.VilkårsvurderingRepository
import no.nav.helse.opptjening.domain.Opptjeningsvurdering
import no.nav.helse.opptjening.domain.VurderingId

internal class InMemoryVilkårsvurderingRepository : VilkårsvurderingRepository {
    private val vurderinger = mutableListOf<Opptjeningsvurdering>()

    internal val alleVurderinger: List<Opptjeningsvurdering> get() = vurderinger.toList()

    override fun lagre(vurdering: Opptjeningsvurdering) {
        check(vurderinger.none { it.id == vurdering.id }) { "Vurdering ${vurdering.id} er allerede lagret. Vurderinger er immutable." }
        vurderinger.add(vurdering)
    }

    override fun gjeldende(fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        vurderinger.lastOrNull { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

    override fun finn(vurderingId: VurderingId) = vurderinger.firstOrNull { it.id == vurderingId }
}
