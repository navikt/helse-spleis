package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import java.util.*

abstract class SykdomstidslinjeHendelse(
    hendelseId: UUID,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(hendelseId, aktivitetslogger, aktivitetslogg) {
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun valider(): Aktivitetslogger

    internal abstract fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger)

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)
}
