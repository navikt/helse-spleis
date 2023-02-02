package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.Opptjening
import no.nav.helse.person.OpptjeningVisitor

internal val Opptjening.inspektør get() = OpptjeningInspektør(this)

internal class OpptjeningInspektør(opptjening: Opptjening) : OpptjeningVisitor {
    private lateinit var nåværendeOrgnr: String

    internal val arbeidsforhold = mutableMapOf<String, MutableList<Triple<LocalDate, LocalDate?, Boolean>>>()

    init {
        opptjening.accept(this)
    }

    override fun preVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer: String, ansattPerioder: List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold>) {
        this.nåværendeOrgnr = orgnummer
    }

    override fun visitArbeidsforhold(ansattFom: LocalDate, ansattTom: LocalDate?, deaktivert: Boolean) {
        arbeidsforhold.getOrPut(nåværendeOrgnr) { mutableListOf() }.add(Triple(ansattFom, ansattTom, deaktivert))
    }
}