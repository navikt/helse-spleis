package no.nav.helse.serde.reflection

import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal enum class Kilde {
    SKATT_SAMMENLIGNINGSGRUNNLAG, SKATT_SYKEPENGEGRUNNLAG, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
}

internal class InntektsendringReflect(inntektsendring: Inntektshistorikk.Inntektsendring) {
    private val fom: LocalDate = inntektsendring["fom"]
    private val hendelseId: UUID = inntektsendring["hendelseId"]
    private val beløp: Inntekt = inntektsendring["beløp"]
    private val kilde: Inntektshistorikk.Inntektsendring.Kilde = inntektsendring["kilde"]
    private val tidsstempel: LocalDateTime = inntektsendring["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to kilde.toString(),
        "tidsstempel" to tidsstempel
    )
}

internal class SaksbehandlerVol2Reflect(private val inntektsopplysning: InntektshistorikkVol2.Saksbehandler) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to Kilde.SAKSBEHANDLER,
        "tidsstempel" to tidsstempel
    )
}

internal class InntektsmeldingVol2Reflect(inntektsopplysning: InntektshistorikkVol2.Inntektsmelding) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to Kilde.INNTEKTSMELDING,
        "tidsstempel" to tidsstempel
    )
}

internal class InfotrygdVol2Reflect(inntektsopplysning: InntektshistorikkVol2.Infotrygd) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to Kilde.INFOTRYGD,
        "tidsstempel" to tidsstempel
    )
}

internal class SykepengegrunnlagVol2Reflect(inntektsopplysning: InntektshistorikkVol2.Skatt.Sykepengegrunnlag) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val måned: YearMonth = inntektsopplysning["måned"]
    private val type: InntektshistorikkVol2.Skatt.Inntekttype = inntektsopplysning["type"]
    private val fordel: String = inntektsopplysning["fordel"]
    private val beskrivelse: String = inntektsopplysning["beskrivelse"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to Kilde.SKATT_SYKEPENGEGRUNNLAG,
        "måned" to måned,
        "type" to type,
        "fordel" to fordel,
        "beskrivelse" to beskrivelse,
        "tidsstempel" to tidsstempel
    )
}

internal class SammenligningsgrunnlagVol2Reflect(inntektsopplysning: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag) {
    private val dato: LocalDate = inntektsopplysning["dato"]
    private val hendelseId: UUID = inntektsopplysning["hendelseId"]
    private val beløp: Inntekt = inntektsopplysning["beløp"]
    private val måned: YearMonth = inntektsopplysning["måned"]
    private val type: InntektshistorikkVol2.Skatt.Inntekttype = inntektsopplysning["type"]
    private val fordel: String = inntektsopplysning["fordel"]
    private val beskrivelse: String = inntektsopplysning["beskrivelse"]
    private val tidsstempel: LocalDateTime = inntektsopplysning["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "dato" to dato,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to Kilde.SKATT_SAMMENLIGNINGSGRUNNLAG,
        "måned" to måned,
        "type" to type,
        "fordel" to fordel,
        "beskrivelse" to beskrivelse,
        "tidsstempel" to tidsstempel
    )
}

