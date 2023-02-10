package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.økonomi.Inntekt

internal class Infotrygd(
    private val id: UUID,
    dato: LocalDate,
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(dato, tidsstempel) {

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitInfotrygd(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning) = this

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserverPort,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        throw IllegalStateException("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is Infotrygd) return false
        return this.dato == other.dato && this.beløp == other.beløp
    }
}