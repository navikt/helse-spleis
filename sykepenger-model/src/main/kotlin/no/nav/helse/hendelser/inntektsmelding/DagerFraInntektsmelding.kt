package no.nav.helse.hendelser.inntektsmelding

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    inntektsmeldingPeriode: Periode
) {
    private val opprinneligeDager = inntektsmeldingPeriode.toSet()
    private val gjenståendeDager = opprinneligeDager.toMutableSet()


    internal fun håndterFør(periode: Periode, arbeidsgiver: Arbeidsgiver) {

    }

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver) {

    }

    internal fun håndterGjenstående(arbeidsgiver: Arbeidsgiver) {

    }
}