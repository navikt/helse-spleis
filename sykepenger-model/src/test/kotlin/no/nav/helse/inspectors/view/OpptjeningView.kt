package no.nav.helse.inspectors.view

import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag

internal data class ArbeidstakerOpptjeningView(val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, val opptjeningsdager: Int, val erOppfylt: Boolean)

internal fun ArbeidstakerOpptjening.view() = ArbeidstakerOpptjeningView(arbeidsforhold = arbeidsforhold, opptjeningsdager = opptjeningsdager, erOppfylt = erOppfylt())
