package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

class ModelArbeidsforholdReflect(arbeidsforhold: Vilkårsgrunnlag.MangeArbeidsforhold) {
    private val arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = arbeidsforhold["arbeidsforhold"]

    fun toList() = arbeidsforhold.map { it.toMap() }

    private fun Vilkårsgrunnlag.Arbeidsforhold.toMap(): MutableMap<String, Any?> = mutableMapOf(
        "orgnummer" to this.orgnummer,
        "fom" to this.fom,
        "tom" to this.tom
    )
}
