package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class Infotrygd(
    private val id: UUID,
    dato: LocalDate,
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Inntektsopplysning(dato, 80) {

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitInfotrygd(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning) = this

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        throw IllegalStateException("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
    }

    override fun skalErstattesAv(other: Inntektsopplysning) =
        other is Infotrygd && this.dato == other.dato

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is Infotrygd) return false
        return this.dato == other.dato && this.beløp == other.beløp
    }
}