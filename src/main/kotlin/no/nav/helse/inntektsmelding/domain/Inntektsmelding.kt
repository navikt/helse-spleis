package no.nav.helse.inntektsmelding.domain

import no.nav.inntektsmeldingkontrakt.Inntektsmelding

fun Inntektsmelding.sisteDagIArbeidsgiverPeriode() = arbeidsgiverperioder.maxBy { it.tom }?.tom