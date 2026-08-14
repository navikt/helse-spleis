---
applyTo: "**/*River*.kt"
---

# Rapids & Rivers — River-mønstre i spleis

## ⚠️ Evig løkke: manglende `forbid("@løsning")` på behov-rivers

Dette er en av de vanligste fellene i Rapids & Rivers-kode, og den er vanskelig å oppdage fordi alt ser riktig ut — helt til det kjører.

**Hva skjer:** Når en River lytter på `behov` og svarer ved å legge `@løsning` på samme melding og republisere den, vil riveren plukke opp sin egen melding på nytt — fordi den fortsatt matcher precondition. Resultatet er en evig løkke som spiser Kafka-offset og potensielt overbelaster systemet.

**Løsningen er enkel:** Legg alltid til `it.forbid("@løsning")` i precondition på Rivers som publiserer løsninger.

### ✅ Riktig

```kotlin
River(rapidsConnection).apply {
    precondition {
        it.requireValue("@event_name", "behov")
        it.requireAllOrAny("@behov", listOf("MittBehov"))
        it.forbid("@løsning")   // ← stopper riveren fra å behandle sin egen løsning
    }
    validate { it.requireKey("fødselsnummer") }
}.register(this)
```

### ❌ Feil — vil gi evig løkke i produksjon

```kotlin
River(rapidsConnection).apply {
    precondition {
        it.requireValue("@event_name", "behov")
        it.requireAllOrAny("@behov", listOf("MittBehov"))
        // mangler forbid("@løsning") — riveren vil plukke opp sin egen løsning
        // og svare på nytt, i det uendelige
    }
}.register(this)
```

### Unntaket

Rivers som sender et **nytt behov med nytt `@id`** i stedet for å svare med `@løsning` på den innkommende meldingen trenger ikke `forbid("@løsning")`. Da er det ikke den samme meldingen som kommer tilbake.

