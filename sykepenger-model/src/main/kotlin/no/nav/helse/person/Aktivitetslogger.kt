package no.nav.helse.person

import no.nav.helse.behov.Pakke
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogger(private val originalMessage: String? = null) : IAktivitetslogger {
    private val aktiviteter = mutableListOf<Aktivitet>()

    fun accept(visitor: AktivitetsloggerVisitor) {
        visitor.preVisitAktivitetslogger(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogger(this)
    }

    override fun info(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet.Info(String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet.Warn(String.format(melding, *params)))
    }

    override fun need(type: Aktivitet.Need.NeedType, melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet.Need(type, String.format(melding, *params)))
    }

    override fun error(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet.Error(String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any): Nothing {
        aktiviteter.add(Aktivitet.Severe(String.format(melding, *params)))
        throw AktivitetException(this)
    }

    override fun hasMessages() = info().isNotEmpty() || hasWarnings()

    override fun hasWarnings() = warn().isNotEmpty() || hasNeeds()

    override fun hasNeeds() = need().isNotEmpty() || hasErrors()

    override fun hasErrors() = error().isNotEmpty() || severe().isNotEmpty()

    override fun addAll(other: Aktivitetslogger, label: String) {
        this.aktiviteter.addAll(other.aktiviteter.map { it.cloneWith(label) })
        this.aktiviteter.sort()
    }

    fun toReport(): String {
        if (!hasMessages()) return "Ingen meldinger eller problemer\n"
        val results = StringBuffer()
        results.append("Meldinger eller problemer finnes. ${originalMessage?.let { "Original melding: $it" }
            ?: ""} \n\t")
        append("Severe errors", severe(), results)
        append("Errors", error(), results)
        append("Needs", need(), results)
        append("Warnings", warn(), results)
        append("Information", info(), results)
        results.append("\n")
        return results.toString()
    }

    private fun append(label: String, messages: List<Aktivitet>, results: StringBuffer) {
        if (messages.isEmpty()) return
        results.append("\n")
        results.append(label)
        results.append(": ")
        results.append(messages.size)
        for (message in messages) {
            results.append("\n\t")
            results.append(message)
        }
    }

    override fun toString() = this.aktiviteter.map { it.inOrder() }.fold("") { acc, s -> acc + "\n" + s }

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    private fun warn() = Aktivitet.Warn.filter(aktiviteter)
    private fun need() = Aktivitet.Need.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)

    class AktivitetException internal constructor(private val aktivitetslogger: Aktivitetslogger) :
        RuntimeException(aktivitetslogger.toString()) {
        fun accept(visitor: AktivitetsloggerVisitor) {
            aktivitetslogger.accept(visitor)
        }
    }

    sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        private var melding: String,
        private val tidsstempel: String
    ) : Comparable<Aktivitet> {
        companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        protected abstract fun label(): Char
        internal abstract fun cloneWith(label: String): Aktivitet

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        override fun toString() = tidsstempel + "\t" + melding
        internal fun inOrder() = label() + "\t" + tidsstempel + "\t" + melding

        abstract fun accept(visitor: AktivitetsloggerVisitor)

        class Info(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(0, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }
            }

            override fun label() = 'I'

            override fun cloneWith(label: String) = Info("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggerVisitor) {
                visitor.visitInfo(this, melding, tidsstempel)
            }
        }

        class Warn(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(25, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Warn> {
                    return aktiviteter.filterIsInstance<Warn>()
                }
            }

            override fun label() = 'W'

            override fun cloneWith(label: String) = Warn("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggerVisitor) {
                visitor.visitWarn(this, melding, tidsstempel)
            }
        }

        class Need(
            private val type: NeedType,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, melding, tidsstempel) {
            sealed class NeedType(private val transportpakke: Pakke.Transportpakke) {
                fun pakke(): Pakke = transportpakke + pakke()
                protected abstract fun tilPakke(): Pakke

                class GjennomgåTidslinje internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("GjennomgåTidslinje"))
                }

                class Sykepengehistorikk internal constructor(
                    transportpakke: Pakke.Transportpakke,
                    private val utgangspunktForBeregningAvYtelse: LocalDate
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(
                        listOf("Sykepengehistorikk"),
                        mapOf("utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse)
                    )
                }

                class Foreldrepenger internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("Foreldrepenger"))
                }

                class Inntektsberegning internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("Inntektsberegning"))
                }

                class EgenAnsatt internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("EgenAnsatt"))
                }

                class Opptjening internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("Opptjening"))
                }

                class Godkjenning internal constructor(
                    transportpakke: Pakke.Transportpakke
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(listOf("Godkjenning"))
                }

                class Utbetaling internal constructor(
                    transportpakke: Pakke.Transportpakke,
                    private val utbetalingsreferanse: String,
                    private val utbetalingslinjer: List<Utbetalingslinje>,
                    private val maksdato: LocalDate,
                    private val saksbehandler: String
                ) : NeedType(transportpakke) {
                    override fun tilPakke() = Pakke(
                        listOf("Utbetaling"), mapOf(
                            "utbetalingsreferanse" to utbetalingsreferanse,
                            "utbetalingslinjer" to utbetalingslinjer,
                            "maksdato" to maksdato,
                            "saksbehandler" to saksbehandler
                        )
                    )
                }
            }

            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Need> {
                    return aktiviteter.filterIsInstance<Need>()
                }
            }

            override fun label() = 'N'

            override fun cloneWith(label: String) = Need(type, "$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggerVisitor) {
                visitor.visitNeed(this, type, melding, tidsstempel)
            }

        }

        class Error(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(75, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Error> {
                    return aktiviteter.filterIsInstance<Error>()
                }
            }

            override fun label() = 'E'

            override fun cloneWith(label: String) = Error("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggerVisitor) {
                visitor.visitError(this, melding, tidsstempel)
            }
        }

        class Severe(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Severe> {
                    return aktiviteter.filterIsInstance<Severe>()
                }
            }

            override fun label() = 'S'

            override fun cloneWith(label: String) = Severe("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggerVisitor) {
                visitor.visitSevere(this, melding, tidsstempel)
            }
        }
    }
}

interface IAktivitetslogger {
    fun info(melding: String, vararg params: Any)
    fun warn(melding: String, vararg params: Any)
    fun need(type: Aktivitetslogger.Aktivitet.Need.NeedType, melding: String, vararg params: Any)
    fun error(melding: String, vararg params: Any)
    fun severe(melding: String, vararg params: Any): Nothing

    fun hasMessages(): Boolean
    fun hasWarnings(): Boolean
    fun hasNeeds(): Boolean
    fun hasErrors(): Boolean

    fun addAll(other: Aktivitetslogger, label: String)
}
