package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.hendelser.Inntektsmelding as InntektsmeldingHendelse

sealed class Inntektsopplysning protected constructor(
    protected val id: UUID,
    protected val hendelseId: UUID,
    protected val dato: LocalDate,
    protected val beløp: Inntekt,
    protected val tidsstempel: LocalDateTime
) {
    internal abstract fun accept(visitor: InntektsopplysningVisitor)
    internal fun fastsattÅrsinntekt() = beløp
    internal open fun omregnetÅrsinntekt() = this
    internal fun overstyresAv(ny: Inntektsopplysning): Inntektsopplysning {
        if (!kanOverstyresAv(ny)) return this
        return blirOverstyrtAv(ny)
    }

    protected abstract fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning

    protected open fun kanOverstyresAv(ny: Inntektsopplysning): Boolean {
        // kun saksbehandlerinntekt eller annen inntektsmelding kan overstyre inntektsmelding-inntekt
        if (ny is SkjønnsmessigFastsatt) return true
        if (ny is Saksbehandler){
            return when {
                // hvis inntekten er skjønnsmessig fastsatt og det overstyres til samme omregnede årsinntekt, så beholdes den skjønnsmessig fastsatte inntekten
                this is SkjønnsmessigFastsatt && this.omregnetÅrsinntekt().fastsattÅrsinntekt() == ny.fastsattÅrsinntekt() -> false
                this is SkjønnsmessigFastsatt -> true
                else -> ny.fastsattÅrsinntekt() != this.beløp
            }
        }
        if (ny !is Inntektsmelding) return false
        val måned = this.dato.withDayOfMonth(1) til this.dato.withDayOfMonth(this.dato.lengthOfMonth())
        return ny.dato in måned
    }

    internal open fun overstyrer(gammel: IkkeRapportert) = this
    internal open fun overstyrer(gammel: SkattSykepengegrunnlag) = this
    internal open fun overstyrer(gammel: Inntektsmelding) = this
    internal open fun overstyrer(gammel: Saksbehandler): Inntektsopplysning {
        throw IllegalStateException("Kan ikke overstyre saksbehandler-inntekt")
    }
    internal open fun overstyrer(gammel: SkjønnsmessigFastsatt): Inntektsopplysning {
        throw IllegalStateException("Kan ikke overstyre skjønnsmessig fastsatt-inntekt")
    }

    final override fun equals(other: Any?) = other is Inntektsopplysning && erSamme(other)

    final override fun hashCode(): Int {
        var result = dato.hashCode()
        result = 31 * result + tidsstempel.hashCode() * 31
        return result
    }

    protected abstract fun erSamme(other: Inntektsopplysning): Boolean

    internal open fun subsumerSykepengegrunnlag(subsumsjonslogg: Subsumsjonslogg, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) { }

    internal open fun subsumerArbeidsforhold(
        subsumsjonslogg: Subsumsjonslogg,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) = apply {
        subsumsjonslogg.`§ 8-15`(
            skjæringstidspunkt = dato,
            organisasjonsnummer = organisasjonsnummer,
            inntekterSisteTreMåneder = emptyList(),
            forklaring = forklaring,
            oppfylt = oppfylt
        )
    }

    internal open fun lagreTidsnærInntekt(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?,
        refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger,
        orgnummer: String,
        beløp: Inntekt? = null
    ) {}

    internal open fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        inntektsmelding: InntektsmeldingHendelse
    ) {}

    internal open fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        orgnummer: String,
        saksbehandlerOverstyring: OverstyrArbeidsgiveropplysninger
    ) {}

    internal fun omregnetÅrsinntekt(builder: GodkjenningsbehovBuilder, orgnummer: String) {
        builder.omregnedeÅrsinntekter(orgnummer, omregnetÅrsinntekt().beløp.reflection { årlig, _, _, _ -> årlig })
    }

    internal companion object {
        internal fun erOmregnetÅrsinntektEndret(før: Inntektsopplysning, etter: Inntektsopplysning) =
            erOmregnetÅrsinntektEndret(listOf(før), listOf(etter))

        internal fun erOmregnetÅrsinntektEndret(før: List<Inntektsopplysning>, etter: List<Inntektsopplysning>) =
            omregnetÅrsinntekt(før) != omregnetÅrsinntekt(etter)

        private fun omregnetÅrsinntekt(liste: List<Inntektsopplysning>): List<Inntekt> = liste.map { it.omregnetÅrsinntekt().beløp }

        internal fun List<Inntektsopplysning>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.dato }.size <= 1 && none { it is SkattSykepengegrunnlag || it is IkkeRapportert }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
        }

        internal fun List<Inntektsopplysning>.validerSkjønnsmessigAltEllerIntet() {
            check(all { it is SkjønnsmessigFastsatt } || none { it is SkjønnsmessigFastsatt }) {"Enten så må alle inntektsopplysninger var skjønnsmessig fastsatt, eller så må ingen være det"}
        }

        internal fun gjenopprett(dto: InntektsopplysningInnDto, inntekter: MutableMap<UUID, Inntektsopplysning>): Inntektsopplysning {
            val inntektsopplysning = inntekter.getOrPut(dto.id) {
                when (dto) {
                    is InntektsopplysningInnDto.IkkeRapportertDto -> IkkeRapportert.gjenopprett(dto)
                    is InntektsopplysningInnDto.InfotrygdDto -> Infotrygd.gjenopprett(dto)
                    is InntektsopplysningInnDto.InntektsmeldingDto -> Inntektsmelding.gjenopprett(dto)
                    is InntektsopplysningInnDto.SaksbehandlerDto -> Saksbehandler.gjenopprett(dto, inntekter)
                    is InntektsopplysningInnDto.SkattSykepengegrunnlagDto -> SkattSykepengegrunnlag.gjenopprett(dto)
                    is InntektsopplysningInnDto.SkjønnsmessigFastsattDto -> SkjønnsmessigFastsatt.gjenopprett(dto, inntekter)
                }
            }
            return inntektsopplysning
        }
    }

    internal abstract fun dto(): InntektsopplysningUtDto
}