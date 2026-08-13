package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.domain.Vilkårsvurdering

interface VilkårsvurderingRepository {
    fun lagre(vilkårsvurdering: Vilkårsvurdering)
    fun <T: Vilkårsvurdering> finnNyesteVilkårsvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate): T?
    fun <T: Vilkårsvurdering> finn(opptjeningsvurderingId: UUID): T?
}
