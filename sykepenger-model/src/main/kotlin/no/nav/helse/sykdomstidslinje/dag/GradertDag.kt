package no.nav.helse.sykdomstidslinje.dag

import java.time.LocalDate

internal abstract class GradertDag(dato: LocalDate, internal val grad: Double): Dag(dato)
