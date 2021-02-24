package no.nav.helse.serde.reflection

import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal enum class Kilde {
    SKATT_SAMMENLIGNINGSGRUNNLAG, SKATT_SYKEPENGEGRUNNLAG, INFOTRYGD, INNTEKTSMELDING, INNTEKTSOPPLYSNING_REFERANSE, SAKSBEHANDLER
}

internal abstract class InntektsopplysningReflect(
    inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning,
    private val kilde: Kilde
) {
    private val id: UUID = inntektsopplysning["id"]
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal open fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to kilde,
        "tidsstempel" to tidsstempel
    )
}

internal abstract class SkattSykepengegrunnlagReflect(
    inntektsopplysning: InntektshistorikkVol2.Skatt,
    private val kilde: Kilde
) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    private val måned: YearMonth = inntektsopplysning["måned"]
    private val type: InntektshistorikkVol2.Skatt.Inntekttype = inntektsopplysning["type"]
    private val fordel: String = inntektsopplysning["fordel"]
    private val beskrivelse: String = inntektsopplysning["beskrivelse"]

    internal fun toMap(): Map<String, Any?> {
        return mapOf(
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to kilde,
            "tidsstempel" to tidsstempel,

            "måned" to måned,
            "type" to type,
            "fordel" to fordel,
            "beskrivelse" to beskrivelse
        )
    }
}

internal class SaksbehandlerReflect(
    inntektsopplysning: InntektshistorikkVol2.Saksbehandler
) : InntektsopplysningReflect(inntektsopplysning, Kilde.SAKSBEHANDLER)

internal class InntektsmeldingReflect(
    inntektsopplysning: InntektshistorikkVol2.Inntektsmelding
) : InntektsopplysningReflect(inntektsopplysning, Kilde.INNTEKTSMELDING)

internal class InntektsopplysningKopiReflect(
    inntektsopplysning: InntektshistorikkVol2.InntektsopplysningReferanse
) {
    private val id: UUID = inntektsopplysning["id"]
    private val innslagId: UUID = inntektsopplysning["innslagId"]
    private val orginalOpplysningId: UUID = inntektsopplysning["orginalOpplysningId"]
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "innslagId" to innslagId,
        "orginalOpplysningId" to orginalOpplysningId,
        "dato" to dato,
        "hendelseId" to hendelseId,
        "kilde" to Kilde.INNTEKTSOPPLYSNING_REFERANSE,
        "tidsstempel" to tidsstempel
    )
}

internal class InfotrygdReflect(
    inntektsopplysning: InntektshistorikkVol2.Infotrygd
) : InntektsopplysningReflect(inntektsopplysning, Kilde.INFOTRYGD)

internal class SykepengegrunnlagReflect(
    inntektsopplysning: InntektshistorikkVol2.Skatt.Sykepengegrunnlag
) : SkattSykepengegrunnlagReflect(inntektsopplysning, Kilde.SKATT_SYKEPENGEGRUNNLAG)

internal class SammenligningsgrunnlagReflect(
    inntektsopplysning: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag
) : SkattSykepengegrunnlagReflect(inntektsopplysning, Kilde.SKATT_SAMMENLIGNINGSGRUNNLAG)
