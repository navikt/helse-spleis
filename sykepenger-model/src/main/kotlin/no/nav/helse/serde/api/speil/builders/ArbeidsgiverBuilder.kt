package no.nav.helse.serde.api.speil.builders

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.api.BuilderState
import no.nav.helse.serde.api.dto.ArbeidsgiverDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.Alder

internal class ArbeidsgiverBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val organisasjonsnummer: String
) : BuilderState() {

    internal fun build(hendelser: List<HendelseDTO>, alder: Alder, vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk): ArbeidsgiverDTO {
        return ArbeidsgiverDTO(
            organisasjonsnummer = organisasjonsnummer,
            id = id,
            generasjoner = GenerasjonerBuilder(organisasjonsnummer, hendelser, alder, arbeidsgiver, vilkårsgrunnlagHistorikk).build()
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
