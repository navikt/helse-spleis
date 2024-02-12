package no.nav.helse.person.aktivitetslogg

interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(aktivitet: Aktivitet.Info) {}

    fun visitVarsel(aktivitet: Aktivitet.Varsel) {}

    fun visitBehov(aktivitet: Aktivitet.Behov) {}

    fun visitFunksjonellFeil(aktivitet: Aktivitet.FunksjonellFeil) {}

    fun visitLogiskFeil(aktivitet: Aktivitet.LogiskFeil) {}

    fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
}