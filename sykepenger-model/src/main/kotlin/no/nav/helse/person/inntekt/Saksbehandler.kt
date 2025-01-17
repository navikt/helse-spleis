package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav b`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav c`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 5`
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.økonomi.Inntekt

class Saksbehandler internal constructor(
    id: UUID,
    inntektsdata: Inntektsdata,
    val forklaring: String?,
    val subsumsjon: Subsumsjon?,
    val overstyrtInntekt: Inntektsopplysning?
) : Inntektsopplysning(id, inntektsdata) {
    constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, forklaring: String, subsumsjon: Subsumsjon?, tidsstempel: LocalDateTime) :
        this(UUID.randomUUID(), Inntektsdata(hendelseId, dato, beløp, tidsstempel), forklaring, subsumsjon, null)

    override fun gjenbrukbarInntekt(beløp: Inntekt?) = overstyrtInntekt?.gjenbrukbarInntekt(beløp ?: this.inntektsdata.beløp)

    fun kopierMed(overstyrtInntekt: Inntektsopplysning) =
        Saksbehandler(id, inntektsdata, forklaring, subsumsjon, overstyrtInntekt)

    override fun erSamme(other: Inntektsopplysning) =
        other is Saksbehandler && this.inntektsdata.funksjoneltLik(other.inntektsdata)

    override fun subsumerSykepengegrunnlag(subsumsjonslogg: Subsumsjonslogg, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        if (subsumsjon == null) return
        requireNotNull(forklaring) { "Det skal være en forklaring fra saksbehandler ved overstyring av inntekt" }
        if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref
            && subsumsjon.ledd == Ledd.LEDD_3.nummer
            && subsumsjon.bokstav == Bokstav.BOKSTAV_B.ref.toString()
        ) {
            requireNotNull(startdatoArbeidsforhold) { "Fant ikke aktivt arbeidsforhold for skjæringstidspunktet i arbeidsforholdshistorikken" }
            subsumsjonslogg.logg(
                `§ 8-28 ledd 3 bokstav b`(
                    organisasjonsnummer = organisasjonsnummer,
                    startdatoArbeidsforhold = startdatoArbeidsforhold,
                    overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                    skjæringstidspunkt = inntektsdata.dato,
                    forklaring = forklaring,
                    grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                    grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                )
            )
        } else if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref
            && subsumsjon.ledd == Ledd.LEDD_3.nummer
            && subsumsjon.bokstav == Bokstav.BOKSTAV_C.ref.toString()
        ) {
            subsumsjonslogg.logg(
                `§ 8-28 ledd 3 bokstav c`(
                    organisasjonsnummer = organisasjonsnummer,
                    overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                    skjæringstidspunkt = inntektsdata.dato,
                    forklaring = forklaring,
                    grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                    grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                )
            )
        } else if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_5.nummer) {
            subsumsjonslogg.logg(
                `§ 8-28 ledd 5`(
                    organisasjonsnummer = organisasjonsnummer,
                    overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                    skjæringstidspunkt = inntektsdata.dato,
                    forklaring = forklaring,
                    grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                    grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                )
            )
        }
    }

    override fun dto() =
        InntektsopplysningUtDto.SaksbehandlerDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            forklaring = forklaring,
            subsumsjon = subsumsjon?.dto(),
            overstyrtInntekt = overstyrtInntekt!!.dto().id
        )

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.SaksbehandlerDto, inntekter: Map<UUID, Inntektsopplysning>) =
            Saksbehandler(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                forklaring = dto.forklaring,
                subsumsjon = dto.subsumsjon?.let { Subsumsjon.gjenopprett(it) },
                overstyrtInntekt = inntekter.getValue(dto.overstyrtInntekt)
            )
    }
}
