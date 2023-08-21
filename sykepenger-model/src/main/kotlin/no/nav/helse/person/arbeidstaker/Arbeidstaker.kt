package no.nav.helse.person.arbeidstaker

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Melding
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

class Arbeidstaker(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    private val arbeidsgiver: String,
    private val søknad: Søknad,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : SykdomstidslinjeHendelse(meldingsreferanseId, fnr, aktørId, arbeidsgiver, LocalDateTime.now(), aktivitetslogg) {
    internal val kilde: Hendelseskilde = Hendelseskilde("Søknad", meldingsreferanseId(), LocalDateTime.now())
    override fun sykdomstidslinje() = søknad.sykdomstidslinje()
    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = søknad.valider(this, subsumsjonObserver)
    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = søknad.leggTil(hendelseIder)
}