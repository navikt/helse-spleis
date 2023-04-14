package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class IkkeRapportert(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(id, hendelseId, dato, tidsstempel) {
    internal constructor(dato: LocalDate, hendelseId: UUID, tidsstempel: LocalDateTime) : this(UUID.randomUUID(), hendelseId, dato, tidsstempel)
    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
        takeIf { this.dato == skjæringstidspunkt }

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitIkkeRapportert(id, hendelseId, dato, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning) = ny

    override fun omregnetÅrsinntekt() = Inntekt.INGEN

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) = apply {
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