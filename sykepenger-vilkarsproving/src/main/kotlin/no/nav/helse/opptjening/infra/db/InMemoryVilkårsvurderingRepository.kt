package no.nav.helse.opptjening.infra.db

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.application.VilkårsvurderingRepository
import no.nav.helse.opptjening.domain.Vilkårsvurdering

internal class InMemoryVilkårsvurderingRepository : VilkårsvurderingRepository {
    private val vurderinger = mutableListOf<Vilkårsvurdering>()

    internal var antallLagringer = 0
        private set

    internal val alleVurderinger: List<Vilkårsvurdering> get() = vurderinger.toList()

    override fun lagre(vilkårsvurdering: Vilkårsvurdering) {
        antallLagringer += 1
        vurderinger.removeIf { it.id == vilkårsvurdering.id }
        vurderinger.add(vilkårsvurdering)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Vilkårsvurdering> finnNyesteVilkårsvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate): T? =
        vurderinger.lastOrNull { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt } as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T : Vilkårsvurdering> finn(opptjeningsvurderingId: UUID): T? =
        vurderinger.firstOrNull { it.id == opptjeningsvurderingId } as T?
}
