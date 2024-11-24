package no.nav.helse

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.Subsumsjonproducer
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.PersonDao
import no.nav.helse.spleis.monitorering.MonitoreringRiver
import no.nav.helse.spleis.monitorering.RegelmessigAvstemming

val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry.defaultRegistry, Clock.SYSTEM)

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val factory = ConsumerProducerFactory(AivenConfig.default)
    private val rapidsConnection = RapidApplication.create(env, factory, meterRegistry = meterRegistry)

    // Håndter on-prem og gcp database tilkobling forskjellig
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val STØTTER_IDENTBYTTE = env["IDENTBYTTE"]?.equals("true", ignoreCase = true) == true
    private val hendelseRepository = HendelseRepository(dataSourceBuilder.dataSource)
    private val personDao = PersonDao(dataSourceBuilder.dataSource, STØTTER_IDENTBYTTE)

    private val subsumsjonsproducer = Subsumsjonproducer.KafkaSubsumsjonproducer("tbd.subsumsjon.v1", factory.createProducer())

    private val hendelseMediator = HendelseMediator(
        hendelseRepository = hendelseRepository,
        personDao = personDao,
        versjonAvKode = versjonAvKode(env),
        støtterIdentbytte = STØTTER_IDENTBYTTE,
        subsumsjonsproducer = subsumsjonsproducer
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
