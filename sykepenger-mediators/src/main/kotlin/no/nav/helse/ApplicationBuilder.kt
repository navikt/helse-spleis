package no.nav.helse

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.PersonDao
import no.nav.helse.spleis.monitorering.MonitoreringRiver
import no.nav.helse.spleis.monitorering.RegelmessigAvstemming

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    // Håndter on-prem og gcp database tilkobling forskjellig
    private val dataSourceBuilder = DataSourceBuilder(env)

    private val dataSource = dataSourceBuilder.getDataSource()

    private val STØTTER_IDENTBYTTE = env["IDENTBYTTE"]?.equals("true", ignoreCase = true) == true

    private val hendelseRepository = HendelseRepository(dataSource)
    private val personDao = PersonDao(dataSource, STØTTER_IDENTBYTTE)
    private val rapidsConnection = RapidApplication.create(env)

    private val hendelseMediator = HendelseMediator(
        rapidsConnection = rapidsConnection,
        hendelseRepository = hendelseRepository,
        personDao = personDao,
        versjonAvKode = versjonAvKode(env),
        støtterIdentbytte = STØTTER_IDENTBYTTE
    )

    init {
        rapidsConnection.register(this)
        MessageMediator(
            rapidsConnection = rapidsConnection,
            hendelseMediator = hendelseMediator,
            hendelseRepository = hendelseRepository
        )
        MonitoreringRiver(rapidsConnection, RegelmessigAvstemming { personDao.manglerAvstemming() })
    }

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }

    private fun versjonAvKode(env: Map<String, String>): String {
        return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
    }
}
