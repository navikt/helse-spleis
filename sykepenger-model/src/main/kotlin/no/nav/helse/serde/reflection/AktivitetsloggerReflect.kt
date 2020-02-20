package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.AktivitetsloggerVisitor
import no.nav.helse.serde.AktivitetsloggerData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

fun Aktivitetslogger.toMap() = AktivitetsloggerReflect(this).toMap()

internal class AktivitetsloggerReflect(aktivitetslogger: Aktivitetslogger) {
    private val originalMessage: String? = aktivitetslogger["originalMessage"]
    private val aktiviteter = Aktivitetsloggerinspektør(aktivitetslogger).aktiviteter

    internal fun toMap() = mutableMapOf(
        "aktiviteter" to aktiviteter,
        "originalMessage" to originalMessage
    )

    private inner class Aktivitetsloggerinspektør(aktivitetslogger: Aktivitetslogger) : AktivitetsloggerVisitor {
        internal val aktiviteter = mutableListOf<Map<String, Any>>()

        init {
            aktivitetslogger.accept(this)
        }

        private fun leggTilMelding(alvorlighetsgrad: AktivitetsloggerData.Alvorlighetsgrad, melding: String, tidsstempel: String) {
            aktiviteter.add(mutableMapOf<String, Any>(
                "alvorlighetsgrad" to alvorlighetsgrad.name,
                "melding" to melding,
                "tidsstempel" to tidsstempel
            ))
        }

        private fun leggTilMelding(type: Aktivitetslogger.Aktivitet.Need.NeedType, melding: String, tidsstempel: String) {
            aktiviteter.add(mutableMapOf<String, Any>(
                "alvorlighetsgrad" to AktivitetsloggerData.Alvorlighetsgrad.NEED.name,
                "needType" to type.name,
                "melding" to melding,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitInfo(aktivitet: Aktivitetslogger.Aktivitet.Info, melding: String, tidsstempel: String) {
            leggTilMelding(AktivitetsloggerData.Alvorlighetsgrad.INFO, melding, tidsstempel)
        }

        override fun visitWarn(aktivitet: Aktivitetslogger.Aktivitet.Warn, melding: String, tidsstempel: String) {
            leggTilMelding(AktivitetsloggerData.Alvorlighetsgrad.WARN, melding, tidsstempel)
        }

        override fun visitNeed(
            aktivitet: Aktivitetslogger.Aktivitet.Need,
            type: Aktivitetslogger.Aktivitet.Need.NeedType,
            tidsstempel: String,
            melding: String
        ) {
            leggTilMelding(type, melding, tidsstempel)
        }

        override fun visitError(aktivitet: Aktivitetslogger.Aktivitet.Error, melding: String, tidsstempel: String) {
            leggTilMelding(AktivitetsloggerData.Alvorlighetsgrad.ERROR, melding, tidsstempel)
        }

        override fun visitSevere(aktivitet: Aktivitetslogger.Aktivitet.Severe, melding: String, tidsstempel: String) {
            leggTilMelding(AktivitetsloggerData.Alvorlighetsgrad.SEVERE, melding, tidsstempel)
        }
    }
}
