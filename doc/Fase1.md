```mermaid
classDiagram

    jobs-->sykepenger-model

    sykepenger-api-->sykepenger-model
    sykepenger-api-->sykepenger-mediators

    sykepenger-etterlevelse-->sykepenger-etterlevelse-api

    sykepenger-mediators-->sykepenger-model

    sykepenger-model-->sykepenger-primitiver
    sykepenger-model-->sykepenger-utbetaling
    sykepenger-model-->sykepenger-aktivitetslogg
    sykepenger-model-->sykepenger-etterlevelse

    sykepenger-opprydding-dev-->sykepenger-utbetaling
    sykepenger-opprydding-dev-->sykepenger-mediators

    sykepenger-primitiver-->sykepenger-etterlevelse-api

    sykepenger-utbetaling-->sykepenger-etterlevelse-api
    sykepenger-utbetaling-->sykepenger-primitiver
    sykepenger-utbetaling-->sykepenger-aktivitetslogg

```