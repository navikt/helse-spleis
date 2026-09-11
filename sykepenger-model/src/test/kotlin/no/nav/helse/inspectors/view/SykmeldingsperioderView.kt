package no.nav.helse.inspectors.view

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Sykmeldingsperioder

internal data class SykmeldingsperioderView(val perioder: List<Periode>)

internal fun Sykmeldingsperioder.view() = SykmeldingsperioderView(perioder())
