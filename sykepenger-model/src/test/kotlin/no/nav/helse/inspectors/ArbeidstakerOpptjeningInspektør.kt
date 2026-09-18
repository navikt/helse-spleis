package no.nav.helse.inspectors

import no.nav.helse.person.ArbeidstakerOpptjening

internal val ArbeidstakerOpptjening.inspektør get() = ArbeidstakerOpptjeningInspektør(this)

internal class ArbeidstakerOpptjeningInspektør(opptjening: ArbeidstakerOpptjening) {
    val arbeidsforhold = opptjening.arbeidsforhold.groupBy(keySelector = { it.orgnummer }) { it.ansattPerioder.map { periode -> Triple(periode.ansattFom, periode.ansattTom, periode.deaktivert) } }
}
