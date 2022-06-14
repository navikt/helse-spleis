package no.nav.helse.hendelser


import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk

class Ytelser(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk?,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val dødsinfo: Dødsinfo,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        utbetalingshistorikk?.oppdaterHistorikk(historikk)
    }

    internal fun lagreDødsdato(person: Person) {
        if (dødsinfo.dødsdato == null) return
        person.lagreDødsdato(dødsinfo.dødsdato)
    }

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt)
        dagpenger.valider(this, skjæringstidspunkt)
        if (foreldrepermisjon.overlapper(periode)) error("Det er utbetalt foreldrepenger i samme periode.")
        if (pleiepenger.overlapper(periode)) error("Det er utbetalt pleiepenger i samme periode.")
        if (omsorgspenger.overlapper(periode)) error("Det er utbetalt omsorgspenger i samme periode.")
        if (opplæringspenger.overlapper(periode)) error("Det er utbetalt opplæringspenger i samme periode.")
        if (institusjonsopphold.overlapper(periode)) error("Det er institusjonsopphold i perioden. Vurder retten til sykepenger.")

        return !hasErrorsOrWorse()
    }
}
