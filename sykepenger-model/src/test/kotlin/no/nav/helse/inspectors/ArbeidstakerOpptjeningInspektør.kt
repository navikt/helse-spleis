package no.nav.helse.inspectors

import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.inspectors.view.ArbeidstakerOpptjeningView
import no.nav.helse.inspectors.view.view

internal val ArbeidstakerOpptjening.inspektør get() = ArbeidstakerOpptjeningInspektør(this.view())

internal class ArbeidstakerOpptjeningInspektør(view: ArbeidstakerOpptjeningView) {
    val arbeidsforhold = view.arbeidsforhold.groupBy(keySelector = { it.orgnummer }) { it.ansattPerioder.map { Triple(it.ansattFom, it.ansattTom, it.deaktivert) } }
}
