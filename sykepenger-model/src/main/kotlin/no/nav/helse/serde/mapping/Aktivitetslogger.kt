package no.nav.helse.serde.mapping

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.serde.AktivitetsloggerData
import no.nav.helse.serde.reflection.ReflectClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogger(aktivitetsloggerData: AktivitetsloggerData): Aktivitetslogger {
    val aktivitetslogger = Aktivitetslogger(aktivitetsloggerData.originalMessage)

    val aktivitetClass: ReflectClass = ReflectClass.getNestedClass<Aktivitetslogger>("Aktivitet")

    val aktiviteter = aktivitetslogger.get<Aktivitetslogger, MutableList<Any>>("aktiviteter")
    aktivitetsloggerData.aktiviteter.forEach {
        aktiviteter.add(when (it.alvorlighetsgrad) {
            AktivitetsloggerData.Alvorlighetsgrad.INFO -> aktivitetClass.getNestedClass("Info").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.WARN -> aktivitetClass.getNestedClass("Warn").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.NEED -> aktivitetClass.getNestedClass("Need").getInstance(
                Aktivitetslogger.Aktivitet.Need.NeedType.valueOf(requireNotNull(it.needType)),
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.ERROR -> aktivitetClass.getNestedClass("Error").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.SEVERE -> aktivitetClass.getNestedClass("Severe").getInstance(
                it.melding,
                it.tidsstempel
            )
        })
    }

    return aktivitetslogger
}
