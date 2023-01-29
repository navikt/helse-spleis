package no.nav.helse.serde.api.speil.builders

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.api.BuilderState
import no.nav.helse.serde.api.dto.ArbeidsgiverDTO
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.Alder

internal class ArbeidsgiverBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val organisasjonsnummer: String
) : BuilderState() {

    companion object {
        internal fun List<ArbeidsgiverDTO>.vilkårsgrunnlagSomPekesPåAvGhostPerioder(): Map<UUID, Vilkårsgrunnlag> {
            return flatMap { it.ghostPerioder }
                .filter { it.vilkårsgrunnlagId != null && it.vilkårsgrunnlag != null }
                .associate { it.vilkårsgrunnlagId!! to it.vilkårsgrunnlag!! }
        }
    }

    internal fun build(hendelser: List<HendelseDTO>, alder: Alder, vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk): ArbeidsgiverDTO {
        return ArbeidsgiverDTO(
            organisasjonsnummer = organisasjonsnummer,
            id = id,
            ghostPerioder = arbeidsgiver.ghostPerioder().map {
                GhostPeriodeDTO(
                    id = UUID.randomUUID(),
                    fom = it.fom.coerceAtLeast(it.skjæringstidspunkt),
                    tom = it.tom,
                    skjæringstidspunkt = it.skjæringstidspunkt,
                    vilkårsgrunnlagHistorikkInnslagId = it.vilkårsgrunnlagHistorikkInnslagId,
                    vilkårsgrunnlagId = it.vilkårsgrunnlagId,
                    vilkårsgrunnlag = vilkårsgrunnlagHistorikk.finn(it.vilkårsgrunnlagHistorikkInnslagId, it.vilkårsgrunnlagId, it.skjæringstidspunkt),
                    deaktivert = it.deaktivert
                )
            },
            generasjoner = GenerasjonerBuilder(hendelser, alder, arbeidsgiver, vilkårsgrunnlagHistorikk).build()
        )
    }

    override fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        popState()
    }
}
