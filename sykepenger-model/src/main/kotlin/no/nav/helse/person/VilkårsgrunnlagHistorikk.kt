package no.nav.helse.person

import no.nav.helse.hendelser.Vilkårsgrunnlag
import java.time.LocalDate

internal class VilkårsgrunnlagHistorikk private constructor(
    private val historikk: MutableMap<LocalDate, VilkårsgrunnlagElement>
) {
    internal constructor() : this(mutableMapOf())

    internal fun accept(personVisitor: PersonVisitor){
        historikk.values.forEach {
            it.accept(personVisitor)
        }
    }

    internal fun lagre(vilkårsgrunnlag: Vilkårsgrunnlag, skjæringstidspunkt: LocalDate ){
        historikk.put(skjæringstidspunkt, vilkårsgrunnlag.grunnlagsdata())
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = historikk[skjæringstidspunkt]

    internal interface VilkårsgrunnlagElement{
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun isOk() : Boolean
        fun accept(personVisitor: PersonVisitor)
    }
}
