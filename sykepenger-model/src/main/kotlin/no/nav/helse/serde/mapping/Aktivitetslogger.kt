package no.nav.helse.serde.mapping

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.serde.AktivitetsloggerData
import no.nav.helse.serde.reflection.ReflectClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogger(aktivitetsloggerData: AktivitetsloggerData): Aktivitetslogger {
    val aktivitetslogger = Aktivitetslogger(aktivitetsloggerData.originalMessage)

    val aktivitetClass: ReflectClass = ReflectClass.getNestedClass<Aktivitetslogger>("Aktivitet")
    val alvorlighetsgradClass: ReflectClass = ReflectClass.getNestedClass<Aktivitetslogger>("Alvorlighetsgrad")

    val aktiviteter = aktivitetslogger.get<Aktivitetslogger, MutableList<Any>>("aktiviteter")
    aktivitetsloggerData.aktiviteter.forEach {
        aktiviteter.add(
            aktivitetClass.getInstance(
                alvorlighetsgradClass.getEnumValue(it.alvorlighetsgrad.name),
                it.melding,
                it.tidsstempel
            )
        )
    }

    return aktivitetslogger
}
