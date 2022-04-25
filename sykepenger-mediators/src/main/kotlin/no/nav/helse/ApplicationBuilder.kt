package no.nav.helse

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.db.EndretVedtaksperiodeDao
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.spleis.db.SlettetVedtaksperiodeDao

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    // Håndter on-prem og gcp database tilkobling forskjellig
    private val dataSourceBuilder = DataSourceBuilder(env)

    private val dataSource = dataSourceBuilder.getDataSource()

    private val hendelseRepository = HendelseRepository(dataSource)
    private val personRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao = LagrePersonDao(dataSource)
    private val slettetVedtaksperiodeDao = SlettetVedtaksperiodeDao(dataSource)
    private val endretVedtaksperiodeDao = EndretVedtaksperiodeDao(dataSource)
    private val rapidsConnection = RapidApplication.create(env)

    private val hendelseMediator = HendelseMediator(
        rapidsConnection = rapidsConnection,
        personRepository = personRepository,
        hendelseRepository = hendelseRepository,
        lagrePersonDao = lagrePersonDao,
        slettetVedtaksperiodeDao = slettetVedtaksperiodeDao,
        endretVedtaksperiodeDao = endretVedtaksperiodeDao,
        versjonAvKode = versjonAvKode(env)
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

    private fun versjonAvKode(env: Map<String, String>): String {
        return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
    }
}
