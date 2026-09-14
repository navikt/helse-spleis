package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import org.junit.jupiter.api.fail

internal val VilkårsgrunnlagHistorikk.inspektør get() = Vilkårgrunnlagsinspektør(this)

internal class Vilkårgrunnlagsinspektør(historikk: VilkårsgrunnlagHistorikk) {
    private val innslag = historikk.historikk

    val vilkårsgrunnlagTeller = innslag.mapIndexed { index, i -> index to i.vilkårsgrunnlag.size }.toMap()
    internal val aktiveSpleisSkjæringstidspunkt = innslag.getOrNull(0)?.vilkårsgrunnlag?.keys?.toSet() ?: emptySet()

    private val grunnlagsdata = innslag.flatMap { it.vilkårsgrunnlag.entries.map { entry -> entry.key to entry.value } }

    internal fun antallGrunnlagsdata() = vilkårsgrunnlagTeller.map(Map.Entry<*, Int>::value).sum()
    internal fun vilkårsgrunnlagHistorikkInnslag() = innslag.toList()
    internal fun grunnlagsdata(skjæringstidspunkt: LocalDate) = grunnlagsdata.firstOrNull { it.first == skjæringstidspunkt }?.second as? Grunnlagsdata ?: fail("Fant ikke grunnlagsdata på skjæringstidspunkt $skjæringstidspunkt")
}

internal val VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.inspektør get() = (this as? Grunnlagsdata ?: fail("Fant ikke grunnlagsdata på skjæringstidspunkt $skjæringstidspunkt"))
