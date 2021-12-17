package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDateTime
import java.util.*

class Søknad(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    andreInntektskilder: List<Inntektskilde>,
    sendtTilNAV: LocalDateTime,
    permittert: Boolean,
    merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime
) : SendtSøknad(meldingsreferanseId, fnr, aktørId, orgnummer, perioder, andreInntektskilder, sendtTilNAV, permittert, merknaderFraSykmelding, sykmeldingSkrevet) {

    private var sykdomstidslinjeUtenUønsketFerieIForkant: Sykdomstidslinje? = null

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    internal fun harArbeidsdager() = perioder.filterIsInstance<Søknadsperiode.Arbeid>().isNotEmpty()

    internal fun feriedagerIForkantAvSykmeldingsperiode(): Sykdomstidslinje? {
        check(sykdomstidslinjeUtenUønsketFerieIForkant == null)
        return sykdomstidslinje().kunFeriedagerFør(sykdomsperiode.start)
    }

    override fun sykdomstidslinje() = sykdomstidslinjeUtenUønsketFerieIForkant ?: super.sykdomstidslinje()

    // registrerer feriedager i forkant av sykmeldingsperioden i søknaden som vi ikke ønsker å beholde
    // kan fjernes når søkere ikke lenger har anledning til å oppgi slik informasjon
    internal fun leggTilFeriedagerSomIkkeSkalVæreMedISykdomstidslinja(feriedagerÅFjerne: Sykdomstidslinje) {
        check(sykdomstidslinjeUtenUønsketFerieIForkant == null)
        sykdomstidslinjeUtenUønsketFerieIForkant = sykdomstidslinje().filtrerVekk(feriedagerÅFjerne)
        info("Feriedager oppgitt i forkant av sykmeldingsperiode, oversees")
    }
}
