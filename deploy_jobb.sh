#!/usr/bin/env sh
read -p "🐳 Image: " IMAGE
read -e -p $'☸️ Cluster\n1 for dev-gcp\n2 for prod-gcp\n☸️ : ' CLUSTER_OPT
read -s -p "🔑 API key: " APIKEY
echo ""
read -p "🔑 Parallelism: (30 default) " PARALLELISM

read -e -p $'🛠️ Hvilken jobb skal du kjøre?\n1 for feriepenger\nEller skriv navnet på jobben\n🛠: ' JOBB_OPT
read -p "🪪 Hva skal arbeidId settes til? " ARBEID_ID
read -p "🏜️ Dryrun? (Y/n): " DRYRUN
read -p "🎒 Eventuelt andre parametre til jobben? " EKSTRA_PARAMETRE

if [ "$CLUSTER_OPT" = "1" ]; then
  VARS_FILE="/config/job-dev.json"
  CLUSTER="dev-gcp"
elif [ "$CLUSTER_OPT" = "2" ]; then
  CLUSTER="prod-gcp"
  VARS_FILE="/config/job-prod.json"
else
  echo "Ugyldig valg av cluster. Avslutter."
  exit 1
fi

if [ "$JOBB_OPT" = "1" ]; then
  JOBB="feriepenger"
else
  JOBB="$JOBB_OPT"
fi

if [ -z "$PARALLELISM" ]; then
  PARALLELISM=30
fi

if [[ -z $DRYRUN || "$DRYRUN" =~ ^(yes|y|true|1|YES|Y|TRUE)$ ]]; then
  DRYRUN="true"
else
  DRYRUN="false"
fi

cat <<EOF
Kjøre jobb med disse parameterene?
Image: $IMAGE
Parallelism: $PARALLELISM
Jobb: $JOBB
Arbeid_id: $ARBEID_ID
Dryrun: $DRYRUN
Ekstra_parametre: $EKSTRA_PARAMETRE"
EOF

docker run --rm -it -v $(PWD)/deploy:/config \
  -e CLUSTER=$CLUSTER \
  -e VAR="team=tbd,app=spleis-migrate,image=$IMAGE,parallelism=$PARALLELISM,jobb=$JOBB,arbeid_id=$ARBEID_ID,dryrun=$DRYRUN,ekstra_parametre=$EKSTRA_PARAMETRE" \
  -e VARS=$VARS_FILE \
  -e APIKEY="$APIKEY" \
  ghcr.io/nais/deploy/deploy:latest /app/deploy \
    --print-payload --resource /config/job.yml

echo "Når jobben er ferdig, husk å kjøre"
echo "  kubectl delete naisjob spleis-migrate"
