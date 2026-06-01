package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.riktigProsent

data class ForsikringBasertPåForsikringsvurdering(
    val forsikringsvurderingId: UUID,
    val dekning: Forsikringsvurdering.Dekning
) : Forsikring {
    override fun dekningsgrad(): Prosentdel = dekning.grad.riktigProsent

    override fun navOvertarAnsvarForVentetid() = dekning.fraDag == 1

    companion object {
        fun fraLøsning(løsning: Forsikringsvurdering): ForsikringBasertPåForsikringsvurdering? =
            løsning.takeIf { it.harForsikring }?.let {
                ForsikringBasertPåForsikringsvurdering(
                    forsikringsvurderingId = it.forsikringsvurderingId,
                    dekning = it.dekning!!
                )
            }
    }
}

data class Forsikringsvurdering(
    val forsikringsvurderingId: UUID,
    val harForsikring: Boolean,
    val dekning: Dekning?
) {
    data class Dekning(
        val grad: Int,
        val fraDag: Int,
    )
}
