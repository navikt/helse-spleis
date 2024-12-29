package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav a`
import no.nav.helse.etterlevelse.`§ 8-29`
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.økonomi.Inntekt

internal class SkattSykepengegrunnlag private constructor(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    beløp: Inntekt,
    val inntektsopplysninger: List<Skatteopplysning>,
    val ansattPerioder: List<AnsattPeriode>,
    tidsstempel: LocalDateTime
) : SkatteopplysningSykepengegrunnlag(id, hendelseId, dato, beløp, tidsstempel) {
    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
            val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
            return SkattSykepengegrunnlag(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                beløp = Skatteopplysning.omregnetÅrsinntekt(skatteopplysninger),
                inntektsopplysninger = skatteopplysninger,
                ansattPerioder = dto.ansattPerioder.map { AnsattPeriode.gjenopprett(it) },
                tidsstempel = dto.tidsstempel
            )
        }
    }

    internal constructor(
        hendelseId: UUID,
        dato: LocalDate,
        inntektsopplysninger: List<Skatteopplysning>,
        ansattPerioder: List<AnsattPeriode>,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : this(UUID.randomUUID(), hendelseId, dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), inntektsopplysninger, ansattPerioder, tidsstempel)

    override fun kanOverstyresAv(ny: Inntektsopplysning): Boolean {
        if (ny !is Inntektsmeldinginntekt) return true
        return super.kanOverstyresAv(ny)
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun subsumerSykepengegrunnlag(subsumsjonslogg: Subsumsjonslogg, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        subsumsjonslogg.logg(
            `§ 8-28 ledd 3 bokstav a`(
                organisasjonsnummer = organisasjonsnummer,
                skjæringstidspunkt = dato,
                inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
            )
        )
        subsumsjonslogg.logg(
            `§ 8-29`(
                skjæringstidspunkt = dato,
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                inntektsopplysninger = inntektsopplysninger.subsumsjonsformat(),
                organisasjonsnummer = organisasjonsnummer
            )
        )
    }

    override fun subsumerArbeidsforhold(
        subsumsjonslogg: Subsumsjonslogg,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) = apply {
        subsumsjonslogg.logg(
            `§ 8-15`(
                skjæringstidspunkt = dato,
                organisasjonsnummer = organisasjonsnummer,
                inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
                forklaring = forklaring,
                oppfylt = oppfylt
            )
        )
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattSykepengegrunnlag && this.dato == other.dato && this.inntektsopplysninger == other.inntektsopplysninger
    }

    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            id = id,
            hendelseId = hendelseId,
            dato = dato,
            beløp = beløp.dto(),
            tidsstempel = tidsstempel,
            inntektsopplysninger = inntektsopplysninger.map { it.dto() },
            ansattPerioder = ansattPerioder.map { it.dto() })
}

class AnsattPeriode(
    private val ansattFom: LocalDate,
    private val ansattTom: LocalDate?
) {
    companion object {
        fun gjenopprett(dto: AnsattPeriodeDto) = AnsattPeriode(
            ansattFom = dto.fom,
            ansattTom = dto.tom
        )
    }

    internal fun dto() = AnsattPeriodeDto(ansattFom, ansattTom)
}
