package no.nav.helse.inspectors

import no.nav.helse.person.Opptjening
import no.nav.helse.person.OpptjeningView

internal val Opptjening.inspektør get() = OpptjeningInspektør(this.view())

internal class OpptjeningInspektør(view: OpptjeningView) {
    val arbeidsforhold = view.arbeidsforhold.groupBy(keySelector = { it.orgnummer }) { it.ansattPerioder.map { Triple(it.ansattFom, it.ansattTom, it.deaktivert) } }
}