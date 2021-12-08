package no.nav.helse.person

import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.TidslinjegrunnlagVisitor.Periode.Companion.dager
import no.nav.helse.person.Bokstav.BOKSTAV_A
import no.nav.helse.person.Ledd.*
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf.*
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(
    private var forelder: Aktivitetslogg? = null
) : IAktivitetslogg {
    internal val aktiviteter = mutableListOf<Aktivitet>()
    private val kontekster = mutableListOf<Aktivitetskontekst>()  // Doesn't need serialization

    internal fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun info(melding: String, vararg params: Any?) {
        add(Aktivitet.Info(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any?) {
        add(Aktivitet.Warn(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) {
        add(Behov(type, kontekster.toSpesifikk(), melding, detaljer))
    }

    override fun error(melding: String, vararg params: Any?) {
        add(Aktivitet.Error(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any?): Nothing {
        add(Aktivitet.Severe(kontekster.toSpesifikk(), String.format(melding, *params)))

        throw AktivitetException(this)
    }

    override fun juridiskVurdering(melding: String, vurdering: Etterlevelse.Vurderingsresultat) {
        add(Etterlevelse(kontekster.toSpesifikk(), melding, vurdering))
    }

    private fun add(aktivitet: Aktivitet) {
        this.aktiviteter.add(aktivitet)
        forelder?.add(aktivitet)
    }

    private fun MutableList<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun hasActivities() = info().isNotEmpty() || hasWarningsOrWorse() || behov().isNotEmpty()

    override fun hasWarningsOrWorse() = warn().isNotEmpty() || hasErrorsOrWorse()

    override fun hasErrorsOrWorse() = error().isNotEmpty() || severe().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    override fun toString() = this.aktiviteter.map { it.inOrder() }.joinToString(separator = "\n") { it }

    override fun aktivitetsteller() = aktiviteter.size

    override fun kontekst(kontekst: Aktivitetskontekst) {
        kontekster.add(kontekst)
    }

    override fun kontekst(person: Person) {
        forelder = person.aktivitetslogg
        kontekst(person as Aktivitetskontekst)
    }

    override fun toMap(): Map<String, List<Map<String, Any>>> = AktivitetsloggMap(this).toMap()

    internal fun logg(vararg kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { it in aktivitet }
            })
        }
    }

    internal fun logg(vararg kontekst: String): Aktivitetslogg {
        return Aktivitetslogg(this).also { aktivitetslogg ->
            aktivitetslogg.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { kontekst -> kontekst in aktivitet.kontekster.map { it.kontekstType } }
            })
        }
    }

    override fun kontekster() =
        aktiviteter
            .groupBy { it.kontekst(null) }
            .map { Aktivitetslogg(this).apply { aktiviteter.addAll(it.value) } }

    override fun hendelseskontekster(): Map<String, String> {
        return kontekster
            .map(Aktivitetskontekst::toSpesifikkKontekst)
            .filter { it.kontekstType in MODELL_KONTEKSTER }
            .map(SpesifikkKontekst::kontekstMap)
            .fold(mapOf()) { result, kontekst -> result + kontekst }
    }

    override fun hendelseskontekst(): Hendelseskontekst = Hendelseskontekst(hendelseskontekster())

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    internal fun warn() = Aktivitet.Warn.filter(aktiviteter)
    override fun behov() = Behov.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)
    override fun juridiskeVurderinger() = Aktivitet.Etterlevelse.filter(aktiviteter)

    companion object {
        private val MODELL_KONTEKSTER: Array<String> = arrayOf("Person", "Arbeidsgiver", "Vedtaksperiode")
    }

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {

        fun kontekst() = aktivitetslogg.kontekster.fold(mutableMapOf<String, String>()) { result, kontekst ->
            result.apply { putAll(kontekst.toSpesifikkKontekst().kontekstMap) }
        }

        fun aktivitetslogg() = aktivitetslogg
    }

    sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        private val label: Char,
        private var melding: String,
        private val tidsstempel: String,
        internal val kontekster: List<SpesifikkKontekst>
    ) : Comparable<Aktivitet> {
        private companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        fun kontekst(): Map<String, String> = kontekst(null)

        internal fun kontekst(typer: Array<String>?): Map<String, String> = kontekster
            .filter { typer == null || it.kontekstType in typer }
            .fold(mapOf()) { result, kontekst -> result + kontekst.kontekstMap }

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        internal fun inOrder() = label + "\t" + this.toString()

        override fun toString() = label + "  \t" + tidsstempel + "  \t" + melding + meldingerString()

        private fun meldingerString(): String {
            return kontekster.joinToString(separator = "") { " (${it.melding()})" }
        }

        internal abstract fun accept(visitor: AktivitetsloggVisitor)

        operator fun contains(kontekst: Aktivitetskontekst) = kontekst.toSpesifikkKontekst() in kontekster
        internal class Info(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(0, 'I', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }

            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitInfo(kontekster, this, melding, tidsstempel)
            }

        }

        internal class Warn(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(25, 'W', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Warn> {
                    return aktiviteter.filterIsInstance<Warn>()
                }

            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitWarn(kontekster, this, melding, tidsstempel)
            }

        }

        class Behov(
            val type: Behovtype,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val detaljer: Map<String, Any?> = emptyMap(),
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, 'N', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Behov> {
                    return aktiviteter.filterIsInstance<Behov>()
                }

                internal fun utbetalingshistorikk(
                    aktivitetslogg: IAktivitetslogg,
                    periode: Periode
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Sykepengehistorikk, "Trenger sykepengehistorikk fra Infotrygd", mapOf(
                            "historikkFom" to periode.start.toString(),
                            "historikkTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun foreldrepenger(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(
                        Behovtype.Foreldrepenger,
                        "Trenger informasjon om foreldrepengeytelser fra FPSAK"
                    )
                }

                internal fun pleiepenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Pleiepenger,
                        "Trenger informasjon om pleiepengeytelser fra Infotrygd", mapOf(
                            "pleiepengerFom" to periode.start.toString(),
                            "pleiepengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun omsorgspenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Omsorgspenger,
                        "Trenger informasjon om omsorgspengerytelser fra Infotrygd", mapOf(
                            "omsorgspengerFom" to periode.start.toString(),
                            "omsorgspengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun opplæringspenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Opplæringspenger,
                        "Trenger informasjon om opplæringspengerytelser fra Infotrygd", mapOf(
                            "opplæringspengerFom" to periode.start.toString(),
                            "opplæringspengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun institusjonsopphold(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Institusjonsopphold,
                        "Trenger informasjon om institusjonsopphold fra Inst2", mapOf(
                            "institusjonsoppholdFom" to periode.start.toString(),
                            "institusjonsoppholdTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun dødsinformasjon(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(
                        Behovtype.Dødsinfo,
                        "Trenger informasjon om dødsdato fra PDL"
                    )
                }

                internal fun inntekterForSammenligningsgrunnlag(
                    aktivitetslogg: IAktivitetslogg,
                    beregningStart: YearMonth,
                    beregningSlutt: YearMonth
                ) {
                    aktivitetslogg.behov(
                        Behovtype.InntekterForSammenligningsgrunnlag,
                        "Trenger inntekter for sammenligningsgrunnlag",
                        mapOf(
                            "beregningStart" to beregningStart.toString(),
                            "beregningSlutt" to beregningSlutt.toString()
                        )
                    )
                }

                internal fun inntekterForSykepengegrunnlag(
                    aktivitetslogg: IAktivitetslogg,
                    beregningStart: YearMonth,
                    beregningSlutt: YearMonth
                ) {
                    aktivitetslogg.behov(
                        Behovtype.InntekterForSykepengegrunnlag,
                        "Trenger inntekter for sykepengegrunnlag",
                        mapOf(
                            "beregningStart" to beregningStart.toString(),
                            "beregningSlutt" to beregningSlutt.toString()
                        )
                    )
                }

                internal fun arbeidsforhold(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(Behovtype.ArbeidsforholdV2, "Trenger informasjon om arbeidsforhold")
                }

                internal fun dagpenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Dagpenger, "Trenger informasjon om dagpenger", mapOf(
                            "periodeFom" to fom.toString(),
                            "periodeTom" to tom.toString()
                        )
                    )
                }

                internal fun arbeidsavklaringspenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Arbeidsavklaringspenger, "Trenger informasjon om arbeidsavklaringspenger", mapOf(
                            "periodeFom" to fom.toString(),
                            "periodeTom" to tom.toString()
                        )
                    )
                }

                internal fun medlemskap(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Medlemskap, "Trenger informasjon om medlemskap", mapOf(
                            "medlemskapPeriodeFom" to fom.toString(),
                            "medlemskapPeriodeTom" to tom.toString()
                        )
                    )
                }

                internal fun godkjenning(
                    aktivitetslogg: IAktivitetslogg,
                    periodeFom: LocalDate,
                    periodeTom: LocalDate,
                    skjæringstidspunkt: LocalDate,
                    vedtaksperiodeaktivitetslogg: Aktivitetslogg,
                    periodetype: Periodetype,
                    utbetalingtype: Utbetalingtype,
                    inntektskilde: Inntektskilde,
                    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
                    orgnummereMedAktiveArbeidsforhold: List<String>,
                    arbeidsforholdId: String?,
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Godkjenning, "Forespør godkjenning fra saksbehandler", mapOf(
                            "periodeFom" to periodeFom.toString(),
                            "periodeTom" to periodeTom.toString(),
                            "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                            "periodetype" to periodetype.name,
                            "utbetalingtype" to utbetalingtype.name,
                            "inntektskilde" to inntektskilde.name,
                            "warnings" to Aktivitetslogg().apply {
                                aktiviteter.addAll(vedtaksperiodeaktivitetslogg.warn())

                            }.toMap(),
                            "aktiveVedtaksperioder" to aktiveVedtaksperioder.map(AktivVedtaksperiode::toMap),
                            "orgnummereMedAktiveArbeidsforhold" to orgnummereMedAktiveArbeidsforhold,
                            "arbeidsforholdId" to arbeidsforholdId
                        )
                    )
                }
            }

            fun detaljer() = detaljer

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitBehov(kontekster, this, type, melding, detaljer, tidsstempel)
            }

            enum class Behovtype {
                Sykepengehistorikk,
                SykepengehistorikkForFeriepenger,
                Foreldrepenger,
                Pleiepenger,
                Omsorgspenger,
                Opplæringspenger,
                Institusjonsopphold,
                EgenAnsatt,
                Godkjenning,
                Simulering,
                Utbetaling,
                InntekterForSammenligningsgrunnlag,
                InntekterForSykepengegrunnlag,

                @Deprecated("Behovet er ikke i bruk, men beholdes for derserialisering av aktivitetsloggen")
                Opptjening,
                Dagpenger,
                Arbeidsavklaringspenger,
                Medlemskap,
                Dødsinfo,
                ArbeidsforholdV2
            }
        }

        internal class Error(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(75, 'E', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Error> {
                    return aktiviteter.filterIsInstance<Error>()
                }
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitError(kontekster, this, melding, tidsstempel)
            }
        }

        internal class Severe(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, 'S', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Severe> {
                    return aktiviteter.filterIsInstance<Severe>()
                }
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitSevere(kontekster, this, melding, tidsstempel)
            }
        }

        class Etterlevelse internal constructor(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val vurdering: Vurderingsresultat,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, 'J', melding, tidsstempel, kontekster) {
            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.preVisitEtterlevelse(kontekster, this, melding, vurdering, tidsstempel)
                vurdering.accept(visitor)
                visitor.postVisitEtterlevelse(kontekster, this, melding, vurdering, tidsstempel)
            }

            internal companion object {
                fun filter(aktiviteter: List<Aktivitet>) = aktiviteter.filterIsInstance<Etterlevelse>()
            }

            class Vurderingsresultat private constructor(
                private val oppfylt: Boolean,
                private val versjon: LocalDate,
                private val paragraf: Paragraf,
                private val ledd: Ledd,
                private val punktum: List<Punktum>,
                private val bokstav: List<Bokstav> = emptyList(),
                private val inputdata: Map<Any, Any?>,
                private val outputdata: Map<Any, Any?>
            ) {
                internal fun accept(visitor: AktivitetsloggVisitor) {
                    visitor.visitVurderingsresultat(oppfylt, versjon, paragraf, ledd, punktum, bokstav, outputdata, inputdata)
                }

                override fun toString(): String {
                    return """
                Juridisk vurdering:
                    oppfylt: $oppfylt
                    versjon: $versjon
                    paragraf: $paragraf
                    ledd: $ledd
                    input: ${
                        inputdata.map { (key, value) ->
                            "$key: $value"
                        }
                    }
                    output: ${
                        outputdata.map { (key, value) ->
                            "$key: $value"
                        }
                    }
            """
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other !is Vurderingsresultat || javaClass != other.javaClass) return false

                    return oppfylt == other.oppfylt
                        && versjon == other.versjon
                        && paragraf == other.paragraf
                        && ledd == other.ledd
                        && inputdata == other.inputdata
                        && outputdata == other.outputdata
                }

                override fun hashCode(): Int {
                    var result = oppfylt.hashCode()
                    result = 31 * result + versjon.hashCode()
                    result = 31 * result + paragraf.hashCode()
                    result = 31 * result + ledd.hashCode()
                    result = 31 * result + inputdata.hashCode()
                    result = 31 * result + outputdata.hashCode()
                    return result
                }

                internal companion object {
                    internal fun filter(aktiviteter: List<Aktivitet>): List<Etterlevelse> {
                        return aktiviteter.filterIsInstance<Etterlevelse>()
                    }

                    internal fun IAktivitetslogg.`§2`(oppfylt: Boolean) {}

                    internal fun IAktivitetslogg.`§8-2 ledd 1`(
                        oppfylt: Boolean,
                        skjæringstidspunkt: LocalDate,
                        tilstrekkeligAntallOpptjeningsdager: Int,
                        arbeidsforhold: List<Map<String, Any?>>,
                        antallOpptjeningsdager: Int
                    ) {
                        juridiskVurdering(
                            "", Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2020, 6, 12),
                                paragraf = PARAGRAF_8_2,
                                ledd = 1.ledd,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "skjæringstidspunkt" to skjæringstidspunkt,
                                    "tilstrekkeligAntallOpptjeningsdager" to tilstrekkeligAntallOpptjeningsdager,
                                    "arbeidsforhold" to arbeidsforhold
                                ),
                                outputdata = mapOf(
                                    "antallOpptjeningsdager" to antallOpptjeningsdager
                                )
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-3 ledd 1 punktum 2`(
                        oppfylt: Boolean,
                        syttiårsdagen: LocalDate,
                        vurderingFom: LocalDate,
                        vurderingTom: LocalDate,
                        tidslinjeFom: LocalDate,
                        tidslinjeTom: LocalDate,
                        avvisteDager: List<LocalDate>
                    ) {
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2011, 12, 16),
                                paragraf = PARAGRAF_8_3,
                                ledd = 1.ledd,
                                punktum = 2.punktum,
                                inputdata = mapOf(
                                    "syttiårsdagen" to syttiårsdagen,
                                    "vurderingFom" to vurderingFom,
                                    "vurderingTom" to vurderingTom,
                                    "tidslinjeFom" to tidslinjeFom,
                                    "tidslinjeTom" to tidslinjeTom
                                ),
                                outputdata = mapOf(
                                    "avvisteDager" to avvisteDager.grupperSammenhengendePerioder()
                                )
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-3 ledd 2 punktum 1`(
                        oppfylt: Boolean,
                        skjæringstidspunkt: LocalDate,
                        grunnlagForSykepengegrunnlag: Inntekt,
                        minimumInntekt: Inntekt
                    ) {
                        juridiskVurdering("",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2011, 12, 16),
                                paragraf = PARAGRAF_8_3,
                                ledd = 2.ledd,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "skjæringstidspunkt" to skjæringstidspunkt,
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                                ),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-10 ledd 2`(
                        oppfylt: Boolean,
                        funnetRelevant: Boolean,
                        maksimaltSykepengegrunnlag: Inntekt,
                        skjæringstidspunkt: LocalDate,
                        grunnlagForSykepengegrunnlag: Inntekt
                    ) {
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt,
                                versjon = LocalDate.of(2020, 1, 1),
                                PARAGRAF_8_10,
                                LEDD_2,
                                1.punktum,
                                inputdata = mapOf(
                                    "maksimaltSykepengegrunnlag" to maksimaltSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                                    "skjæringstidspunkt" to skjæringstidspunkt,
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig }
                                ),
                                outputdata = mapOf(
                                    "funnetRelevant" to funnetRelevant
                                )
                            )
                        )
                    }

                    //TODO: Hvordan skal denne kunne legges inn???
                    internal fun IAktivitetslogg.`§8-10 ledd 3`(oppfylt: Boolean) {}

                    internal fun IAktivitetslogg.`§8-11 første ledd`() {
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt = true,
                                paragraf = PARAGRAF_8_11,
                                ledd = LEDD_1,
                                punktum = 1.punktum,
                                versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
                                inputdata = emptyMap(),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-12 ledd 1 punktum 1`(
                        oppfylt: Boolean,
                        fom: LocalDate,
                        tom: LocalDate,
                        tidslinjegrunnlag: List<Utbetalingstidslinje>,
                        beregnetTidslinje: Utbetalingstidslinje,
                        gjenståendeSykedager: Int,
                        forbrukteSykedager: Int,
                        maksdato: LocalDate,
                        avvisteDager: List<LocalDate>
                    ) {
                        juridiskVurdering(
                            "§8-12 ledd 1",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2021, 5, 21),
                                paragraf = PARAGRAF_8_12,
                                ledd = 1.ledd,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "fom" to fom,
                                    "tom" to tom,
                                    "tidslinjegrunnlag" to tidslinjegrunnlag.map { TidslinjegrunnlagVisitor(it).dager() },
                                    "beregnetTidslinje" to TidslinjegrunnlagVisitor(beregnetTidslinje).dager()
                                ),
                                outputdata = mapOf(
                                    "gjenståendeSykedager" to gjenståendeSykedager,
                                    "forbrukteSykedager" to forbrukteSykedager,
                                    "maksdato" to maksdato,
                                    "avvisteDager" to avvisteDager.grupperSammenhengendePerioder()
                                )
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-12 ledd 2`(
                        dato: LocalDate,
                        tilstrekkeligOppholdISykedager: Int,
                        tidslinjegrunnlag: List<Utbetalingstidslinje>,
                        beregnetTidslinje: Utbetalingstidslinje
                    ) {
                        juridiskVurdering(
                            "§8-12 ledd 2",
                            Vurderingsresultat(
                                oppfylt = true,
                                versjon = LocalDate.of(2021, 5, 21),
                                paragraf = PARAGRAF_8_12,
                                ledd = 2.ledd,
                                punktum = (1..2).punktum,
                                inputdata = mapOf(
                                    "dato" to dato,
                                    "tilstrekkeligOppholdISykedager" to tilstrekkeligOppholdISykedager,
                                    "tidslinjegrunnlag" to tidslinjegrunnlag.map { TidslinjegrunnlagVisitor(it).dager() },
                                    "beregnetTidslinje" to TidslinjegrunnlagVisitor(beregnetTidslinje).dager()
                                ),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-13 ledd 1`(oppfylt: Boolean, avvisteDager: List<LocalDate>) {
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                paragraf = PARAGRAF_8_13,
                                ledd = LEDD_1,
                                punktum = (1..2).punktum,
                                versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
                                inputdata = mapOf("avvisteDager" to avvisteDager),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-16 ledd 1`(oppfylt: Boolean) {} // Én gang, med periode over hvilke dager vi har satt dekningsgrad

                    internal fun IAktivitetslogg.`§8-17 ledd 1 bokstav a`(oppfylt: Boolean, arbeidsgiverperioder: List<LocalDate>, førsteNavdag: LocalDate) {
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2018, 1, 1),
                                paragraf = PARAGRAF_8_17,
                                ledd = LEDD_1,
                                punktum = 1.punktum,
                                bokstav = listOf(BOKSTAV_A),
                                inputdata = mapOf(
                                    "førsteNavdag" to førsteNavdag,
                                    "arbeidsgiverperioder" to arbeidsgiverperioder.grupperSammenhengendePerioder().map {
                                        mapOf("fom" to it.start, "tom" to it.endInclusive)
                                    }
                                ),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-17 ledd 2`(oppfylt: Boolean) {} //Legges inn på ferie/permisjonsdager i utbetalingstidslinje, med periodene av ferie/permisjon som input

                    internal fun IAktivitetslogg.`§8-28 ledd 3 bokstav a`(
                        oppfylt: Boolean,
                        inntekter: List<Inntektshistorikk.Skatt>,
                        inntekterSisteTreMåneder: List<Inntektshistorikk.Skatt>,
                        grunnlagForSykepengegrunnlag: Inntekt
                    ) {
                    }

                    internal fun IAktivitetslogg.`§8-30 ledd 1`(
                        oppfylt: Boolean,
                        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Inntekt>,
                        grunnlagForSykepengegrunnlag: Inntekt
                    ) {
                    val beregnetMånedsinntektPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver
                            .mapValues { it.value.reflection { _, månedlig, _, _ -> månedlig } }
                        juridiskVurdering(
                            "",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2019, 1, 1),
                                paragraf = PARAGRAF_8_30,
                                ledd = LEDD_1,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "beregnetMånedsinntektPerArbeidsgiver" to beregnetMånedsinntektPerArbeidsgiver
                                ),
                                outputdata = mapOf(
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig }
                                )
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-30 ledd 2 punktum 1`(
                        oppfylt: Boolean,
                        maksimaltTillattAvvikPåÅrsinntekt: Prosent,
                        grunnlagForSykepengegrunnlag: Inntekt,
                        sammenligningsgrunnlag: Inntekt,
                        avvik: Prosent
                    ) {
                        juridiskVurdering("Vurdering av avviksprosent ved inntektsvurdering",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2017, 4, 5),
                                paragraf = PARAGRAF_8_30,
                                ledd = 2.ledd,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "maksimaltTillattAvvikPåÅrsinntekt" to maksimaltTillattAvvikPåÅrsinntekt.prosent(),
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                                    "sammenligningsgrunnlag" to sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig }
                                ),
                                outputdata = mapOf(
                                    "avvik" to avvik.prosent()
                                )
                            ))
                    }

                    internal fun IAktivitetslogg.`§8-33 ledd 1`() {}

                    internal fun IAktivitetslogg.`§8-33 ledd 3`(
                        grunnlagForFeriepenger: Int,
                        opptjeningsår: Year,
                        prosentsats: Double,
                        alder: Int,
                        feriepenger: Double
                    ) {
                    }

                    internal fun IAktivitetslogg.`§8-51 ledd 2`(
                        oppfylt: Boolean,
                        skjæringstidspunkt: LocalDate,
                        grunnlagForSykepengegrunnlag: Inntekt,
                        minimumInntekt: Inntekt
                    ) {
                        juridiskVurdering("",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2011, 12, 16),
                                paragraf = PARAGRAF_8_51,
                                ledd = LEDD_2,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "skjæringstidspunkt" to skjæringstidspunkt,
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                                ),
                                outputdata = emptyMap()
                            )
                        )
                    }

                    internal fun IAktivitetslogg.`§8-51 ledd 3`(
                        oppfylt: Boolean,
                        skjæringstidspunkt: LocalDate,
                        grunnlagForSykepengegrunnlag: Inntekt,
                        minimumInntekt: Inntekt
                    ) {
                        juridiskVurdering("",
                            Vurderingsresultat(
                                oppfylt = oppfylt,
                                versjon = LocalDate.of(2011, 12, 16),
                                paragraf = PARAGRAF_8_51,
                                ledd = LEDD_3,
                                punktum = 1.punktum,
                                inputdata = mapOf(
                                    "skjæringstidspunkt" to skjæringstidspunkt,
                                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                                ),
                                outputdata = emptyMap()
                            )
                        )
                    }
                }
            }

            private class TidslinjegrunnlagVisitor(utbetalingstidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {
                private val navdager = mutableListOf<Periode>()
                private var forrigeDato: LocalDate? = null

                private class Periode(
                    val fom: LocalDate,
                    var tom: LocalDate,
                    val dagtype: String
                ) {
                    companion object {
                        fun List<Periode>.dager() = map {
                            mapOf(
                                "fom" to it.fom,
                                "tom" to it.tom,
                                "dagtype" to it.dagtype
                            )
                        }
                    }
                }

                init {
                    utbetalingstidslinje.accept(this)
                }

                fun dager() = navdager.dager()

                override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
                    visit(dato, "NAVDAG")
                }

                override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
                    visit(dato, "NAVDAG")
                }

                override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
                    if (forrigeDato != null && forrigeDato?.plusDays(1) == dato) visit(dato, "FRIDAG")
                }

                private fun visit(dato: LocalDate, dagtype: String) {
                    forrigeDato = dato
                    if (navdager.isEmpty() || dagtype != navdager.last().dagtype || navdager.last().tom.plusDays(1) != dato) {
                        navdager.add(Periode(dato, dato, dagtype))
                    } else {
                        navdager.last().tom = dato
                    }
                }
            }
        }

        internal data class AktivVedtaksperiode(
            val orgnummer: String,
            val vedtaksperiodeId: UUID,
            val periodetype: Periodetype
        ) {
            fun toMap() = mapOf<String, Any>(
                "orgnummer" to orgnummer,
                "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                "periodetype" to periodetype.name
            )
        }
    }
}

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun warn(melding: String, vararg params: Any?)
    fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any?> = emptyMap())
    fun error(melding: String, vararg params: Any?)
    fun severe(melding: String, vararg params: Any?): Nothing
    fun juridiskVurdering(melding: String, vurdering: Etterlevelse.Vurderingsresultat)

    fun hasActivities(): Boolean
    fun hasWarningsOrWorse(): Boolean
    fun hasErrorsOrWorse(): Boolean

    fun aktivitetsteller(): Int
    fun behov(): List<Behov>
    fun juridiskeVurderinger(): List<Etterlevelse>
    fun barn(): Aktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(person: Person)
    fun kontekster(): List<IAktivitetslogg>
    fun hendelseskontekster(): Map<String, String>
    fun hendelseskontekst(): Hendelseskontekst
    fun toMap(): Map<String, List<Map<String, Any>>>
}

internal interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Info,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitWarn(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Warn,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Behov,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
    }

    fun visitError(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Error,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitSevere(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Severe,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun preVisitEtterlevelse(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Etterlevelse,
        melding: String,
        vurderingsresultat: Etterlevelse.Vurderingsresultat,
        tidsstempel: String
    ) {
    }

    fun visitVurderingsresultat(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstav: List<Bokstav>,
        outputdata: Map<Any, Any?>,
        inputdata: Map<Any, Any?>
    ) {
    }

    fun postVisitEtterlevelse(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Etterlevelse,
        melding: String,
        vurderingsresultat: Etterlevelse.Vurderingsresultat,
        tidsstempel: String
    ) {
    }

    fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
}

interface Aktivitetskontekst {
    fun toSpesifikkKontekst(): SpesifikkKontekst
}

class SpesifikkKontekst(internal val kontekstType: String, internal val kontekstMap: Map<String, String> = mapOf()) {
    internal fun melding() =
        kontekstType + kontekstMap.entries.joinToString(separator = "") { " ${it.key}: ${it.value}" }

    override fun equals(other: Any?) =
        this === other || other is SpesifikkKontekst && this.kontekstMap == other.kontekstMap

    override fun hashCode() = kontekstMap.hashCode()
}
