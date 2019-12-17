package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.hendelser.*
import no.nav.helse.sak.ArbeidstakerHendelse
import javax.sql.DataSource

class HendelseRecorder(private val dataSource: DataSource,
                       private val probe: PostgresProbe = PostgresProbe) : HendelseListener {

    override fun onNySøknad(søknad: NySøknad) {
        lagreHendelse(søknad)
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        lagreHendelse(inntektsmelding)
    }

    override fun onSendtSøknad(søknad: SendtSøknad) {
        lagreHendelse(søknad)
    }

    override fun onYtelser(ytelser: Ytelser) {
        lagreHendelse(ytelser)
    }

    override fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        lagreHendelse(manuellSaksbehandling)
    }

    private fun lagreHendelse(hendelse: ArbeidstakerHendelse) {
        if (!hendelse.kanBehandles()) return

        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO hendelse (aktor_id, type, opprettet, data) VALUES (?, ?, ?, (to_json(?::json)))",
                hendelse.aktørId(), hendelse.hendelsetype().name, hendelse.opprettet(), hendelse.toJson()).asExecute)
        }.also {
            probe.hendelseSkrevetTilDb()
        }
    }
}
