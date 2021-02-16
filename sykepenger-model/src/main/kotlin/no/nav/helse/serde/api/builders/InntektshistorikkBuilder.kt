package no.nav.helse.serde.api.builders

import no.nav.helse.Toggles
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Person
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.serde.api.inntektsgrunnlag
import java.time.LocalDate

internal class InntektshistorikkBuilder {
    private val inntektshistorikk = mutableMapOf<String, InntektshistorikkVol2>()
    private val nøkkeldataOmInntekter = mutableListOf<NøkkeldataOmInntekt>()

    internal fun inntektshistorikk(organisasjonsnummer: String, inntektshistorikkVol2: InntektshistorikkVol2) {
        inntektshistorikk[organisasjonsnummer] = inntektshistorikkVol2
    }

    internal fun nøkkeldataOmInntekt(nøkkeldataOmInntekt: NøkkeldataOmInntekt) {
        nøkkeldataOmInntekter.add(nøkkeldataOmInntekt)
    }

    fun build(person: Person): List<InntektsgrunnlagDTO> {
        if (!Toggles.SpeilInntekterVol2Enabled.enabled) return emptyList()
        return inntektsgrunnlag(
            person,
            inntektshistorikk,
            nøkkeldataOmInntekter
                .groupBy { it.skjæringstidspunkt }
                .mapNotNull { (_, value) -> value.maxByOrNull { it.sisteDagISammenhengendePeriode } }
        )
    }

    internal class NøkkeldataOmInntekt(
        val sisteDagISammenhengendePeriode: LocalDate,
        val skjæringstidspunkt: LocalDate,
        var avviksprosent: Double? = null
    )
}
