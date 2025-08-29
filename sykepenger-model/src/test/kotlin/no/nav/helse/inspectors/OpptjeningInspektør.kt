package no.nav.helse.inspectors

import no.nav.helse.person.Opptjening
import no.nav.helse.person.ArbeidstakerOpptjeningView

internal val Opptjening.inspektør get() = OpptjeningInspektør(this.view() as ArbeidstakerOpptjeningView)

internal class OpptjeningInspektør(view: ArbeidstakerOpptjeningView) {
    val arbeidsforhold = view.arbeidsforhold.groupBy(keySelector = { it.orgnummer }) { it.ansattPerioder.map { Triple(it.ansattFom, it.ansattTom, it.deaktivert) } }
}
