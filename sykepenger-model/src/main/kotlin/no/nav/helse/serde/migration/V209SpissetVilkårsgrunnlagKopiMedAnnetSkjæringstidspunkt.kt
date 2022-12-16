package no.nav.helse.serde.migration

import java.time.LocalDate
import java.util.UUID

internal class V209SpissetVilkårsgrunnlagKopiMedAnnetSkjæringstidspunkt : KopiereVilkårsgrunnlag(
    versjon = 209,
    Triple(vilkårsgrunnlagId, skjæringstidspunkt, null)
) {

    private companion object {
        private val vilkårsgrunnlagId = UUID.fromString("0b407252-8e5e-47a7-b407-23d1acfd02e2")
        private val skjæringstidspunkt = LocalDate.parse("2021-07-11")
    }

}