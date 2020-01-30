package no.nav.helse.person

interface AktivitetsloggerVisitor {
    fun preVisitAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}
    fun visitInfo(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {}
    fun visitWarn(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {}
    fun visitError(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {}
    fun visitSevere(aktivitet: Aktivitetslogger.Aktivitet, melding: String, tidsstempel: String) {}
    fun postVisitAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}

}
