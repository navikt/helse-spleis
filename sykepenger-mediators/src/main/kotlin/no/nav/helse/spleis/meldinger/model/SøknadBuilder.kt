package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal abstract class SøknadBuilder {
    protected lateinit var meldingsreferanseId: UUID
    protected lateinit var sykmeldingSkrevet: LocalDateTime
    protected lateinit var fnr: String
    protected lateinit var aktørId: String
    protected lateinit var organisasjonsnummer: String
    protected lateinit var opprettet: LocalDateTime
    protected lateinit var fom: LocalDate
    protected lateinit var tom: LocalDate

    internal fun meldingsreferanseId(meldingsreferanseId: UUID) = apply { this.meldingsreferanseId = meldingsreferanseId }
    internal fun sykmeldingSkrevet(sykmeldingSkrevet: LocalDateTime) = apply { this.sykmeldingSkrevet = sykmeldingSkrevet }
    internal fun fnr(fnr: String) = apply { this.fnr = fnr }
    internal fun aktørId(aktørId: String) = apply { this.aktørId = aktørId }
    internal fun organisasjonsnummer(organisasjonsnummer: String) = apply { this.organisasjonsnummer = organisasjonsnummer }
    internal fun opprettet(opprettet: LocalDateTime) = apply { this.opprettet = opprettet }
    internal fun fom(fom: LocalDate) = apply { this.fom = fom }
    internal fun tom(tom: LocalDate) = apply { this.tom = tom }

    internal abstract fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?): SøknadBuilder
}
