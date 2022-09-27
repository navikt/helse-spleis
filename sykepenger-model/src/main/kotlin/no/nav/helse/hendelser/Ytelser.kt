package no.nav.helse.hendelser


import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.Varselkode.RV_AY_5
import no.nav.helse.person.Varselkode.RV_AY_6
import no.nav.helse.person.Varselkode.RV_AY_7
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class Ytelser(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val infotrygdhistorikk: InfotrygdhistorikkElement?,
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
        if (infotrygdhistorikk == null) return
        info("Oppdaterer Infotrygdhistorikk")
        if (!historikk.oppdaterHistorikk(infotrygdhistorikk)) return info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
        info("Oppfrisket Infotrygdhistorikk ble lagret")
    }

    internal fun lagreDødsdato(person: Person) {
        if (dødsinfo.dødsdato == null) return
        person.lagreDødsdato(dødsinfo.dødsdato)
    }

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt)
        dagpenger.valider(this, skjæringstidspunkt)
        if (foreldrepermisjon.overlapper(this, periode)) funksjonellFeil(RV_AY_5)
        if (pleiepenger.overlapper(this, periode)) funksjonellFeil(RV_AY_6)
        if (omsorgspenger.overlapper(this, periode)) funksjonellFeil(RV_AY_7)
        if (opplæringspenger.overlapper(this, periode)) funksjonellFeil("Det er utbetalt opplæringspenger i samme periode.")
        if (institusjonsopphold.overlapper(this, periode)) funksjonellFeil("Det er institusjonsopphold i perioden. Vurder retten til sykepenger.")

        return !harFunksjonelleFeilEllerVerre()
    }
}
