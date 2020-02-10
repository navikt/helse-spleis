package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

class ModelArbeidsforholdReflect(modelArbeidsforhold: ModelVilkårsgrunnlag.ModelArbeidsforhold) {
    private val arbeidsforhold: List<ModelVilkårsgrunnlag.Arbeidsforhold> = modelArbeidsforhold["arbeidsforhold"]

    fun toList() = arbeidsforhold.map { it.toMap() }

    private fun ModelVilkårsgrunnlag.Arbeidsforhold.toMap(): MutableMap<String, Any?> = mutableMapOf(
        "orgnummer" to this.orgnummer,
        "fom" to this.fom,
        "tom" to this.tom
    )
}
