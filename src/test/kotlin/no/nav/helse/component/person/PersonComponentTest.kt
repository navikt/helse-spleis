package no.nav.helse.component.person

import org.junit.jupiter.api.Test


internal class PersonComponentTest {

    @Test
    internal fun `testtopology`() {
/*
        val sakskompleksService = SakskompleksService(
                behovProducer = mockk(relaxed = true),
                sakskompleksDao = SakskompleksDao(getDataSource(createHikariConfigFromEnvironment())))

        val builder = StreamsBuilder()

        SøknadConsumer(builder, Topics.søknadTopic, sakskompleksService)
        InntektsmeldingConsumer(builder, Topics.inntektsmeldingTopic, sakskompleksService)

        return KafkaStreams(builder.build(), streamsConfig()).apply {
            addShutdownHook(this)

            environment.monitor.subscribe(ApplicationStarted) {
                start()
            }

            environment.monitor.subscribe(ApplicationStopping) {
                close(Duration.ofSeconds(10))
            }
        }

        val props = Properties()
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
        val testDriver = TopologyTestDriver(topology, props)*/
    }
}
