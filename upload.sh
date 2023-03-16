#!/bin/bash

main() {
	GO=$SECONDS
	START=1
	END=$2
	PATHFILE=$1

	echo "$START"
	echo "$END"
	echo "$PATHFILE"

	for (( i="$START"; i<="$END"; i++ ))
	do
		echo -e "\n======== $i ========\n"
		curl --location --request POST 'http://localhost:8010/upload' \
			--form "file=@$PATHFILE" \
			--form "directoryPath=/fruits/apple/red" \
			--form "aspects=app-doc:fruit, app-doc:fish" \
			--form "properties[fruit:name]=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 13 ; echo '')" \
			--form "properties[fruit:weight]=$(shuf -i 10-9999 -n 1)" \
			--form "properties[fruit:harvest-date]=$(date -I -d "2020-01-01 +$(shuf -i 0-1100 -n 1) days")" \
			--form "properties[fruit:reference]=$(shuf -i 1000000-9999999 -n 1)"
	done
	STOP=$SECONDS

	DURATION=$((STOP - GO))
	echo -e "\nDuration: $DURATION seconds\n"
}

main "$@"
