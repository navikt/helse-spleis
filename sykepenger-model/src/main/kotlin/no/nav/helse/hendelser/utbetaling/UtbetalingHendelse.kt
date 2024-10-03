package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_2
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT_MED_FEIL
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AVVIST
import no.nav.helse.utbetalingslinjer.Oppdragstatus.FEIL

class UtbetalingHendelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    override val fagsystemId: String,
    override val utbetalingId: UUID,
    override val status: Oppdragstatus,
    override val melding: String,
    override val avstemmingsnøkkel: Long,
    override val overføringstidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer), UtbetalingHendelse