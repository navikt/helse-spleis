package no.nav.helse.person

interface AktivitetsloggerVisitor {
    fun preVisitAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}
    fun visitInfo(aktivitet: Aktivitetslogger.Aktivitet.Info, melding: String, tidsstempel: String) {}
    fun visitWarn(aktivitet: Aktivitetslogger.Aktivitet.Warn, melding: String, tidsstempel: String) {}
    fun visitNeed(aktivitet: Aktivitetslogger.Aktivitet.Need, melding: String, tidsstempel: String) {}
    fun visitError(aktivitet: Aktivitetslogger.Aktivitet.Error, melding: String, tidsstempel: String) {}
    fun visitSevere(aktivitet: Aktivitetslogger.Aktivitet.Severe, melding: String, tidsstempel: String) {}
    fun postVisitAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}

}
