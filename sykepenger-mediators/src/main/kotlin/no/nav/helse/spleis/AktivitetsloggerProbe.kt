package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.AktivitetsloggerVisitor


internal object AktivitetsloggerProbe {
    private val aktivitetscounter = Counter.build(
        "aktivitetslogger_meldinger_totals",
        "antall aktivitetsmeldinger fordelt på alvorlighetsgrad og meldingstype"
    ).labelNames("alvorlighetsgrad", "melding").register()

    private val inspektør = AktivitetsloggerCounter()

    fun inspiser(aktivitetslogger: Aktivitetslogger) = aktivitetslogger.accept(inspektør)
    fun inspiser(aktivitetException: Aktivitetslogger.AktivitetException) = aktivitetException.accept(inspektør)

    private class AktivitetsloggerCounter : AktivitetsloggerVisitor {
        override fun visitInfo(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
            aktivitetscounter.labels("info", melding).inc()
        }

        override fun visitWarn(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
            aktivitetscounter.labels("warn", melding).inc()
        }

        override fun visitNeed(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
            aktivitetscounter.labels("need", melding).inc()
        }

        override fun visitError(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
            aktivitetscounter.labels("error", melding).inc()
        }

        override fun visitSevere(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {
            aktivitetscounter.labels("severe", melding).inc()
        }
    }
}
