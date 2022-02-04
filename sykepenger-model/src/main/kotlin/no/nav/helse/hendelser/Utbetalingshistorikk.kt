package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.infotrygdhistorikk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    arbeidskategorikoder: Map<String, LocalDate>,
    harStatslønn: Boolean,
    perioder: List<Infotrygdperiode>,
    inntektshistorikk: List<Inntektsopplysning>,
    ugyldigePerioder: List<UgyldigPeriode>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    besvart: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {
    private val element = InfotrygdhistorikkElement.opprett(
        oppdatert = besvart,
        hendelseId = meldingsreferanseId(),
        perioder = perioder,
        inntekter = inntektshistorikk,
        arbeidskategorikoder = arbeidskategorikoder,
        ugyldigePerioder = ugyldigePerioder,
        harStatslønn = harStatslønn
    )

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        info("Oppdaterer Infotrygdhistorikk")
        if (!historikk.oppdaterHistorikk(element)) return info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
        info("Oppfrisket Infotrygdhistorikk ble lagret")
    }

    internal fun erRelevant(vedtaksperiodeId: UUID) =
        vedtaksperiodeId.toString() == this.vedtaksperiodeId
}
