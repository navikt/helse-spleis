package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import kotlin.collections.flatMap

internal class V303KopiereMaksdatoFraUtbetalingTilBehandling: JsonMigration(version = 303) {
    override val description = "lagrer maksdatoresultat på behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fødselsdato = LocalDate.parse(jsonNode.path("fødselsdato").asText())
        val syttiårsdagen: LocalDate = fødselsdato.plusYears(70)
        val maksdatoSyttiåring = syttiårsdagen.forrigeVirkedagFør()

        val infotrygdbetalteDager = jsonNode.path("infotrygdhistorikk").map { element ->
            val tidligsteTidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
            val arbeidsgiverutbetalinger = element.path("arbeidsgiverutbetalingsperioder").map { periode ->
                LocalDate.parse(periode.path("fom").asText())..LocalDate.parse(periode.path("tom").asText())
            }
            val personutbetalinger = element.path("arbeidsgiverutbetalingsperioder").map { periode ->
                LocalDate.parse(periode.path("fom").asText())..LocalDate.parse(periode.path("tom").asText())
            }
            Infotrygdutbetalinger(
                tidspunkt = tidligsteTidspunkt,
                betalteDager = (arbeidsgiverutbetalinger + personutbetalinger)
            )
        }
        val utbetalingerPerArbeidsgiver = jsonNode.path("arbeidsgivere").associate { arbeidsgiver ->
            val annullerteUtbetalinger = arbeidsgiver.path("utbetalinger")
                .filter { it.path("type").asText() == "ANNULLERING" }
                .map { it.path("korrelasjonsId").asText() }

            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .filter { it.path("type").asText() in setOf("UTBETALING", "REVURDERING") }
                .filterNot { it.path("korrelasjonsId").asText() in annullerteUtbetalinger }
                .filter {
                    it.path("status").asText() in setOf(
                        "UTBETALT",
                        "GODKJENT_UTEN_UTBETALING",
                        "IKKE_UTBETALT",
                        "GODKJENT",
                        "OVERFØRT"
                    )
                }
                .map {
                    val forbrukteDager = it.path("utbetalingstidslinje")
                        .path("dager")
                        .filter { it.path("type").asText() == "NavDag" }
                        .map { dag -> dag.somPeriode() }
                    val avslåtteDager = it.path("utbetalingstidslinje")
                        .path("dager")
                        .filter {
                            it.path("type").asText() == "AvvistDag" && it.path("begrunnelser")
                                .any { it.asText() in maksdatobegrunnelser }
                        }
                        .map { dag ->
                            val årsaker = dag.path("begrunnelser").map { it.asText() }
                            val maksdatobegrunnelse = Maksdatobegrunnelse.entries.first { it.name in årsaker }
                            val periode = dag.somPeriode()
                            maksdatobegrunnelse to periode
                        }
                        .groupBy({ it.first }) { it.second }

                    Utbetalingmetadata(
                        utbetalingId = UUID.fromString(it.path("id").asText()),
                        korrelasjonsId = UUID.fromString(it.path("korrelasjonsId").asText()),
                        beregningstidspunkt = LocalDateTime.parse(it.path("tidsstempel").asText()),
                        utbetalingTom = LocalDate.parse(it.path("tom").asText()),
                        maksdato = LocalDate.parse(it.path("maksdato").asText()),
                        antallForbrukteDager = it.path("forbrukteSykedager").asInt(),
                        antallGjenståendeDager = it.path("gjenståendeSykedager").asInt(),
                        forbrukteDager = forbrukteDager,
                        oppholdsdager = emptySet(),
                        avslåtteDager = avslåtteDager
                    )
                }
            arbeidsgiver.path("organisasjonsnummer").asText() to utbetalinger
        }

        val maksdatoresultater = utbetalingerPerArbeidsgiver.flatMap { (arbeidsgiver, utbetalinger) ->
            utbetalinger.map { utbetaling ->
                // kanskje er det dager utbetalt i Infotrygd?
                // eller på andre arbeidsgivere?
                // Vi må danne oss et totalbilde av alt som er forbrukt før utbetalingen ble beregnet
                val sisteInfotrygdelementFørBeregning = infotrygdbetalteDager
                    .firstOrNull { it.tidspunkt < utbetaling.beregningstidspunkt }
                    ?.betalteDager
                    ?.flatMap { it.datoer() }
                    ?.toSet()
                    ?: emptySet()

                val forbrukteDagerAndreArbeidsgivere = utbetalingerPerArbeidsgiver
                    .flatMap { (_, andreUtbetalinger) ->
                        andreUtbetalinger
                            .filterNot { it.korrelasjonsId == utbetaling.korrelasjonsId }
                            .groupBy { it.korrelasjonsId }
                            .flatMap { (_, utbetalinger) ->
                                utbetalinger
                                    .filter { it.beregningstidspunkt < utbetaling.beregningstidspunkt }
                                    .sortedByDescending { it.beregningstidspunkt }
                                    .takeLast(1)
                                    .flatMap { it.forbrukteDager.flatMap { it.datoer() } }
                            }
                    }
                    .toSet()

                val forbrukteDager = utbetaling.forbrukteDager.flatMap { it.datoer() }.toSet()
                val totalbildeForbrukteDager = sisteInfotrygdelementFørBeregning + forbrukteDagerAndreArbeidsgivere + forbrukteDager

                val faktiskForbrukte = totalbildeForbrukteDager
                    .filter { it <= utbetaling.utbetalingTom }
                    .sortedDescending()
                    .take(utbetaling.antallForbrukteDager)

                val begrunnelerForUtbetalingen = utbetaling.avslåtteDager.keys
                val bestemmelse = when {
                    // bruker først evt. avslagsbegrunnelser som utgangspunkt for å bestemme rettighetsbestemmelsen
                    Maksdatobegrunnelse.Over70 in begrunnelerForUtbetalingen -> Maksdatobestemmelse.SYTTI_ÅR
                    Maksdatobegrunnelse.SykepengedagerOppbruktOver67 in begrunnelerForUtbetalingen -> Maksdatobestemmelse.BEGRENSET_RETT
                    Maksdatobegrunnelse.SykepengedagerOppbrukt in begrunnelerForUtbetalingen -> Maksdatobestemmelse.ORDINÆR_RETT
                    // hvis maksdato er syttiårsdagen, da er det avgjort!
                    utbetaling.maksdato == maksdatoSyttiåring -> Maksdatobestemmelse.SYTTI_ÅR
                    // hvis bruker ikke har gått til maks, men totalt antall sykepengedager er under 248, da må det være snakk om begrenset
                    (utbetaling.antallGjenståendeDager + utbetaling.antallForbrukteDager) < 248 -> Maksdatobestemmelse.BEGRENSET_RETT
                    else -> Maksdatobestemmelse.ORDINÆR_RETT
                }

                utbetaling.utbetalingId to Maksdatoresultat(
                    vurdertTilOgMed = utbetaling.utbetalingTom,
                    bestemmelse = bestemmelse,
                    startdatoTreårsvindu = forbrukteDager.firstOrNull()?.minusYears(3) ?: LocalDate.MIN,
                    startdatoSykepengerettighet = forbrukteDager.firstOrNull(),
                    antallForbrukteDager = utbetaling.antallForbrukteDager,
                    forbrukteDager = faktiskForbrukte.merge().map { Datoperiode(it.start, it.endInclusive) },
                    oppholdsdager = emptyList(),
                    avslåtteDager = utbetaling.avslåtteDager
                        .flatMap { (_, dager) -> dager.flatMap { it.datoer() } }
                        .merge()
                        .map { Datoperiode(it.start, it.endInclusive) },
                    maksdato = utbetaling.maksdato,
                    gjenståendeDager = utbetaling.antallGjenståendeDager
                )
            }
        }.toMap()


        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(aktørId, maksdatoresultater, periode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(aktørId, maksdatoresultater, forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(aktørId: String, maksdatoresultater: Map<UUID, Maksdatoresultat>, vedtaksperiode: JsonNode) {
        val vedtaksperiodeId = vedtaksperiode.path("id").asText()
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                migrerEndring(aktørId, vedtaksperiodeId, maksdatoresultater, endring as ObjectNode)
            }
        }
    }

    private fun migrerEndring(aktørId: String, vedtaksperiodeId: String, maksdatoresultater: Map<UUID, Maksdatoresultat>, endring: ObjectNode) {
        if (!endring.hasNonNull("utbetalingId")) return
        if (endring.path("maksdatoresultat").path("bestemmelse").asText() != "IKKE_VURDERT") return

        val utbetalingId = UUID.fromString(endring.path("utbetalingId").asText())
        val resultat = maksdatoresultater[utbetalingId] ?:
            return sikkerlogg.info("[V303] Fant ikke maksdatoresultat for utbetaling. KorrelasjonsIDen har muligens blitt annullert", kv("aktørId", aktørId), kv("vedtaksperiodeId", vedtaksperiodeId))

        /* vurdering av kvaliteten */
        if (resultat.antallForbrukteDager > 0 && resultat.forbrukteDager.isEmpty())
            return sikkerlogg.info("[V303] Fant ingen forbrukte dager, men det skulle vært ${resultat.antallForbrukteDager}", kv("aktørId", aktørId), kv("vedtaksperiodeId", vedtaksperiodeId))

        val vurdertTilOgMed = endring.path("tom").asText()

        /* sette inn data */
        (endring.path("maksdatoresultat") as ObjectNode).apply {
            put("vurdertTilOgMed", vurdertTilOgMed)
            put("bestemmelse", "${resultat.bestemmelse}")
            put("startdatoTreårsvindu", "${resultat.startdatoTreårsvindu}")
            if (resultat.startdatoSykepengerettighet == null) putNull("startdatoSykepengerettighet")
            else put("startdatoSykepengerettighet", "${resultat.startdatoSykepengerettighet}")
            put("maksdato", "${resultat.maksdato}")
            put("gjenståendeDager", resultat.gjenståendeDager)
            (path("forbrukteDager") as ArrayNode).apply {
                resultat.forbrukteDager.forEach { perioden ->
                    addObject().also { nyPeriode ->
                        nyPeriode.put("fom", perioden.fom.toString())
                        nyPeriode.put("tom", perioden.tom.toString())
                    }
                }
            }
            (path("avslåtteDager") as ArrayNode).apply {
                resultat.avslåtteDager.forEach { perioden ->
                    addObject().also { nyPeriode ->
                        nyPeriode.put("fom", perioden.fom.toString())
                        nyPeriode.put("tom", perioden.tom.toString())
                    }
                }
            }
        }
    }

    private enum class Maksdatobegrunnelse {
        SykepengedagerOppbrukt, SykepengedagerOppbruktOver67, Over70, NyVilkårsprøvingNødvendig
    }

    private data class Utbetalingmetadata(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val beregningstidspunkt: LocalDateTime,
        val utbetalingTom: LocalDate,
        val maksdato: LocalDate,
        val antallForbrukteDager: Int,
        val antallGjenståendeDager: Int,
        /* disse dagene må vi søke oss frem til  */
        val oppholdsdager: Set<LocalDate>,
        val forbrukteDager: List<ClosedRange<LocalDate>>,
        val avslåtteDager: Map<Maksdatobegrunnelse, List<ClosedRange<LocalDate>>>
    )

    private data class Maksdatoresultat(
        // denne migreres fra endringens TOM
        val vurdertTilOgMed: LocalDate,
        // denne krever en egen reverse engineering-algoritme
        val bestemmelse: Maksdatobestemmelse,
        // denne kan migreres til 3 år tilbake fra første forbrukte dag
        val startdatoTreårsvindu: LocalDate,
        // denne kan migreres til første forbrukte dag (kan være feil i _noen_ historiske tilfeller)
        val startdatoSykepengerettighet: LocalDate?,
        // alle NavDag-er på utbetalingstidslinjene fra utbetalingene (eller en Infotrygd-utbetalt-dag)
        // kun ukedager
        val antallForbrukteDager: Int,
        val forbrukteDager: List<Datoperiode>,
        // alle arbeidsdager eller fridager
        val oppholdsdager: List<Datoperiode>,
        // alle avviste dager med en maksdatobegrunnelse:
        // SykepengedagerOppbrukt, SykepengedagerOppbruktOver67, Over70, NyVilkårsprøvingNødvendig
        val avslåtteDager: List<Datoperiode>,
        val maksdato: LocalDate,
        val gjenståendeDager: Int
    )

    /*
        bestemmelsen kan først bestemmes ut fra de avslåtte dagene, hvilken begrunnelse de har.
        om det ikke er noen avslåtte dager må vi se på hvor mange dager brukeren har totalt.
        om maksdato faller på 70årsdagen er det SYTTI_ÅR
        om forbrukteDager+gjenståendeDager er under 248 så er det Begrenset rett
     */
    private enum class Maksdatobestemmelse {
        IKKE_VURDERT, ORDINÆR_RETT, BEGRENSET_RETT, SYTTI_ÅR
    }

    private data class Datoperiode(val fom: LocalDate, val tom: LocalDate)

    private data class Infotrygdutbetalinger(
        val tidspunkt: LocalDateTime,
        val betalteDager: List<ClosedRange<LocalDate>>
    )

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val maksdatobegrunnelser = Maksdatobegrunnelse.entries.map { it.name }

        private fun JsonNode.somPeriode() =
            if (this.hasNonNull("dato"))
                LocalDate.parse(this.path("dato").asText()).let { it..it }
            else
                LocalDate.parse(this.path("fom").asText())..LocalDate.parse(this.path("tom").asText())

        private fun ClosedRange<LocalDate>.datoer() =
            start.datesUntil(endInclusive.plusDays(1)).toList().toSet()

        private fun Iterable<LocalDate>.merge(): List<ClosedRange<LocalDate>> {
            val sortert = sortedBy { it }
            return sortert.fold(emptyList<ClosedRange<LocalDate>>()) { resultat, dagen ->
                val last = resultat.lastOrNull()
                when {
                    // listen er tom
                    last == null -> listOf(dagen..dagen)
                    // dagen dekkes av tidligere intervall
                    last.endInclusive == dagen -> resultat
                    // dagen utvider forrige intervall
                    last.endInclusive.plusDays(1) == dagen -> resultat.dropLast(1).plusElement(last.start..dagen)
                    // dagen er starten på et nytt intervall
                    else -> resultat.plusElement(dagen..dagen)
                }
            }
        }

        private fun LocalDate.forrigeVirkedagFør() = minusDays(when (dayOfWeek) {
            SUNDAY -> 2
            MONDAY -> 3
            else -> 1
        })
    }
}