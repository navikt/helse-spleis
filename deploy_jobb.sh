#!/usr/bin/env sh

read -p "🐳 Image: " IMAGE
read -p "☸️ Cluster (1: dev-gcp | 2: prod-gcp): " CLUSTER_OPT
read -s -p "🔑 API key: " APIKEY
echo ""
read -p "🔑 Parallelism: (30 default) " PARALLELISM

read -p "🛠️ Hvilken jobb skal du kjøre? " JOBB
read -p "🪪 Hva skal arbeidId settes til? " ARBEID_ID
read -p "🏜️ Dryrun? (Y/n): " DRYRUN
read -p "🎒 Eventuelt andre parametre til jobben? " EKSTRA_PARAMETRE

if [ "$CLUSTER_OPT" = "1" ]; then
  CLUSTER="dev-gcp"
elif [ "$CLUSTER_OPT" = "2" ]; then
  CLUSTER="prod-gcp"
else
  echo "Ugyldig valg. Avslutter."
  exit 1
fi

if [ -z "$PARALLELISM" ]; then
  PARALLELISM=30
fi

POOL="nav-dev"
DATABASE="helse-spleis"
DATABASE_AVSTEMMING_SECRET="google-sql-helse-spleis-spleis-spleis-avstemming-038f9535"
DATABASE_MIGRATE_SECRET="google-sql-helse-spleis-spleis-spleis-migrate-70ddf3c6"

if [ "$CLUSTER" = "prod-gcp" ]; then
  POOL="nav-prod"
  DATABASE="spleis2"
  DATABASE_AVSTEMMING_SECRET="google-sql-spleis-spleis-spleis-avstemming-c09f6283"
  DATABASE_MIGRATE_SECRET="google-sql-spleis-spleis-spleis-migrate-dc6feb9b"
fi

if [[ -z $DRYRUN || "$DRYRUN" =~ ^(yes|y|true|1|YES|Y|TRUE)$ ]]; then
  DRYRUN="true"
else
  DRYRUN="false"
fi


docker run --rm -it -v $(PWD)/deploy:/config \
  -e CLUSTER=$CLUSTER \
  -e VAR="team=tbd,app=spleis-migrate,image=$IMAGE,database=$DATABASE,avstemming-secret=$DATABASE_AVSTEMMING_SECRET,migrate-secret=$DATABASE_MIGRATE_SECRET,parallelism=$PARALLELISM,pool=$POOL,jobb=$JOBB,arbeid_id=$ARBEID_ID,dryrun=$DRYRUN,ekstra_parametre=$EKSTRA_PARAMETRE" \
  -e APIKEY="$APIKEY" \
  ghcr.io/nais/deploy/deploy:latest /app/deploy \
    --print-payload --resource /config/job.yml

echo "Når jobben er ferdig, husk å kjøre"
echo "  kubectl delete naisjob spleis-migrate"
