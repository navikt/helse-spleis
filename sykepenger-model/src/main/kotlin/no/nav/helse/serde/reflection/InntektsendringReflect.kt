package no.nav.helse.serde.reflection

import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

internal class InntektsendringVol2Reflect(inntektsendring: InntektshistorikkVol2.Inntektsendring) {
    private val fom: LocalDate = inntektsendring["fom"]
    private val hendelseId: UUID = inntektsendring["hendelseId"]
    private val beløp: Inntekt = inntektsendring["beløp"]
    private val kilde: InntektshistorikkVol2.Inntektsendring.Kilde = inntektsendring["kilde"]
    private val tidsstempel: LocalDateTime = inntektsendring["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to kilde.toString(),
        "tidsstempel" to tidsstempel
    )
}

internal class InntektsendringSkattVol2Reflect(inntektsendring: InntektshistorikkVol2.Inntektsendring.Skatt) {
    private val fom: LocalDate = inntektsendring["fom"]
    private val hendelseId: UUID = inntektsendring["hendelseId"]
    private val beløp: Inntekt = inntektsendring["beløp"]
    private val kilde: InntektshistorikkVol2.Inntektsendring.Kilde = inntektsendring["kilde"]
    private val type: InntektshistorikkVol2.Inntektsendring.Inntekttype = inntektsendring["type"]
    private val fordel: String = inntektsendring["fordel"]
    private val beskrivelse: String = inntektsendring["beskrivelse"]
    private val tilleggsinformasjon: String? = inntektsendring["tilleggsinformasjon"]
    private val tidsstempel: LocalDateTime = inntektsendring["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to kilde.toString(),
        "type" to type,
        "fordel" to fordel,
        "beskrivelse" to beskrivelse,
        "tilleggsinformasjon" to tilleggsinformasjon,
        "tidsstempel" to tidsstempel
    )
}

internal class InntektsendringSaksbehandlerVol2Reflect(inntektsendring: InntektshistorikkVol2.Inntektsendring.Saksbehandler) {
    private val fom: LocalDate = inntektsendring["fom"]
    private val hendelseId: UUID = inntektsendring["hendelseId"]
    private val beløp: Inntekt = inntektsendring["beløp"]
    private val kilde: InntektshistorikkVol2.Inntektsendring.Kilde = inntektsendring["kilde"]
    private val begrunnelse: String = inntektsendring["begrunnelse"]
    private val tidsstempel: LocalDateTime = inntektsendring["tidsstempel"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "fom" to fom,
        "hendelseId" to hendelseId,
        "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
        "kilde" to kilde.toString(),
        "begrunnelse" to begrunnelse,
        "tidsstempel" to tidsstempel
    )
}
