package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.migration.V275GenerasjonMedEndringer.Dokumentsporing.Companion.dokumenter
import no.nav.helse.serde.migration.V275GenerasjonMedEndringer.Dokumentsporing.Companion.dokumentsporing
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V275GenerasjonMedEndringer: JsonMigration(275) {
    override val description = "dry run av migrering for å legge til <endringer> på generasjoner"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        try {
            migrer(jsonNode.deepCopy())
        } catch (err: Exception) {
            sikkerlogg.info("[V275] Ville ha trynet med feil: $err", err)
        }
    }

    private fun migrer(jsonNode: ObjectNode) {
        val tidligsteTidspunktForHendelse = mutableMapOf<UUID, LocalDateTime>()

        fun oppdaterTidspunkt(hendelse: UUID, tidspunkt: LocalDateTime) {
            tidligsteTidspunktForHendelse.compute(hendelse) { _, gammelVerdi ->
                gammelVerdi?.let { minOf(tidspunkt, gammelVerdi) } ?: tidspunkt
            }
        }

        jsonNode.path("vilkårsgrunnlagHistorikk")
            .reversed()
            .forEach { element ->
                val elementOpprettet = LocalDateTime.parse(element.path("opprettet").asText())
                element.path("vilkårsgrunnlag").forEach { grunnlag ->
                    grunnlag.path("meldingsreferanseId").takeIf(JsonNode::isTextual)?.also { hendelseId ->
                        oppdaterTidspunkt(hendelseId.asText().uuid, elementOpprettet)
                    }

                    grunnlag.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                        opplysning.path("inntektsopplysning").also { inntektopplysning ->
                            val id = inntektopplysning.path("hendelseId").asText().uuid
                            val tidspunkt = LocalDateTime.parse(inntektopplysning.path("tidsstempel").asText())
                            oppdaterTidspunkt(id, tidspunkt)
                        }
                        opplysning.path("refusjonsopplysninger").forEach { refusjonsopplysning ->
                            val id = refusjonsopplysning.path("meldingsreferanseId").asText().uuid
                            oppdaterTidspunkt(id, elementOpprettet)
                        }
                    }
                }
            }

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            fun sykdomstidslinjesubsetting(hendelseId: UUID, periodeFom: LocalDate, periodeTom: LocalDate): ObjectNode? {
                return arbeidsgiver
                    .path("sykdomshistorikk")
                    .firstOrNull { element ->
                        val node = element.path("hendelseId")
                        node.isTextual && node.asText().uuid == hendelseId
                    }
                    ?.path("beregnetSykdomstidslinje")
                    ?.let { beregnetTidslinje ->
                        val dager = beregnetTidslinje
                            .path("dager")
                            .filter { dagen ->
                                val periodeForDagen = dagen.periode
                                maxOf(periodeFom, periodeForDagen.start) <= minOf(periodeTom, periodeForDagen.endInclusive)
                            }
                            .map { it.deepCopy<ObjectNode>() }
                            .onEach { dagen ->
                                val periodeForDagen = dagen.periode
                                if (periodeForDagen.start < periodeFom) dagen.put("fom", "$periodeFom")
                                if (periodeForDagen.endInclusive > periodeTom) dagen.put("tom", "$periodeTom")
                            }
                        serdeObjectMapper
                            .createObjectNode()
                            .also {
                                it.putArray("låstePerioder")
                            }
                            .also {
                                it.putObject("periode")
                                    .put("fom", "$periodeFom")
                                    .put("tom", "$periodeTom")
                            }
                            .also {
                                it.putArray("dager").addAll(dager)
                            }
                    }
            }
            arbeidsgiver.path("sykdomshistorikk").reversed().forEach { element ->
                val node = element.path("hendelseId")
                if (node.isTextual) {
                    val hendelseId = node.asText().uuid
                    val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                    oppdaterTidspunkt(hendelseId, tidspunkt)
                }
                element.path("beregnetSykdomstidslinje").also { tidslinje ->
                    tidslinje.path("dager").forEach { dag ->
                        val id = dag.path("kilde").path("id").asText().uuid
                        val tidsstempel = LocalDateTime.parse(dag.path("kilde").path("tidsstempel").asText())
                        // oppdaterer tidspunkt for hendelsen bare dersom vi ikke har hørt om den før;
                        // søknader har et tidspunkt satt til midnatt som er pga det er verdien av 'sykmeldingSkrevet'.
                        // det er åpenbart ikke riktig tidspunkt for _når_ hendelsen ble håndtert i spleis
                        if (id !in tidligsteTidspunktForHendelse) oppdaterTidspunkt(id, tidsstempel)
                    }
                }
            }
            arbeidsgiver.path("refusjonshistorikk").forEach { element ->
                val hendelseId = element.path("meldingsreferanseId").asText().uuid
                val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                oppdaterTidspunkt(hendelseId, tidspunkt)
            }
            arbeidsgiver.path("inntektshistorikk").forEach { element ->
                val hendelseId = element.path("hendelseId").asText().uuid
                val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                oppdaterTidspunkt(hendelseId, tidspunkt)
            }

            val tidspunktForUtbetalingOpprettelse = arbeidsgiver.path("utbetalinger").associate { utbetaling ->
                utbetaling.path("id").asText().uuid to LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
            }
            val tidspunktForUtbetalingOppdatering = arbeidsgiver.path("utbetalinger").associate { utbetaling ->
                utbetaling.path("id").asText().uuid to LocalDateTime.parse(utbetaling.path("oppdatert").asText())
            }
            val tidspunktForUtbetalingVurdert = arbeidsgiver.path("utbetalinger")
                .filter { utbetaling ->
                    utbetaling.path("vurdering").hasNonNull("tidspunkt")
                }
                .associate { utbetaling ->
                    utbetaling.path("id").asText().uuid to LocalDateTime.parse(utbetaling.path("vurdering").path("tidspunkt").asText())
                }
            val tidspunktForUtbetalingAvsluttet = arbeidsgiver.path("utbetalinger")
                .filter { utbetaling ->
                    utbetaling.hasNonNull("avsluttet")
                }
                .associate { utbetaling ->
                    utbetaling.path("id").asText().uuid to LocalDateTime.parse(utbetaling.path("avsluttet").asText())
                }
            val utbetalingtype = arbeidsgiver.path("utbetalinger")
                .associate { utbetaling ->
                    utbetaling.path("id").asText().uuid to utbetaling.path("type").asText()
                }
            val utbetalingstatus = arbeidsgiver.path("utbetalinger")
                .associate { utbetaling ->
                    utbetaling.path("id").asText().uuid to utbetaling.path("status").asText()
                }

            arbeidsgiver.path("vedtaksperioder")
                .onEach { periode -> migrerVedtaksperiode(periode, tidligsteTidspunktForHendelse::get, tidspunktForUtbetalingOpprettelse::get, tidspunktForUtbetalingVurdert::get, tidspunktForUtbetalingAvsluttet::get, utbetalingstatus::getValue, ::sykdomstidslinjesubsetting, utbetalingtype::getValue, tidspunktForUtbetalingOppdatering::getValue) }
                .onEach { periode -> bekreftAktivVedtaksperiode(periode) }
            arbeidsgiver.path("forkastede")
                .onEach { periode -> migrerForkastetVedtaksperiode(periode.path("vedtaksperiode"), tidligsteTidspunktForHendelse::get, tidspunktForUtbetalingOpprettelse::get, tidspunktForUtbetalingVurdert::get, tidspunktForUtbetalingAvsluttet::get, utbetalingstatus::getValue, ::sykdomstidslinjesubsetting, utbetalingtype::getValue, tidspunktForUtbetalingOppdatering::getValue) }
                .onEach { periode -> bekreftForkastetPeriode(periode.path("vedtaksperiode")) }
        }
    }

    private fun bekreftAktivVedtaksperiode(periode: JsonNode) {
        bekreftPeriode(periode)
        check(periode.path("tilstand").asText() != "TIL_INFOTRYGD") {
            "En aktiv vedtaksperiode står i TIL_INFOTRYGD etter migrering"
        }
        check(periode.path("generasjoner").none { generasjon ->
            generasjon.path("tilstand").asText() == "TIL_INFOTRYGD"
        }) {
            "En aktiv generasjon står i TIL_INFOTRYGD etter migrering"
        }
    }
    private fun bekreftForkastetPeriode(periode: JsonNode) {
        bekreftPeriode(periode)
    }
    private fun bekreftPeriode(periode: JsonNode) {
        val dokumentsporing = periode.path("hendelseIder").map { it.dokumentsporing }
        check(!periode.path("generasjoner").isEmpty) {
            "En vedtaksperiode har 0 generasjoner (vedtaksperiode ${periode.path("id").asText()})"
        }
        check(periode.path("generasjoner").none { generasjon ->
            generasjon.path("endringer").isEmpty
        }) {
            "En vedtaksperiode har en generasjon med 0 endringer (vedtaksperiode ${periode.path("id").asText()})"
        }
        val dokumenterUtenEndring = dokumentsporing.filterNot { dokument ->
            periode.path("generasjoner").generasjonHarHåndtert(dokument)
        }
        check(dokumenterUtenEndring.isEmpty()) {
            "Et/flere dokument(er) har ikke en tilhørende endring, og ville forsvunnet: $dokumenterUtenEndring (vedtaksperiode ${periode.path("id").asText()})"
        }
    }

    private fun Iterable<JsonNode>.generasjonHarHåndtert(dokument: Dokumentsporing) =
        any { generasjon ->
            generasjon.path("endringer").any { endring ->
                dokument == endring.path("dokumentsporing").dokumentsporing
            }
        }
    private fun Iterable<JsonNode>.utbetalingHarHåndtert(dokument: Dokumentsporing) =
        any { utbetaling ->
            dokument == utbetaling.path("dokumentsporing").dokumentsporing
        }

    private val JsonNode.periode: ClosedRange<LocalDate> get() {
        return if (this.hasNonNull("fom")) {
            this.path("fom").asText().dato..this.path("tom").asText().dato
        } else {
            this.path("dato").asText().dato.let { it..it }
        }
    }

    private fun migrerForkastetVedtaksperiode(
        periode: JsonNode,
        tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingOpprettelse: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingVurdert: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingAvsluttet: (UUID) -> LocalDateTime?,
        utbetalingstatus: (UUID) -> String,
        sykdomstidslinjesubsetting: (UUID, LocalDate, LocalDate) -> ObjectNode?,
        utbetalingtype: (UUID) -> String,
        tidspunktForUtbetalingOppdatering: (UUID) -> LocalDateTime
    ) {
        val generasjoner = migrerVedtaksperiode(periode, tidligsteTidspunktForHendelse, tidspunktForUtbetalingOpprettelse, tidspunktForUtbetalingVurdert, tidspunktForUtbetalingAvsluttet, utbetalingstatus, sykdomstidslinjesubsetting, utbetalingtype, tidspunktForUtbetalingOppdatering)
        val siste = generasjoner.last()
        if (siste.path("tilstand").asText() == "TIL_INFOTRYGD") return

        val forkastingTidspunkt = LocalDateTime.parse(periode.path("oppdatert").asText())
        val fom = periode.path("fom").asText().dato
        val tom = periode.path("tom").asText().dato
        val sisteEndring = siste.path("endringer").last()
        val sykmeldingsperiodeFom = sisteEndring.path("sykmeldingsperiodeFom").asText().dato
        val sykmeldingsperiodeTom = sisteEndring.path("sykmeldingsperiodeTom").asText().dato
        val dokument = sisteEndring.path("dokumentsporing").dokumentsporing
        val sykdomstidslinje = sisteEndring.path("sykdomstidslinje").deepCopy<ObjectNode>()
        val endring = lagEndring(dokument.id, dokument.type, forkastingTidspunkt, sykmeldingsperiodeFom, sykmeldingsperiodeTom, sykdomstidslinje, null, null)
        val infotrygdgenerasjon = lagGenerasjon("TIL_INFOTRYGD", forkastingTidspunkt, fom, tom, listOf(endring), avsluttet = forkastingTidspunkt)
        (periode.path("generasjoner") as ArrayNode).add(infotrygdgenerasjon)
    }

    private fun migrerVedtaksperiode(
        periode: JsonNode,
        tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingOpprettelse: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingVurdert: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingAvsluttet: (UUID) -> LocalDateTime?,
        utbetalingstatus: (UUID) -> String,
        sykdomstidslinjesubsetting: (UUID, LocalDate, LocalDate) -> ObjectNode?,
        utbetalingtype: (UUID) -> String,
        tidspunktForUtbetalingOppdatering: (UUID) -> LocalDateTime
    ): List<ObjectNode> {
        val forkastedeUtbetalinger: MutableList<ObjectNode> = mutableListOf()
        val dokumentsporing = periode.path("hendelseIder").deepCopy<ArrayNode>()
        val fom = periode.path("fom").asText().dato
        val tom = periode.path("tom").asText().dato
        val sykmeldingsperiodeFom = periode.path("sykmeldingFom").asText().dato
        val sykmeldingsperiodeTom = periode.path("sykmeldingTom").asText().dato
        val opprettettidspunkt = LocalDateTime.parse(periode.path("opprettet").asText())
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val dokumenterHåndtertAvTidligereGenerasjoner = mutableListOf<Dokumentsporing>()
        val nyeGenerasjoner = generasjoner
            .mapNotNull { node ->
                migrerGenerasjon(periode, node, forkastedeUtbetalinger, dokumentsporing, tidligsteTidspunktForHendelse, sykmeldingsperiodeFom, sykmeldingsperiodeTom, opprettettidspunkt, tidspunktForUtbetalingOpprettelse, tidspunktForUtbetalingVurdert, tidspunktForUtbetalingAvsluttet, utbetalingstatus, sykdomstidslinjesubsetting, utbetalingtype, dokumenterHåndtertAvTidligereGenerasjoner).also {
                    if (it != null) {
                        forkastedeUtbetalinger.clear()
                        dokumenterHåndtertAvTidligereGenerasjoner.addAll(it.path("endringer").map { endring -> endring.path("dokumentsporing").dokumentsporing })
                    }
                }
            }

        val endringerFraForkastedeUtbetalinger = endringerFraForkastetUtbetaling(forkastedeUtbetalinger, opprettettidspunkt, sykmeldingsperiodeFom, sykmeldingsperiodeTom, tidligsteTidspunktForHendelse, tidspunktForUtbetalingOpprettelse)
        val dokumenterUtenEndring = dokumentsporing
            .dokumenter
            .filterNot { dokument -> nyeGenerasjoner.generasjonHarHåndtert(dokument) }
            .filterNot { dokument -> endringerFraForkastedeUtbetalinger.utbetalingHarHåndtert(dokument) }

        val endeligResultat = if (nyeGenerasjoner.isEmpty()) {
            listOf(migrerInitiellGenerasjon(fom, tom, sykmeldingsperiodeFom, sykmeldingsperiodeTom, opprettettidspunkt, periode, tidligsteTidspunktForHendelse, endringerFraForkastedeUtbetalinger, sykdomstidslinjesubsetting))
        } else {
            val førsteGenerasjon = nyeGenerasjoner.first()
            val auuGenerasjon = if (førsteGenerasjon.path("tilstand").asText() == "BEREGNET_OMGJØRING") {
                val auuTidspunkt = LocalDateTime.parse(periode.path("opprettet").asText())
                val førsteHendelse = dokumentsporing.dokumenter.first()
                (førsteGenerasjon.path("endringer") as ArrayNode).also {
                    check(it.size() > 1) {
                        "Forventer at den beregnede omgjøringen har flere hendelser enn 1"
                    }
                    it.removeAll { dokument ->
                        dokument.path("dokumentsporing").dokumentsporing == førsteHendelse
                    }
                }
                listOf(
                    lagGenerasjon("AVSLUTTET_UTEN_VEDTAK", auuTidspunkt, sykmeldingsperiodeFom, sykmeldingsperiodeTom, listOf(
                        oversettDokumentsporingTilEndring(førsteHendelse, opprettettidspunkt, tidligsteTidspunktForHendelse, sykmeldingsperiodeFom, sykmeldingsperiodeTom, { id -> checkNotNull(sykdomstidslinjesubsetting(id, sykmeldingsperiodeFom, sykmeldingsperiodeTom)) { "Finner ikke sykdomstidslinje for $id for vedtaksperiode ${periode.path("id").asText()}" } }, null, null, null)
                    ), null, auuTidspunkt)
                )
            } else emptyList<ObjectNode>()
            auuGenerasjon + nyeGenerasjoner + if (endringerFraForkastedeUtbetalinger.isNotEmpty()) {
                // forutsetter at alle de forkastede utbetalingene er revurderinger
                check(dokumenterUtenEndring.isEmpty()) {
                    "har ikke tatt høyde for at det kan finnes dokumenter som ikke inngår i noen utbetalinger: $dokumenterUtenEndring"
                }
                /*
                    hvis vedtaksperioden er forkastet så antar vi at vedtaksperioden har blitt annullert etter å ha beregnet revurdering.
                    hvis vedtaksperioden fremdeles er aktiv antar vi at revurderingen har blitt forkastet fordi en eldre periode har "tatt over"
                 */
                val antarAtPeriodenErUberegnetRevurdering = periode.path("tilstand").asText() in setOf("AVVENTER_REVURDERING", "AVVENTER_HISTORIKK_REVURDERING")
                val tilstand = if (antarAtPeriodenErUberegnetRevurdering) "UBEREGNET_REVURDERING" else "BEREGNET_REVURDERING"
                val endringUtenUtbetaling = if (antarAtPeriodenErUberegnetRevurdering) endringerFraForkastedeUtbetalinger.last().deepCopy().let {
                    val forkastetUtbetalingId = it.path("utbetalingId").asText().uuid
                    check("FORKASTET" == utbetalingstatus(forkastetUtbetalingId)) {
                        "Forventer at utbetalingen $forkastetUtbetalingId skal være forkastet for vedtaksperiode ${periode.path("id").asText()}"
                    }
                    it.put("tidsstempel", tidspunktForUtbetalingOppdatering(forkastetUtbetalingId).toString())
                    it.putNull("utbetalingId")
                    it.putNull("vilkårsgrunnlagId")
                } else null
                listOf(lagGenerasjon(tilstand, LocalDateTime.parse(endringerFraForkastedeUtbetalinger.first().path("tidsstempel").asText()), fom, tom, endringerFraForkastedeUtbetalinger + listOfNotNull(endringUtenUtbetaling)))
            } else {
                val sisteGenerasjon = nyeGenerasjoner.last()
                val sisteEndringISisteGenerasjon = sisteGenerasjon.path("endringer").last()

                dokumenterUtenEndring.mapNotNull { dokumentUtenEndring ->
                    val vedtaksperiodeOppdatert = LocalDateTime.parse(periode.path("oppdatert").asText())
                    // et best guess basert på tidspunktet den siste hendelsen ble håndtert i spleis, eller oppdatert-tidspunktet til vedtaksperioden
                    val tidspunktForNårPeriodenGikkTilAvventerRevurdering = tidligsteTidspunktForHendelse(dokumentUtenEndring.id) ?: vedtaksperiodeOppdatert
                    val sykdomstidslinjeForUberegnetRevurdering = sykdomstidslinjesubsetting(dokumentUtenEndring.id, fom, tom) ?: sisteEndringISisteGenerasjon.path("sykdomstidslinje").deepCopy()
                    val endring = lagEndring(dokumentUtenEndring.id, dokumentUtenEndring.type, tidspunktForNårPeriodenGikkTilAvventerRevurdering, sykmeldingsperiodeFom, sykmeldingsperiodeTom, sykdomstidslinjeForUberegnetRevurdering, null, null)

                    if (sisteGenerasjon.hasNonNull("vedtakFattet")) lagGenerasjon("UBEREGNET_REVURDERING", tidspunktForNårPeriodenGikkTilAvventerRevurdering, fom, tom, listOf(endring))
                    else null
                }
            }
        }
        generasjoner.removeAll()
        generasjoner.addAll(endeligResultat)
        check(endeligResultat.isNotEmpty()) {
            "Vedtaksperioden ${periode.path("id").asText()} endte opp uten generasjoner"
        }
        return endeligResultat
    }

    private fun migrerInitiellGenerasjon(
        fom: LocalDate,
        tom: LocalDate,
        sykmeldingsperiodeFom: LocalDate,
        sykmeldingsperiodeTom: LocalDate,
        opprettettidspunkt: LocalDateTime,
        periode: JsonNode,
        tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?,
        forkastedeUtbetalingerSomEndring: List<ObjectNode>,
        sykdomstidslinjesubsetting: (UUID, LocalDate, LocalDate) -> ObjectNode?
    ): ObjectNode {
        val sykdomstidslinje = periode.path("sykdomstidslinje").deepCopy<ObjectNode>()
        val dokumenter = periode.path("hendelseIder").dokumenter.takeUnless { it.isEmpty() }
            ?: sykdomstidslinje
                .path("dager")
                .map { dag ->
                    val type = when (val kildetype = dag.path("kilde").path("type").asText()) {
                        "Inntektsmelding" -> "InntektsmeldingDager"
                        else -> kildetype
                    }
                    Dokumentsporing(dag.path("kilde").path("id").asText().uuid, type)
                }
                .distinct()

        var forrigeDokumentId: UUID? = null
        val egneEndringer = dokumenter
            .endringerSomIkkeInngårIForkastedeUtbetalinger(forkastedeUtbetalingerSomEndring)
            .endringerSomIkkeInngårIForkastedeUtbetalinger(forkastedeUtbetalingerSomEndring)
            .map { dokument ->
                val tidspunktForHendelsen = tidligsteTidspunktForHendelse(dokument.id)
                val sykdomstidslinjeForHendelsen = sykdomstidslinjesubsetting(dokument.id, fom, tom) ?: forrigeDokumentId?.let { sykdomstidslinjesubsetting(it, fom, tom) } ?: sykdomstidslinje
                forrigeDokumentId = dokument.id
                lagEndring(dokument.id, dokument.type, tidspunktForHendelsen ?: opprettettidspunkt, sykmeldingsperiodeFom, sykmeldingsperiodeTom, sykdomstidslinjeForHendelsen, null, null)
            }
        val endringer = endringer(forkastedeUtbetalingerSomEndring, egneEndringer)
        check(endringer.isNotEmpty()) {
            "Vedtaksperioden $fom - $tom (${periode.path("id").asText()}) har ingen hendelser"
        }
        endringer.first().put("tidsstempel", "$opprettettidspunkt")
        val vedtaksperiodeOppdatert = LocalDateTime.parse(periode.path("oppdatert").asText())
        val avsluttet = when (periode.path("tilstand").asText()) {
            "AVSLUTTET_UTEN_UTBETALING" -> vedtaksperiodeOppdatert
            "TIL_INFOTRYGD" -> {
                // antar at perioden gikk til AUU kort tid etter siste hendelse mottatt
                LocalDateTime.parse(endringer.last().path("tidsstempel").asText()).plusSeconds(1).coerceAtMost(vedtaksperiodeOppdatert)
            }
            else -> null
        }
        val starttilstand = when (periode.path("tilstand").asText()) {
            "AVSLUTTET_UTEN_UTBETALING" -> "AVSLUTTET_UTEN_VEDTAK"
            /* vi må bare gjette; det kan hende at perioden var i AUU, så ble forkastet. I så fall ville riktig vært å sagt at tilstand
                skulle være "AVSLUTTET_UTEN_VEDTAK". Men for alle andre perioder som blir forkastet på direkten av andre årsaker
                så vil antagelsen være feil
             */
            "TIL_INFOTRYGD" -> "TIL_INFOTRYGD"
            else -> "UBEREGNET"
        }
        return lagGenerasjon(starttilstand, opprettettidspunkt, fom, tom, endringer, avsluttet = avsluttet)
    }

    private fun lagGenerasjon(starttilstand: String, opprettettidspunkt: LocalDateTime, periodeFom: LocalDate, periodeTom: LocalDate, endringer: List<ObjectNode>, vedtakFattet: LocalDateTime? = null, avsluttet: LocalDateTime? = null): ObjectNode {
        return serdeObjectMapper
            .createObjectNode()
            .put("id", "${UUID.randomUUID()}")
            .put("tidsstempel", "$opprettettidspunkt")
            .put("tilstand", starttilstand)
            .put("fom", "$periodeFom")
            .put("tom", "$periodeTom")
            .also {
                if (vedtakFattet == null) it.putNull("vedtakFattet")
                else it.put("vedtakFattet", "$vedtakFattet")
            }
            .also {
                if (avsluttet == null) it.putNull("avsluttet")
                else it.put("avsluttet", "$avsluttet")
            }
            .also {
                it.putArray("endringer").addAll(endringer)
            }
    }
    private fun lagEndring(
        dokumentId: UUID,
        dokumenttype: String,
        opprettettidspunkt: LocalDateTime,
        sykmeldingsperiodeFom: LocalDate,
        sykmeldingsperiodeTom: LocalDate,
        sykdomstidslinje: ObjectNode,
        forkastetUtbetalingId: UUID?,
        forkastetVilkårsgrunnlagId: UUID?
    ): ObjectNode {
        return serdeObjectMapper
            .createObjectNode()
            .put("id", "${UUID.randomUUID()}")
            .put("tidsstempel", "$opprettettidspunkt")
            .put("sykmeldingsperiodeFom", "$sykmeldingsperiodeFom")
            .put("sykmeldingsperiodeTom", "$sykmeldingsperiodeTom")
            .put("fom", sykdomstidslinje.path("periode").path("fom").asText())
            .put("tom", sykdomstidslinje.path("periode").path("tom").asText())
            .also {
                if (forkastetUtbetalingId == null) it.putNull("utbetalingId")
                else it.put("utbetalingId", "$forkastetUtbetalingId")
            }
            .also {
                if (forkastetVilkårsgrunnlagId == null) it.putNull("vilkårsgrunnlagId")
                else it.put("vilkårsgrunnlagId", "$forkastetVilkårsgrunnlagId")
            }
            .also { endringobj ->
                endringobj.set<ObjectNode>("sykdomstidslinje", sykdomstidslinje)
            }
            .also { endringobj ->
                endringobj
                    .putObject("dokumentsporing")
                    .put("dokumentId", "$dokumentId")
                    .put("dokumenttype", dokumenttype)
            }
    }

    private fun migrerGenerasjon(
        periode: JsonNode,
        generasjon: JsonNode,
        forkastedeUtbetalinger: MutableList<ObjectNode>,
        dokumentsporing: ArrayNode,
        tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?,
        sykmeldingsperiodeFom: LocalDate,
        sykmeldingsperiodeTom: LocalDate,
        opprettettidspunkt: LocalDateTime,
        tidspunktForUtbetalingOpprettelse: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingVurdert: (UUID) -> LocalDateTime?,
        tidspunktForUtbetalingAvsluttet: (UUID) -> LocalDateTime?,
        utbetalingstatus: (UUID) -> String,
        sykdomstidslinjesubsetting: (UUID, LocalDate, LocalDate) -> ObjectNode?,
        utbetalingtype: (UUID) -> String,
        dokumenterHåndtertAvTidligereGenerasjoner: MutableList<Dokumentsporing>
    ): ObjectNode? {
        generasjon as ObjectNode

        check(generasjon.hasNonNull("utbetalingId")) {
            "generasjonen har ikke utbetalingId"
        }
        val utbetalingId = generasjon.path("utbetalingId").asText().uuid

        val utbetalingstatusForGenerasjonen = utbetalingstatus(utbetalingId)
        if (utbetalingstatusForGenerasjonen == "FORKASTET") {
            forkastedeUtbetalinger.add(generasjon.deepCopy())
            return null
        }

        val sykdomstidslinje = generasjon.path("sykdomstidslinje").deepCopy<ObjectNode>().takeIf { sykdomstidslinje ->
            sykdomstidslinje.path("periode").hasNonNull("fom")
        } ?: dokumentsporing.dokumenter.firstNotNullOfOrNull { dokument ->
            sykdomstidslinjesubsetting(dokument.id, sykmeldingsperiodeFom, sykmeldingsperiodeFom)
        }
        checkNotNull(sykdomstidslinje) {
            "sykdomstidslinjen er tom for vedtaksperiode"
        }
        val fom = sykdomstidslinje.path("periode").path("fom").asText().dato
        val tom = sykdomstidslinje.path("periode").path("tom").asText().dato
        val vilkårsgrunnlagId = generasjon.path("vilkårsgrunnlagId").asText().uuid
        val forkastedeUtbetalingerSomEndring = endringerFraForkastetUtbetaling(forkastedeUtbetalinger, opprettettidspunkt, sykmeldingsperiodeFom, sykmeldingsperiodeTom, tidligsteTidspunktForHendelse, tidspunktForUtbetalingOpprettelse)
        val utbetalingOpprettet = checkNotNull(tidspunktForUtbetalingOpprettelse(utbetalingId)) {
            "forventer å finne tidspunkt for når utbetaling er opprettet"
        }
        var forrigeSykdomstidslinje: ObjectNode = sykdomstidslinje
        fun sykdomstidslinjeSubsetForPeriode(hendelseId: UUID) = sykdomstidslinjesubsetting(hendelseId, fom, tom)?.also { forrigeSykdomstidslinje = it } ?: forrigeSykdomstidslinje
        val egneEndringer = generasjon.path("dokumentsporing").dokumenter
            .endringerSomIkkeInngårIForkastedeUtbetalinger(forkastedeUtbetalingerSomEndring)
            .endringerSomIkkeInngårITidligereGenerasjoner(dokumenterHåndtertAvTidligereGenerasjoner)
            .map { oversettDokumentsporingTilEndring(it, opprettettidspunkt, tidligsteTidspunktForHendelse, sykmeldingsperiodeFom, sykmeldingsperiodeTom, ::sykdomstidslinjeSubsetForPeriode, null, null, null) }
        val endringer = endringer(egneEndringer, forkastedeUtbetalingerSomEndring)
        val utbetalingVurdert = tidspunktForUtbetalingVurdert(utbetalingId)
        val utbetalingAvsluttet = tidspunktForUtbetalingAvsluttet(utbetalingId)
        val vedtakFattet = utbetalingVurdert?.takeUnless { utbetalingstatusForGenerasjonen == "IKKE_GODKJENT" }
        val generasjontype = utbetalingtype(utbetalingId)
        val generasjonAvsluttet = utbetalingAvsluttet ?: utbetalingVurdert?.takeIf { utbetalingstatusForGenerasjonen == "IKKE_GODKJENT" && generasjontype == "UTBETALING" }

        check(endringer.isNotEmpty()) {
            "Vedtaksperioden $fom - $tom (${periode.path("id").asText()}) har ingen hendelser for generasjon med utbetaling $utbetalingId"
        }

        val tilstand = when {
            utbetalingAvsluttet != null -> "VEDTAK_IVERKSATT"
            utbetalingVurdert != null -> when (utbetalingstatusForGenerasjonen) {
                "IKKE_GODKJENT" -> when (generasjontype) {
                    "UTBETALING" -> when (periode.path("tilstand").asText()) {
                        "TIL_INFOTRYGD" -> "TIL_INFOTRYGD"
                        else -> "BEREGNET_OMGJØRING"
                    }
                    "REVURDERING" -> "REVURDERT_VEDTAK_AVVIST"
                    else -> error("forventer ikke utbetalingtypen: $generasjontype")
                }
                else -> "VEDTAK_FATTET"
            }
            generasjontype == "REVURDERING" -> "BEREGNET_REVURDERING"
            else -> "BEREGNET"
        }
        val endringMedUtbetaling = endringer.last().deepCopy().apply {
            put("tidsstempel", "$utbetalingOpprettet")
            put("utbetalingId", "$utbetalingId")
            put("vilkårsgrunnlagId", "$vilkårsgrunnlagId")
        }
        return lagGenerasjon(tilstand, LocalDateTime.parse(endringer.first().path("tidsstempel").asText()), fom, tom, endringer.plusElement(endringMedUtbetaling), vedtakFattet, generasjonAvsluttet.takeUnless { tilstand == "BEREGNET_OMGJØRING" })
    }

    private fun endringer(a: List<ObjectNode>, b: List<ObjectNode>) = (a + b).sortedBy {
        LocalDateTime.parse(it.path("tidsstempel").asText())
    }
    private fun List<Dokumentsporing>.endringerSomIkkeInngårIForkastedeUtbetalinger(forkastedeUtbetalingerSomEndring: List<ObjectNode>) =
        filterNot { dokument ->
            dokument in forkastedeUtbetalingerSomEndring.map { it.path("dokumentsporing").dokumentsporing }
        }
    private fun List<Dokumentsporing>.endringerSomIkkeInngårITidligereGenerasjoner(dokumenterHåndtertAvTidligereGenerasjoner: List<Dokumentsporing>) =
        filterNot { dokument ->
            dokument in dokumenterHåndtertAvTidligereGenerasjoner
        }

    private fun endringerFraForkastetUtbetaling(forkastedeUtbetalinger: List<ObjectNode>, opprettettidspunkt: LocalDateTime, sykmeldingsperiodeFom: LocalDate, sykmeldingsperiodeTom: LocalDate, tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?, tidspunktForUtbetalingOpprettelse: (UUID) -> LocalDateTime?): List<ObjectNode> {
        return forkastedeUtbetalinger.flatMap { forkastet ->
            val sisteHendelse = forkastet.path("dokumentsporing").last().dokumentsporing
            val forkastetSykdomstidslinje = forkastet.path("sykdomstidslinje").deepCopy<ObjectNode>()
            val forkastetUtbetalingId = forkastet.path("utbetalingId").asText().uuid
            val forkastetVilkårsgrunnlagId = forkastet.path("vilkårsgrunnlagId").asText().uuid
            val forkastetUtbetalingOpprettet = checkNotNull(tidspunktForUtbetalingOpprettelse(forkastetUtbetalingId)) {
                "forventer at en forkastet utbetaling kan mappes til et opprettelsestidspunkt"
            }
            listOf(
                oversettDokumentsporingTilEndring(sisteHendelse, opprettettidspunkt, tidligsteTidspunktForHendelse, sykmeldingsperiodeFom, sykmeldingsperiodeTom, { forkastetSykdomstidslinje }, null, null, null),
                oversettDokumentsporingTilEndring(sisteHendelse, opprettettidspunkt, tidligsteTidspunktForHendelse, sykmeldingsperiodeFom, sykmeldingsperiodeTom, { forkastetSykdomstidslinje }, forkastetUtbetalingId, forkastetVilkårsgrunnlagId, forkastetUtbetalingOpprettet)
            )
        }
    }

    private fun oversettDokumentsporingTilEndring(
        dokument: Dokumentsporing,
        opprettettidspunkt: LocalDateTime,
        tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?,
        sykmeldingsperiodeFom: LocalDate,
        sykmeldingsperiodeTom: LocalDate,
        sykdomstidslinjesubsetting: (UUID) -> ObjectNode,
        forkastetUtbetalingId: UUID? = null,
        forkastetVilkårsgrunnlagId: UUID? = null,
        forkastetUtbetalingOpprettet: LocalDateTime? = null
    ): ObjectNode {
        val tidspunktForEndring = forkastetUtbetalingOpprettet ?: tidligsteTidspunktForHendelse(dokument.id)?.coerceAtLeast(opprettettidspunkt) ?: opprettettidspunkt
        val sykdomstidslinjeForEndring = sykdomstidslinjesubsetting(dokument.id)
        return lagEndring(dokument.id, dokument.type, tidspunktForEndring, sykmeldingsperiodeFom, sykmeldingsperiodeTom, sykdomstidslinjeForEndring, forkastetUtbetalingId, forkastetVilkårsgrunnlagId)
    }

    private data class Dokumentsporing(
        val id: UUID,
        val type: String
    ) {
        companion object {
            val JsonNode.dokumentsporing get() = Dokumentsporing(
                id = this.path("dokumentId").asText().uuid,
                type = this.path("dokumenttype").asText()
            )
            val JsonNode.dokumenter get() = map { it.dokumentsporing }
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
