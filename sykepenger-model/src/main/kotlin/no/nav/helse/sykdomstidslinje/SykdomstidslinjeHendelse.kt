package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate
import java.util.*

abstract class SykdomstidslinjeHendelse(
    private val meldingsreferanseId: UUID
) : ArbeidstakerHendelse() {
    internal fun meldingsreferanseId() = meldingsreferanseId

    internal abstract fun sykdomstidslinje(fom: LocalDate?, tom: LocalDate): ConcreteSykdomstidslinje
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun valider(): Aktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)
}
