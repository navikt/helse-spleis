package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val log = LoggerFactory.getLogger("no.nav.helse.spleis.gc.App")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

@ExperimentalTime
fun main(cliArgs: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        log.error(
            "Uncaught exception in thread ${thread.name}: {}",
            err.message,
            err
        )
    }

    val args = cliArgs.takeIf(Array<*>::isNotEmpty)?.toList() ?: System.getenv("RUNTIME_OPTS").split(" ")
    if (args.isEmpty()) return log.error("Provide a task name as CLI argument")

    when (val task = args[0].trim().lowercase()) {
        "vacuum" -> vacuumTask()
        "avstemming" -> avstemmingTask(ConsumerProducerFactory(AivenConfig.default), args.getOrNull(1)?.toIntOrNull())
        "migrate" -> migrateTask(ConsumerProducerFactory(AivenConfig.default))
        "migrate_v2" -> migrateV2Task(args[1].trim())
        "test_speiljson" -> testSpeilJsonTask(args[1].trim())
        "migrere_avviksvurderinger" -> migrereAvviksvurderinger(ConsumerProducerFactory(AivenConfig.default), args[1].trim())
        else -> log.error("Unknown task $task")
    }
}

@ExperimentalTime
private fun vacuumTask() {
    val ds = DataSourceConfiguration(DbUser.SPLEIS).dataSource()
    log.info("Commencing VACUUM FULL")
    val duration = measureTime {
        sessionOf(ds).use { session -> session.run(queryOf(("VACUUM FULL person")).asExecute) }
    }
    log.info(
        "VACUUM FULL completed after {} hour(s), {} minute(s) and {} second(s)",
        duration.toInt(DurationUnit.HOURS),
        duration.toInt(DurationUnit.MINUTES) % 60,
        duration.toInt(DurationUnit.SECONDS) % 60
    )
}

private fun migrateV2Task(arbeidId: String) {
    @Language("PostgreSQL")
    val query = """
        SELECT data FROM person WHERE fnr = ? LIMIT 1 FOR UPDATE SKIP LOCKED;
    """
    var migreringCounter = 0
    opprettOgUtførArbeid(arbeidId) { session, fnr ->
        session.transaction { txSession ->
            // låser ned person-raden slik at spleis ikke tar inn meldinger og overskriver mens denne podden holder på
            val data = txSession.run(queryOf(query, fnr).map { it.string("data") }.asSingle)
            if (data != null) {
                migreringCounter += 1
                log.info("[$migreringCounter] Utfører migrering")
                val time = measureTimeMillis {
                    val dto = SerialisertPerson(data).tilPersonDto()
                    val gjenopprettetPerson = Person.gjenopprett(MaskinellJurist(), dto)
                    val resultat = gjenopprettetPerson.dto().tilPersonData().tilSerialisertPerson()
                    check(1 == txSession.run(queryOf("UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE fnr=:ident", mapOf(
                        "skjemaversjon" to resultat.skjemaVersjon,
                        "data" to resultat.json,
                        "ident" to fnr
                    )).asUpdate))
                }
                log.info("[$migreringCounter] Utført på $time ms")
            }
        }
    }
}

private fun fåLås(session: Session, arbeidId: String): Boolean {
    // oppretter en lås som varer ut levetiden til sesjonen.
    // returnerer umiddelbart med true/false avhengig om vi fikk låsen eller ikke
    @Language("PostgreSQL")
    val query = "SELECT pg_try_advisory_lock(:arbeidId)"
    return session.run(queryOf(query, mapOf("arbeidId" to arbeidId)).map { it.boolean(1) }.asSingle)!!
}

private fun fyllArbeidstabell(session: Session, arbeidId: String) {
    @Language("PostgreSQL")
    val query = """
        INSERT INTO arbeidstabell (arbeid_id,fnr,arbeid_startet,arbeid_ferdig)
        SELECT ?, fnr, null, null from person
        ON CONFLICT (arbeid_id,fnr) DO NOTHING; 
    """
    session.run(queryOf(query, arbeidId).asExecute)
}
private fun hentArbeid(session: Session, arbeidId: String, size: Int = 500): List<Long> {
    @Language("PostgreSQL")
    val query = """
    select fnr from arbeidstabell where arbeid_startet IS NULL and arbeid_id = ? limit $size for update skip locked; 
    """
    @Language("PostgreSQL")
    val oppdater = "update arbeidstabell set arbeid_startet=now() where arbeid_id=? and fnr IN(%s)"
    return session.transaction { txSession ->
        txSession.run(queryOf(query, arbeidId).map { it.long("fnr") }.asList).also { personer ->
            if (personer.isNotEmpty()) {
                txSession.run(queryOf(String.format(oppdater, personer.joinToString { "?" }), arbeidId, *personer.toTypedArray()).asUpdate)
            }
        }
    }
}
private fun arbeidFullført(session: Session, arbeidId: String, fnr: Long) {
    @Language("PostgreSQL")
    val query = "update arbeidstabell set arbeid_ferdig=now() where arbeid_id=? and fnr=?"
    session.run(queryOf(query, arbeidId, fnr).asUpdate)
}
private fun arbeidFinnes(session: Session, arbeidId: String): Boolean {
    @Language("PostgreSQL")
    val query = "SELECT COUNT(1) as antall FROM arbeidstabell where arbeid_id=?"
    val antall = session.run(queryOf(query, arbeidId).map { it.long("antall") }.asSingle) ?: 0
    return antall > 0
}

private fun klargjørEllerVentPåTilgjengeligArbeid(session: Session, arbeidId: String) {
    if (fåLås(session, arbeidId)) {
        if (arbeidFinnes(session, arbeidId)) return
        return fyllArbeidstabell(session, arbeidId)
    }

    log.info("Venter på at arbeid skal bli tilgjengelig")
    while (!arbeidFinnes(session, arbeidId)) {
        log.info("Arbeid finnes ikke ennå, venter litt")
        runBlocking { delay(250) }
    }
}

private fun opprettOgUtførArbeid(arbeidId: String, size: Int = 1, arbeider: (session: Session, fnr: Long) -> Unit) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource(maximumPoolSize = 1).use { ds ->
        sessionOf(ds).use { session ->
            klargjørEllerVentPåTilgjengeligArbeid(session, arbeidId)
            do {
                log.info("Forsøker å hente arbeid")
                val arbeidsliste = hentArbeid(session, arbeidId, size)
                    .also {
                        if (it.isNotEmpty()) log.info("Fikk ${it.size} stk")
                    }
                    .onEach { fnr ->
                        try {
                            arbeider(session, fnr)
                        } finally {
                            arbeidFullført(session, arbeidId, fnr)
                        }
                    }
            } while (arbeidsliste.isNotEmpty())
            log.info("Fant ikke noe arbeid, avslutter")
        }
    }
}

private fun testSpeilJsonTask(arbeidId: String) {
    /*opprettOgUtførArbeid(arbeidId) { session, fnr ->
        hentPerson(session, fnr)?.let { (aktørId, data) ->
            try {
                val dto = SerialisertPerson(data).tilPersonDto()
                val person = Person.gjenopprett(MaskinellJurist(), dto)
                val pølsepakke: SpekematDTO.PølsepakkeDTO = TODO("Må hente pølsepakken fra spekemat")
                serializePersonForSpeil(person, pølsepakke)
            } catch (err: Exception) {
                log.info("$aktørId lar seg ikke serialisere: ${err.message}")
            }
        }
    }*/
}

internal val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun migrereAvviksvurderinger(factory: ConsumerProducerFactory, arbeidId: String) {
    factory.createProducer().use { producer ->
        opprettOgUtførArbeid(arbeidId, size = 500) { session, fnr ->
            hentPerson(session, fnr)?.let { (aktørId, data) ->
                val mdcContextMap = MDC.getCopyOfContextMap() ?: emptyMap()
                try {
                    MDC.put("aktørId", aktørId)
                    val node = objectMapper.readTree(data)
                    val vurderinger = hentAvviksvurderinger(node)
                    if (vurderinger.isEmpty()) return@let
                    val fødselsnummer = fødselsnummerSomString(fnr)
                    val event = AvviksvurderingerEvent(fødselsnummer, vurderinger)
                    sikkerlogg.info("Ønsker å skrive avviksvurdering til kafka:\n {}", objectMapper.writeValueAsString(event))
                    producer.send(
                        ProducerRecord(
                            "tbd.avviksvurdering.migreringer.v2",
                            null,
                            fødselsnummer,
                            objectMapper.writeValueAsString(event)
                        )
                    )
                    sikkerlogg.info("Skrev avviksvurderinger til avviksvurdering.migreringer")

                } catch (err: Exception) {
                    log.info("$aktørId lar seg ikke serialisere: ${err.message}")
                    sikkerlogg.error("$aktørId lar seg ikke serialisere: ${err.message}", err)
                } finally {
                    MDC.setContextMap(mdcContextMap)
                }
            }
        }
        producer.flush()
    }
    runBlocking {
        log.info("Venter med å skru av i ett minutt for at securelogs-sidecar forhåpentligvis skal synce loggene")
        delay(60000L)
    }
}

private fun fødselsnummerSomString(fnr: Long) = fnr.toString().let { if (it.length == 11) it else "0$it" }

private fun hentAvviksvurderinger(node: JsonNode): List<AvviksvurderingDto> {
    return node.path("vilkårsgrunnlagHistorikk")
        .flatMap { innslag ->
            val opprettet = LocalDateTime.parse(innslag.path("opprettet").asText())
            innslag.path("vilkårsgrunnlag")
                .mapNotNull { vilkårsgrunnlag ->
                    val type = vilkårsgrunnlag.path("type").asText()
                    when (type) {
                        "Vilkårsprøving" -> parseSpleisVilkårsgrunnlag(node, vilkårsgrunnlag, opprettet)
                        "Infotrygd" -> parseInfotrygdVilkårsgrunnlag(vilkårsgrunnlag, opprettet)
                        else -> null
                    }
                }
        }
        .groupBy { it.vilkårsgrunnlagId }
        .map { (_, values) -> values.minByOrNull { it.vurderingstidspunkt }!! }
}

private val DA_SPINNVILL_TOK_OVER = LocalDateTime.of(2024, 1, 2, 10, 0, 0, 0)
private fun parseSpleisVilkårsgrunnlag(node: JsonNode, grunnlagsdata: JsonNode, opprettet: LocalDateTime): AvviksvurderingDto? {
    val sammenligningsgrunnlagTotalbeløp = grunnlagsdata.path("sykepengegrunnlag").path("sammenligningsgrunnlag").path("sammenligningsgrunnlag").asDouble()
    if (opprettet >= DA_SPINNVILL_TOK_OVER && sammenligningsgrunnlagTotalbeløp == 0.0) return null
    val skjæringstidspunkt = LocalDate.parse(grunnlagsdata.path("skjæringstidspunkt").asText())
    return try {
        val vilkårsgrunnlagId = UUID.fromString(grunnlagsdata.path("vilkårsgrunnlagId").asText())
        val beregningsgrunnlagTotalbeløp = grunnlagsdata.path("sykepengegrunnlag").path("totalOmregnetÅrsinntekt").asDouble()
        val avviksprosentSomBrøk = when (sammenligningsgrunnlagTotalbeløp) {
            0.0 -> 1.0
            else -> (sammenligningsgrunnlagTotalbeløp - beregningsgrunnlagTotalbeløp).absoluteValue / sammenligningsgrunnlagTotalbeløp
        }
        AvviksvurderingDto(
            beregningsgrunnlagTotalbeløp = beregningsgrunnlagTotalbeløp,
            sammenligningsgrunnlagTotalbeløp = sammenligningsgrunnlagTotalbeløp,
            avviksprosent = (avviksprosentSomBrøk * 10_000).roundToInt() / 100.0,
            skjæringstidspunkt = skjæringstidspunkt,
            vurderingstidspunkt = opprettet,
            type = VilkårsgrunnlagtypeDto.SPLEIS,
            omregnedeÅrsinntekter = grunnlagsdata.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").map { opplysning ->
                OmregnetÅrsinntektDto(
                    orgnummer = opplysning.path("orgnummer").asText(),
                    beløp = omregnetÅrsinntekt(node, opplysning.path("inntektsopplysning"))
                )
            },
            sammenligningsgrunnlag = grunnlagsdata.path("sykepengegrunnlag").path("sammenligningsgrunnlag").path("arbeidsgiverInntektsopplysninger").map { opplysning ->
                SammenligningsgrunnlagDto(
                    orgnummer = opplysning.path("orgnummer").asText(),
                    skatteopplysninger = opplysning.path("skatteopplysninger").map { skatt ->
                        SkatteopplysningDto(
                            beløp = skatt.path("beløp").asDouble(),
                            måned = YearMonth.parse(skatt.path("måned").asText()),
                            type = skatt.path("type").asText(),
                            fordel = skatt.path("fordel").asText(),
                            beskrivelse = skatt.path("beskrivelse").asText()
                        )
                    }
                )
            },
            vilkårsgrunnlagId = vilkårsgrunnlagId
        )
    } catch (err: Exception) {
        sikkerlogg.error("Klarte ikke migrere skjæringstidspunkt $skjæringstidspunkt: ${err.message}", err)
        null
    }
}
private fun omregnetÅrsinntekt(node: JsonNode, opplysning: JsonNode): Double {
    if (opplysning.path("kilde").asText() == "SKJØNNSMESSIG_FASTSATT") return omregnetÅrsinntekt(node, finnInntektsopplysning(node, opplysning.path("overstyrtInntektId").asText()))
    val månedsbeløp = opplysning.path("skatteopplysninger")
        .takeIf(JsonNode::isArray)
        ?.sumOf { skatt -> skatt.path("beløp").asDouble() }
        ?.coerceAtLeast(0.0)
        ?.div(3)
        ?: opplysning.path("beløp").asDouble()
    return månedsbeløp * 12
}
private fun finnInntektsopplysning(node: JsonNode, opplysningId: String): JsonNode {
    return node.path("vilkårsgrunnlagHistorikk").firstNotNullOfOrNull { vilkårsgrunnlag ->
        vilkårsgrunnlag.path("vilkårsgrunnlag").firstNotNullOfOrNull { grunnlagsdata ->
            grunnlagsdata.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").firstOrNull { opplysning ->
                opplysning.path("inntektsopplysning").path("id").asText() == opplysningId
            }
        }
    } ?: throw IllegalStateException("Finner ikke vilkårsgrunnlag med inntektsopplysning med $opplysningId")
}
private fun parseInfotrygdVilkårsgrunnlag(grunnlagsdata: JsonNode, opprettet: LocalDateTime): AvviksvurderingDto {
    return AvviksvurderingDto(
        beregningsgrunnlagTotalbeløp = null,
        sammenligningsgrunnlagTotalbeløp = null,
        avviksprosent = null,
        skjæringstidspunkt = LocalDate.parse(grunnlagsdata.path("skjæringstidspunkt").asText()),
        vurderingstidspunkt = opprettet,
        type = VilkårsgrunnlagtypeDto.INFOTRYGD,
        omregnedeÅrsinntekter = emptyList(),
        sammenligningsgrunnlag = emptyList(),
        vilkårsgrunnlagId = UUID.fromString(grunnlagsdata.path("vilkårsgrunnlagId").asText())
    )
}

private data class AvviksvurderingerEvent(
    val fødselsnummer: String,
    val skjæringstidspunkter: List<AvviksvurderingDto>,
    @JsonProperty("@event_name")
    val eventName: String = "avviksvurderinger"
)
private data class AvviksvurderingDto(
    val beregningsgrunnlagTotalbeløp: Double?,
    val sammenligningsgrunnlagTotalbeløp: Double?,
    val avviksprosent: Double?,
    val skjæringstidspunkt: LocalDate,
    val vurderingstidspunkt: LocalDateTime,
    val type: VilkårsgrunnlagtypeDto,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntektDto>,
    val sammenligningsgrunnlag: List<SammenligningsgrunnlagDto>,
    val vilkårsgrunnlagId: UUID
) {
    init {
        if (type == VilkårsgrunnlagtypeDto.SPLEIS && omregnedeÅrsinntekter.isEmpty()) sikkerlogg.error("Ingen omregnede årsinntekter for $skjæringstidspunkt")
        if (type == VilkårsgrunnlagtypeDto.SPLEIS && sammenligningsgrunnlag.isEmpty()) sikkerlogg.error("Ingen sammenligningsgrunnlag for $skjæringstidspunkt")
        if (type == VilkårsgrunnlagtypeDto.SPLEIS && sammenligningsgrunnlagTotalbeløp == 0.0) sikkerlogg.error("sammenligningsgrunnlagTotalbeløp er 0 for $skjæringstidspunkt")
    }
}

private class OmregnetÅrsinntektDto(
    val orgnummer: String,
    val beløp: Double
)

private class SammenligningsgrunnlagDto(
    val orgnummer: String,
    val skatteopplysninger: List<SkatteopplysningDto>
)

private class SkatteopplysningDto(
    val beløp: Double,
    val måned: YearMonth,
    val type: String,
    val fordel: String,
    val beskrivelse: String
)

private enum class VilkårsgrunnlagtypeDto {
    INFOTRYGD, SPLEIS
}

private fun hentPerson(session: Session, fnr: Long) =
    session.run(queryOf("SELECT aktor_id, data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fnr).map {
        it.string("aktor_id") to it.string("data")
    }.asSingle)

private fun migrateTask(factory: ConsumerProducerFactory) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        var count = 0L
        factory.createProducer().use { producer ->
            sessionOf(ds).use { session ->
                session.run(queryOf("SELECT fnr,aktor_id FROM person").map { row ->
                    val fnr = row.string("fnr").padStart(11, '0')
                    fnr to row.string("aktor_id")
                }.asList)
            }.forEach { (fnr, aktørId) ->
                count += 1
                producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagMigrate(fnr, aktørId)))
            }
            producer.flush()
        }
        println()
        println("==============================")
        println("Sendte ut $count migreringer")
        println("==============================")
        println()
    }

}

@ExperimentalTime
private fun avstemmingTask(factory: ConsumerProducerFactory, customDayOfMonth: Int? = null) {
    // Håndter on-prem og gcp database tilkobling forskjellig
    val ds = DataSourceConfiguration(DbUser.AVSTEMMING).dataSource()
    val dayOfMonth = customDayOfMonth ?: LocalDate.now().dayOfMonth
    log.info("Commencing avstemming for dayOfMonth=$dayOfMonth")
    val producer = factory.createProducer()
    // spørringen funker slik at den putter alle personer opp i 28 ulike "bøtter" (fnr % 28 gir tall mellom 0 og 27, også plusser vi på én slik at vi starter fom. 1)
    // hvor én bøtte tilsvarer én dag i måneden. Tallet 28 (pga februar) ble valgt slik at vi sikrer oss at vi avstemmer
    // alle personer hver måned. Dag 29, 30, 31 avstemmes 0 personer siden det er umulig å ha disse rest-verdiene

    sessionOf(ds).use { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT fnr, aktor_id
            FROM person
            WHERE (1 + mod(fnr, 28)) = :dayOfMonth AND (sist_avstemt is null or sist_avstemt < now() - interval '1 day')
            """
        session.run(queryOf(statement, mapOf("dayOfMonth" to dayOfMonth)).map { row ->
            row.string("aktor_id") to row.string("fnr")
        }.asList).forEach { (aktørId, fnr) ->
            val fnrStr = fnr.padStart(11, '0')
            producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagAvstemming(fnrStr, aktørId)))
        }
    }

    producer.flush()
    log.info("Avstemming completed")
}

private fun lagMigrate(fnr: String, aktørId: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "json_migrate",
  "@opprettet": "${LocalDateTime.now()}",
  "aktørId": "$aktørId",
  "fødselsnummer": "$fnr"
}
"""

private fun lagAvstemming(fnr: String, aktørId: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "person_avstemming",
  "@opprettet": "${LocalDateTime.now()}",
  "aktørId": "$aktørId",
  "fødselsnummer": "$fnr"
}
"""

private class DataSourceConfiguration(dbUsername: DbUser) {
    private val env = System.getenv()

    private val gcpProjectId = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "gcp project id must be set" }
    private val databaseRegion = requireNotNull(env["DATABASE_REGION"]) { "database region must be set" }
    private val databaseInstance = requireNotNull(env["DATABASE_INSTANCE"]) { "database instance must be set" }
    private val databaseUsername = requireNotNull(env["${dbUsername}_USERNAME"]) { "database username must be set" }
    private val databasePassword = requireNotNull(env["${dbUsername}_PASSWORD"]) { "database password must be set"}
    private val databaseName = requireNotNull(env["${dbUsername}_DATABASE"]) { "database name must be set"}

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        username = databaseUsername
        password = databasePassword

        maximumPoolSize = 2
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    fun dataSource(maximumPoolSize: Int = 2) = HikariDataSource(hikariConfig.apply {
        this.maximumPoolSize = maximumPoolSize
    })
}

private enum class DbUser(private val dbUserPrefix: String) {
    SPLEIS("DATABASE"), AVSTEMMING("DATABASE_SPLEIS_AVSTEMMING"), MIGRATE("DATABASE_SPLEIS_MIGRATE");

    override fun toString() = dbUserPrefix
}
