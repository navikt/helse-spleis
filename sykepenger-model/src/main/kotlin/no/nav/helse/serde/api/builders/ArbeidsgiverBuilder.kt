package no.nav.helse.serde.api.builders

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.api.ArbeidsgiverDTO
import no.nav.helse.serde.api.GhostPeriodeDTO
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.buildere.GenerasjonerBuilder
import no.nav.helse.serde.api.v2.buildere.IVilkårsgrunnlagHistorikk
import no.nav.helse.somFødselsnummer

internal class ArbeidsgiverBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val organisasjonsnummer: String
) : BuilderState() {
    internal fun build(hendelser: List<HendelseDTO>, fødselsnummer: String, vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk): ArbeidsgiverDTO {
        return ArbeidsgiverDTO(
            organisasjonsnummer = organisasjonsnummer,
            id = id,
            generasjoner = GenerasjonerBuilder(hendelser, fødselsnummer.somFødselsnummer(), vilkårsgrunnlagHistorikk, arbeidsgiver).build(),
            ghostPerioder = arbeidsgiver.ghostPerioder().map {
                GhostPeriodeDTO(
                    id = UUID.randomUUID(),
                    fom = it.fom.coerceAtLeast(it.skjæringstidspunkt),
                    tom = it.tom,
                    skjæringstidspunkt = it.skjæringstidspunkt,
                    vilkårsgrunnlagHistorikkInnslagId = it.vilkårsgrunnlagHistorikkInnslagId,
                    deaktivert = it.deaktivert
                )
            }
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
