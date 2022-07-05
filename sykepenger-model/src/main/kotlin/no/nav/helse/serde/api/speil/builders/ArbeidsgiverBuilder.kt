package no.nav.helse.serde.api.speil.builders

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.api.BuilderState
import no.nav.helse.serde.api.dto.ArbeidsgiverDTO
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.utbetalingstidslinje.Alder

internal class ArbeidsgiverBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val organisasjonsnummer: String
) : BuilderState() {

    internal fun build(hendelser: List<HendelseDTO>, alder: Alder): ArbeidsgiverDTO {
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
                    deaktivert = it.deaktivert
                )
            },
            generasjoner = GenerasjonerBuilder(hendelser, alder, arbeidsgiver).build()
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
