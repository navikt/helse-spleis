package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav a`
import no.nav.helse.etterlevelse.`§ 8-29`
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat

internal class SkattSykepengegrunnlag(
    id: UUID,
    inntektsdata: Inntektsdata,
    val inntektsopplysninger: List<Skatteopplysning>,
    val ansattPerioder: List<AnsattPeriode>
) : SkatteopplysningSykepengegrunnlag(id, inntektsdata) {
    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
            val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
            return SkattSykepengegrunnlag(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                inntektsopplysninger = skatteopplysninger,
                ansattPerioder = dto.ansattPerioder.map { AnsattPeriode.gjenopprett(it) },
            )
        }
    }

    override fun subsumerSykepengegrunnlag(subsumsjonslogg: Subsumsjonslogg, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        subsumsjonslogg.logg(
            `§ 8-28 ledd 3 bokstav a`(
                organisasjonsnummer = organisasjonsnummer,
                skjæringstidspunkt = inntektsdata.dato,
                inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
            )
        )
        subsumsjonslogg.logg(
            `§ 8-29`(
                skjæringstidspunkt = inntektsdata.dato,
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
                skjæringstidspunkt = inntektsdata.dato,
                organisasjonsnummer = organisasjonsnummer,
                inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
                forklaring = forklaring,
                oppfylt = oppfylt
            )
        )
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattSykepengegrunnlag && this.inntektsdata.funksjoneltLik(other.inntektsdata)
    }

    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
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
