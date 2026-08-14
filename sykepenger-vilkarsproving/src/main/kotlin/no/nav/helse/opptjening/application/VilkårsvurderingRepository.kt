package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.Opptjeningsvurdering
import no.nav.helse.opptjening.domain.VurderingId

/**
 * Lager for ferdige vurderinger. Vurderinger skrives kun én gang og oppdateres aldri; en ny prøving
 * gir en ny vurdering, slik at historikken består.
 */
internal interface VilkårsvurderingRepository {
    fun lagre(vurdering: Opptjeningsvurdering)

    fun gjeldende(fødselsnummer: String, skjæringstidspunkt: LocalDate): Opptjeningsvurdering?

    fun finn(vurderingId: VurderingId): Opptjeningsvurdering?
}
