package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt

sealed class Inntektsopplysning(
    val id: UUID,
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
) {
    internal fun fastsattÅrsinntekt() = beløp
    internal fun omregnetÅrsinntekt() = when (this) {
        is Infotrygd,
        is Inntektsmeldinginntekt,
        is Saksbehandler,
        is IkkeRapportert,
        is SkattSykepengegrunnlag -> this
        is SkjønnsmessigFastsatt -> this.omregnetÅrsinntekt!!
    }

    internal fun overstyresAv(ny: Inntektsopplysning): Inntektsopplysning {
        return when (this) {
            // infotrygd kan ikke overstyres
            is Infotrygd -> this

            is Inntektsmeldinginntekt,
            is Saksbehandler,
            is SkjønnsmessigFastsatt,
            is IkkeRapportert,
            is SkattSykepengegrunnlag -> when (ny) {
                is Inntektsmeldinginntekt -> when (this) {
                    // erstatter skjønnsmessig inntekt hvis inntektsmeldingen har annet beløp enn den inntekten
                    // som ligger bak den skjønnsmessige. i praksis medfører det at det skjønnsmessige sykepengegrunnlaget "rulles tilbake"
                    is SkjønnsmessigFastsatt -> when (erOmregnetÅrsinntektEndret(ny, this)) {
                        true -> ny
                        else -> this.kopierMed(ny)
                    }
                    // inntektsmelding tillates bare hvis inntekten er i samme måned.
                    // hvis det er flere AG med ulik fom, så kan f.eks. skjæringstidspunktet være i en måned og inntektmåneden være i en annen mnd
                    else -> when (ny.dato.yearMonth == this.dato.yearMonth) {
                        true -> ny
                        else -> this
                    }
                }
                // bare sett inn ny inntekt hvis beløp er ulikt (speil sender inntekt- og refusjonoverstyring i samme melding)
                is Saksbehandler -> when (ny.fastsattÅrsinntekt() != this.omregnetÅrsinntekt().fastsattÅrsinntekt()) {
                    true -> ny.kopierMed(this)
                    false -> this
                }
                is SkjønnsmessigFastsatt -> ny.kopierMed(this)

                is Infotrygd,
                is IkkeRapportert,
                is SkattSykepengegrunnlag -> error("${ny::class.simpleName} kan ikke erstatte annen inntekt")
            }
        }
    }

    final override fun equals(other: Any?) = other is Inntektsopplysning && erSamme(other)

    final override fun hashCode(): Int {
        var result = dato.hashCode()
        result = 31 * result + tidsstempel.hashCode() * 31
        return result
    }

    protected abstract fun erSamme(other: Inntektsopplysning): Boolean

    internal open fun subsumerSykepengegrunnlag(
        subsumsjonslogg: Subsumsjonslogg,
        organisasjonsnummer: String,
        startdatoArbeidsforhold: LocalDate?
    ) {
    }

    internal open fun subsumerArbeidsforhold(
        subsumsjonslogg: Subsumsjonslogg,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) = apply {
        subsumsjonslogg.logg(
            `§ 8-15`(
                skjæringstidspunkt = dato,
                organisasjonsnummer = organisasjonsnummer,
                inntekterSisteTreMåneder = emptyList(),
                forklaring = forklaring,
                oppfylt = oppfylt
            )
        )
    }

    internal open fun gjenbrukbarInntekt(beløp: Inntekt? = null): Inntektsmeldinginntekt? = null

    internal fun lagreTidsnærInntekt(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        orgnummer: String,
        beløp: Inntekt? = null
    ) {
        val gjenbrukbarInntekt = gjenbrukbarInntekt(beløp) ?: return
        arbeidsgiver.lagreTidsnærInntektsmelding(
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummer = orgnummer,
            inntektsmeldinginntekt = gjenbrukbarInntekt,
            aktivitetslogg = aktivitetslogg,
            nyArbeidsgiverperiode = nyArbeidsgiverperiode
        )
    }

    internal open fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        orgnummer: String,
        saksbehandlerOverstyring: OverstyrArbeidsgiveropplysninger
    ) {
    }

    internal companion object {
        internal fun erOmregnetÅrsinntektEndret(før: Inntektsopplysning, etter: Inntektsopplysning) =
            erOmregnetÅrsinntektEndret(listOf(før), listOf(etter))

        internal fun erOmregnetÅrsinntektEndret(før: List<Inntektsopplysning>, etter: List<Inntektsopplysning>) =
            omregnetÅrsinntekt(før) != omregnetÅrsinntekt(etter)

        private fun omregnetÅrsinntekt(liste: List<Inntektsopplysning>) = liste
            .map { it.omregnetÅrsinntekt().beløp }
            .map { it.årlig.toInt() }

        internal fun List<Inntektsopplysning>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.dato }.size <= 1 && none { it is SkattSykepengegrunnlag || it is IkkeRapportert }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
        }

        internal fun List<Inntektsopplysning>.validerSkjønnsmessigAltEllerIntet() {
            check(all { it is SkjønnsmessigFastsatt } || none { it is SkjønnsmessigFastsatt }) { "Enten så må alle inntektsopplysninger var skjønnsmessig fastsatt, eller så må ingen være det" }
        }

        internal fun gjenopprett(
            dto: InntektsopplysningInnDto,
            inntekter: MutableMap<UUID, Inntektsopplysning>
        ): Inntektsopplysning {
            val inntektsopplysning = inntekter.getOrPut(dto.id) {
                when (dto) {
                    is InntektsopplysningInnDto.IkkeRapportertDto -> IkkeRapportert.gjenopprett(dto)
                    is InntektsopplysningInnDto.InfotrygdDto -> Infotrygd.gjenopprett(dto)
                    is InntektsopplysningInnDto.InntektsmeldingDto -> Inntektsmeldinginntekt.gjenopprett(dto)
                    is InntektsopplysningInnDto.SaksbehandlerDto -> Saksbehandler.gjenopprett(dto, inntekter)
                    is InntektsopplysningInnDto.SkattSykepengegrunnlagDto -> SkattSykepengegrunnlag.gjenopprett(dto)
                    is InntektsopplysningInnDto.SkjønnsmessigFastsattDto -> SkjønnsmessigFastsatt.gjenopprett(
                        dto,
                        inntekter
                    )
                }
            }
            return inntektsopplysning
        }
    }

    internal abstract fun dto(): InntektsopplysningUtDto
}
