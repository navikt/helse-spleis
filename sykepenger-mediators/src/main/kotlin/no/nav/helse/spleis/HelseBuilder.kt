package no.nav.helse.spleis

import no.nav.helse.sak.SakObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.streams.KafkaStreams
import java.util.*
import javax.sql.DataSource

internal class HelseBuilder(dataSource: DataSource, hendelseProducer: KafkaProducer<String, String>) {

    private val hendelseBuilder: HendelseBuilder
    private val hendelseProbe: HendelseListener = HendelseProbe()
    private val hendelseRecorder: HendelseListener = HendelseRecorder(dataSource)

    internal val sakMediator: SakMediator
    private val sakRepository: SakRepository = SakPostgresRepository(dataSource)
    private val lagreSakDao: SakObserver = LagreSakDao(dataSource)
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository = UtbetalingsreferansePostgresRepository(dataSource)
    private val lagreUtbetalingDao: SakObserver = LagreUtbetalingDao(dataSource)

    init {
        sakMediator = SakMediator(
            sakRepository = sakRepository,
            lagreSakDao = lagreSakDao,
            utbetalingsreferanseRepository = utbetalingsreferanseRepository,
            lagreUtbetalingDao = lagreUtbetalingDao,
            producer = hendelseProducer
        )

        hendelseBuilder = HendelseBuilder().apply {
            addListener(hendelseProbe)
            addListener(hendelseRecorder)
            addListener(sakMediator)
        }
    }

    fun addStateListener(stateListener: KafkaStreams.StateListener) {
        hendelseBuilder.addStateListener(stateListener)
    }

    fun start(props: Properties) {
        hendelseBuilder.start(props)
    }


    fun stop() {
        hendelseBuilder.stop()
    }
}
