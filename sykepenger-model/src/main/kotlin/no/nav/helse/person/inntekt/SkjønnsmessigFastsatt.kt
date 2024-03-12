package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Inntekt

class SkjønnsmessigFastsatt internal constructor(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    private val overstyrtInntekt: Inntektsopplysning?,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {
    constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) : this(UUID.randomUUID(), dato, hendelseId, beløp, null, tidsstempel)

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.preVisitSkjønnsmessigFastsatt(this, id, dato, hendelseId, beløp, tidsstempel)
        overstyrtInntekt?.accept(visitor)
        visitor.postVisitSkjønnsmessigFastsatt(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun omregnetÅrsinntekt(): Inntektsopplysning {
        return checkNotNull(overstyrtInntekt) { "overstyrt inntekt kan ikke være null" }.omregnetÅrsinntekt()
    }

    override fun lagreTidsnærInntekt(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?,
        refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger,
        orgnummer: String,
        beløp: Inntekt?
    ) {
        checkNotNull(overstyrtInntekt) { "overstyrt inntekt kan ikke være null" }.lagreTidsnærInntekt(skjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom, refusjonsopplysninger, orgnummer, beløp)
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun overstyrer(gammel: Saksbehandler) = kopierMed(gammel)
    override fun overstyrer(gammel: SkjønnsmessigFastsatt) = kopierMed(gammel)
    override fun overstyrer(gammel: IkkeRapportert) = kopierMed(gammel)
    override fun overstyrer(gammel: SkattSykepengegrunnlag) = kopierMed(gammel)
    override fun overstyrer(gammel: Inntektsmelding) = kopierMed(gammel)

    private fun kopierMed(overstyrtInntekt: Inntektsopplysning) =
        SkjønnsmessigFastsatt(id, dato, hendelseId, beløp, overstyrtInntekt, tidsstempel)

    override fun erSamme(other: Inntektsopplysning) =
        other is SkjønnsmessigFastsatt && this.dato == other.dato && this.beløp == other.beløp
    override fun dto() =
        InntektsopplysningDto.SkjønnsmessigFastsattDto(
            id = id,
            hendelseId = hendelseId,
            dato = dato,
            beløp = beløp.dtoMånedligDouble(),
            tidsstempel = tidsstempel,
            overstyrtInntekt = overstyrtInntekt?.dto()!!
        )

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningDto.SkjønnsmessigFastsattDto, inntekter: Map<UUID, Inntektsopplysning>) =
            SkjønnsmessigFastsatt(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel,
                overstyrtInntekt = inntekter.getValue(dto.overstyrtInntekt.id)
            )
    }
}