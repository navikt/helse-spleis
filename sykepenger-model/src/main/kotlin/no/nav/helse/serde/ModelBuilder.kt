package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.InntektHistorie
import no.nav.helse.person.Person
import no.nav.helse.serde.reflection.create.ReflectionCreationHelper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Stack
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal class ModelBuilder(private val jsonString: String) : StructureVisitor {
    private var personResult: Person? = null
    private val hendelser = mutableMapOf<String, ArbeidstakerHendelse>()
    private val reflector = ReflectionCreationHelper()

    private val stack: Stack<ModelState> = Stack()

    internal fun result(): Person {
        if (personResult == null) parse()
        return personResult ?: throw RuntimeException("Kunne ikke gjenopprette personen")
    }

    private fun parse() {
        val json: JsonNode = jacksonObjectMapper().readTree(jsonString)
        stack.push(HendelserState())
        JsonVisitable(json["hendelser"]).accept(this)
        stack.push((PersonState()))
        JsonVisitable(json).accept(this)
    }

    private val currentState: ModelState
        get() = stack.peek()

    override fun toString() = currentState.toString()
    override fun preVisitArrayField(name: String) = currentState.preVisitArrayField(name)
    override fun postVisitArrayField() = currentState.postVisitArrayField()
    override fun preVisitObjectField(name: String) = currentState.preVisitObjectField(name)
    override fun postVisitObjectField() = currentState.postVisitObjectField()
    override fun visitStringField(name: String, value: String) = currentState.visitStringField(name, value)
    override fun visitBooleanField(name: String, value: Boolean) = currentState.visitBooleanField(name, value)
    override fun visitNumberField(name: String, value: Number) = currentState.visitNumberField(name, value)
    override fun preVisitArray() = currentState.preVisitArray()
    override fun postVisitArray() = currentState.postVisitArray()
    override fun preVisitObject() = currentState.preVisitObject()
    override fun postVisitObject() = currentState.postVisitObject()
    override fun visitString(value: String) = currentState.visitString(value)
    override fun visitBoolean(value: Boolean) = currentState.visitBoolean(value)
    override fun visitNumber(value: Number) = currentState.visitNumber(value)

    private interface ModelState : StructureVisitor

    private inner class HendelserState : ModelState {
        override fun preVisitObject() {
            stack.push(UkjentHendelseState())
        }

        override fun postVisitArray() {
            stack.pop()
        }
    }

    private inner class UkjentHendelseState : ModelState {
        private lateinit var type: String
        private lateinit var tidspunkt: LocalDateTime
        private lateinit var recordingState: VisitorRecordingState

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "type" -> type = value
                "tidspunkt" -> tidspunkt = LocalDateTime.parse(value)
            }
        }

        override fun preVisitObjectField(name: String) {
            if (name == "data") {
                recordingState = VisitorRecordingState()
                stack.push(recordingState)
            }
        }

        override fun postVisitObject() {
            when (type) {
                "Inntektsmelding" -> recordingState.accept(stack.push(InntektsmeldingDataState {
                    hendelser[it.hendelseId().toString()] = it
                }))
            }
            stack.pop()
        }
    }

    private inner class InntektsmeldingDataState(private val setter: (ModelInntektsmelding) -> Unit) : ModelState {
        private lateinit var hendelseId: String

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "hendelseId" -> hendelseId = value
            }
        }

        override fun postVisitObjectField() {
            setter(
                ModelInntektsmelding(
                    hendelseId = UUID.fromString(hendelseId),
                    refusjon = ModelInntektsmelding.Refusjon(LocalDate.now(), 30000.0, null),
                    orgnummer = "88888888",
                    fødselsnummer = "12020052345",
                    aktørId = "100010101010",
                    mottattDato = LocalDateTime.now(),
                    førsteFraværsdag = LocalDate.now(),
                    beregnetInntekt = 30000.0,
                    aktivitetslogger = Aktivitetslogger(),
                    originalJson = "{}",
                    arbeidsgiverperioder = emptyList(),
                    ferieperioder = emptyList()
                )
            )
            stack.pop()
        }
    }

    private inner class PersonState : ModelState {
        private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
        private lateinit var aktørId: String
        private lateinit var fødselsnummer: String

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "aktørId" -> aktørId = value
                "fødselsnummer" -> fødselsnummer = value
            }
        }

        override fun postVisitObject() {
            val res = Person(aktørId, fødselsnummer)
            val personArbeidsgivere = res.privatProp<MutableMap<String, Arbeidsgiver>>("arbeidsgivere")
            personArbeidsgivere.putAll(arbeidsgivere)
            personResult = res
        }

        override fun preVisitArrayField(name: String) {
            if (name == "arbeidsgivere") {
                stack.push(ArbeidsgivereArrayState(arbeidsgivere))
            }
        }
    }

    private inner class ArbeidsgivereArrayState(
        private val arbeidsgivere: MutableMap<String, Arbeidsgiver>
    ) : ModelState {
        override fun preVisitObject() {
            stack.push(ArbeidsgiverState { orgnr, arbeidsgiver ->
                arbeidsgivere[orgnr] = arbeidsgiver
            })
        }

        override fun postVisitArrayField() {
            stack.pop()
        }
    }

    private inner class ArbeidsgiverState(private val setter: (String, Arbeidsgiver) -> Unit) : ModelState {
        private lateinit var organisasjonsnummer: String
        private lateinit var uuid: UUID
        private lateinit var inntektHistorie: InntektHistorie

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "organisasjonsnummer" -> organisasjonsnummer = value
                "id" -> uuid = UUID.fromString(value)
            }
        }

        override fun preVisitArrayField(name: String) {
            if (name == "inntekter") {
                inntektHistorie = InntektHistorie()
                stack.push(InntekterArrayState(inntektHistorie))
            }
        }

        override fun postVisitObject() {
            setter(
                organisasjonsnummer, reflector.lagArbeidsgiver(
                    organisasjonsnummer, uuid, inntektHistorie
                )
            )
            stack.pop()
        }
    }

    private inner class InntekterArrayState(private val inntektHistorie: InntektHistorie) : ModelState {
        override fun preVisitObject() {
            stack.push(InntektState { dagen: LocalDate, hendelse: ModelInntektsmelding, beløp: BigDecimal ->
                inntektHistorie.add(dagen, hendelse, beløp)
            })
        }

        override fun postVisitArrayField() {
            stack.pop()
        }
    }

    private inner class InntektState(
        private val setter: (dagen: LocalDate, hendelse: ModelInntektsmelding, beløp: BigDecimal) -> Unit
    ) : ModelState {
        private lateinit var fom: LocalDate
        private lateinit var hendelse: ModelInntektsmelding
        private lateinit var beløp: BigDecimal

        override fun visitStringField(name: String, value: String) {
            when (name) {
                "fom" -> fom = LocalDate.parse(value)
                "hendelse" -> hendelse =
                    hentHendelseMedId(value) as ModelInntektsmelding? ?: error("Fant ikke hendelse med id: $value")
            }
        }

        override fun visitNumberField(name: String, value: Number) {
            when (name) {
                "beløp" -> beløp = BigDecimal.valueOf(value.toDouble()) // TODO: ok?
            }
        }

        override fun postVisitObject() {
            setter(fom, hendelse, beløp)
            stack.pop()
        }
    }

    private fun hentHendelseMedId(id: String) = hendelser[id]

    private inner class VisitorRecordingState : ModelState {
        private var objectFieldDepth = 0
        private val recordedVisits = mutableListOf<(ModelState) -> Unit>()

        fun accept(modelState: ModelState) = recordedVisits.forEach { it(modelState) }

        override fun preVisitObjectField(name: String) {
            recordedVisits.record { it.preVisitObjectField(name) }
            objectFieldDepth++
        }

        override fun postVisitObjectField() {
            recordedVisits.record { it.postVisitObjectField() }
            if (objectFieldDepth == 0) stack.pop()
            objectFieldDepth--
        }

        override fun preVisitArrayField(name: String) = recordedVisits.record { it.preVisitArrayField(name) }
        override fun postVisitArrayField() = recordedVisits.record { it.postVisitArrayField() }
        override fun visitStringField(name: String, value: String) =
            recordedVisits.record { it.visitStringField(name, value) }

        override fun visitBooleanField(name: String, value: Boolean) =
            recordedVisits.record { it.visitBooleanField(name, value) }

        override fun visitNumberField(name: String, value: Number) =
            recordedVisits.record { it.visitNumberField(name, value) }

        override fun preVisitArray() = recordedVisits.record { it.preVisitArray() }
        override fun postVisitArray() = recordedVisits.record { it.postVisitArray() }
        override fun preVisitObject() = recordedVisits.record { it.preVisitObject() }
        override fun postVisitObject() = recordedVisits.record { it.postVisitObject() }
        override fun visitString(value: String) = recordedVisits.record { it.visitString(value) }
        override fun visitBoolean(value: Boolean) = recordedVisits.record { it.visitBoolean(value) }
        override fun visitNumber(value: Number) = recordedVisits.record { it.visitNumber(value) }

        private fun MutableList<(ModelState) -> Unit>.record(recording: (ModelState) -> Unit) {
            this.add(recording)
        }
    }
}

internal inline fun <reified T> Any.privatProp(fieldName: String): T =
    this::class.memberProperties.first { it.name == fieldName }.apply {
        isAccessible = true
    }.call(this) as T
