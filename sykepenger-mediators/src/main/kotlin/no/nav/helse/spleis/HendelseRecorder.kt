package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import javax.sql.DataSource

class HendelseRecorder(private val dataSource: DataSource,
                       private val probe: PostgresProbe = PostgresProbe) : HendelseListener {

    override fun onNySøknad(søknad: ModelNySøknad, aktivitetslogger: Aktivitetslogger) {
        lagreHendelse(søknad)
    }

    override fun onInntektsmelding(inntektsmelding: ModelInntektsmelding) {
        lagreHendelse(inntektsmelding)
    }

    override fun onSendtSøknad(søknad: ModelSendtSøknad) {
        lagreHendelse(søknad)
    }

    override fun onYtelser(ytelser: ModelYtelser) {
        lagreHendelse(ytelser)
    }

    override fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        lagreHendelse(manuellSaksbehandling)
    }

    override fun onVilkårsgrunnlag(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        lagreHendelse(vilkårsgrunnlag)
    }

    private fun lagreHendelse(hendelse: ArbeidstakerHendelse) {
        if (!hendelse.kanBehandles()) return

        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO hendelse (id, aktor_id, fnr, type, rapportertdato, data) VALUES (?, ?, ?, ?, ?, (to_json(?::json)))",
                hendelse.hendelseId().toString(), hendelse.aktørId(), hendelse.fødselsnummer(), hendelse.hendelsetype().name, hendelse.rapportertdato(), hendelse.toJson()).asExecute)
        }.also {
            probe.hendelseSkrevetTilDb()
        }
    }
}
