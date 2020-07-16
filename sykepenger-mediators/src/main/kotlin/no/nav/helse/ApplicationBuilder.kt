package no.nav.helse

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()

    private val hendelseRepository = HendelseRepository(dataSource)
    private val personRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao = LagrePersonDao(dataSource)
    private val rapidsConnection = RapidApplication.create(env)

    private val hendelseMediator = HendelseMediator(
        rapidsConnection = rapidsConnection,
        personRepository = personRepository,
        hendelseRepository = hendelseRepository,
        lagrePersonDao = lagrePersonDao
    )

    init {
        rapidsConnection.register(this)
        MessageMediator(
            rapidsConnection = rapidsConnection,
            hendelseMediator = hendelseMediator,
            hendelseRepository = hendelseRepository
        )
    }

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }
}
