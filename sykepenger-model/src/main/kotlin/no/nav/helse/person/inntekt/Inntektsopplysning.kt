package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsdataInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsdataUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt

data class Inntektsdata(
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
) {

    fun funksjoneltLik(other: Inntektsdata) =
        this.dato == other.dato && this.beløp == other.beløp

    fun dto() = InntektsdataUtDto(
        hendelseId = hendelseId,
        dato = dato,
        beløp = beløp.dto(),
        tidsstempel = tidsstempel
    )

    companion object {
        fun ingen(hendelseId: UUID, dato: LocalDate, tidsstempel: LocalDateTime = LocalDateTime.now()) = Inntektsdata(
            hendelseId = hendelseId,
            dato = dato,
            beløp = Inntekt.INGEN,
            tidsstempel = tidsstempel
        )

        fun gjenopprett(dto: InntektsdataInnDto) = Inntektsdata(
            hendelseId = dto.hendelseId,
            dato = dto.dato,
            beløp = Inntekt.gjenopprett(dto.beløp),
            tidsstempel = dto.tidsstempel
        )
    }
}

sealed class Inntektsopplysning(
    val id: UUID,
    val inntektsdata: Inntektsdata
) {
    internal fun fastsattÅrsinntekt() = inntektsdata.beløp
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
                    else -> when (ny.inntektsdata.dato.yearMonth == this.inntektsdata.dato.yearMonth) {
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

    fun funksjoneltLik(other: Inntektsopplysning) =
        this::class == other::class && this.inntektsdata.funksjoneltLik(other.inntektsdata)

    internal fun subsumerArbeidsforhold(
        subsumsjonslogg: Subsumsjonslogg,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        subsumsjonslogg.logg(
            `§ 8-15`(
                skjæringstidspunkt = inntektsdata.dato,
                organisasjonsnummer = organisasjonsnummer,
                inntekterSisteTreMåneder = when (this) {
                    is SkattSykepengegrunnlag -> inntektsopplysninger.subsumsjonsformat()
                    is Infotrygd,
                    is Inntektsmeldinginntekt,
                    is Saksbehandler,
                    is IkkeRapportert,
                    is SkjønnsmessigFastsatt -> emptyList()
                },
                forklaring = forklaring,
                oppfylt = oppfylt
            )
        )
    }

    internal fun gjenbrukbarInntekt(beløp: Inntekt? = null): Inntektsmeldinginntekt? = when (this) {
        is Inntektsmeldinginntekt -> beløp?.let { Inntektsmeldinginntekt(inntektsdata.dato, inntektsdata.hendelseId, it, kilde, inntektsdata.tidsstempel) } ?: this
        is Saksbehandler -> checkNotNull(overstyrtInntekt) { "overstyrt inntekt kan ikke være null" }.gjenbrukbarInntekt(beløp ?: this.inntektsdata.beløp)
        is SkjønnsmessigFastsatt -> checkNotNull(overstyrtInntekt) { "overstyrt inntekt kan ikke være null" }.gjenbrukbarInntekt(beløp)

        is Infotrygd,
        is IkkeRapportert,
        is SkattSykepengegrunnlag -> null
    }

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

    internal companion object {
        internal fun erOmregnetÅrsinntektEndret(før: Inntektsopplysning, etter: Inntektsopplysning) =
            erOmregnetÅrsinntektEndret(listOf(før), listOf(etter))

        internal fun erOmregnetÅrsinntektEndret(før: List<Inntektsopplysning>, etter: List<Inntektsopplysning>) =
            omregnetÅrsinntekt(før) != omregnetÅrsinntekt(etter)

        private fun omregnetÅrsinntekt(liste: List<Inntektsopplysning>) = liste
            .map { it.omregnetÅrsinntekt().inntektsdata.beløp }
            .map { it.årlig.toInt() }

        internal fun List<Inntektsopplysning>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.inntektsdata.dato }.size <= 1 && none { it is SkattSykepengegrunnlag || it is IkkeRapportert }) return
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
