package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class IkkeRapportert(
    private val id: UUID,
    dato: LocalDate,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Inntektsopplysning(dato, 10) {

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitIkkeRapportert(id, dato, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning) = ny

    override fun omregnetÅrsinntekt() = Inntekt.INGEN

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        subsumsjonObserver.`§ 8-15`(
            skjæringstidspunkt = dato,
            organisasjonsnummer = organisasjonsnummer,
            inntekterSisteTreMåneder = emptyList(),
            forklaring = forklaring,
            oppfylt = oppfylt
        )
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is IkkeRapportert && this.dato == other.dato
    }
}