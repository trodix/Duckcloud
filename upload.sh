#!/bin/bash

main() {
	GO=$SECONDS
	START=0
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
			--form 'directoryPath="/dir1"' \
			--form 'aspects="app-doc:fruit, app-doc:fish"' \
			--form 'properties[app-doc:fruitName]="banana"' \
			--form 'properties[app-doc:fishName]="salmon"'
	done
	STOP=$SECONDS

	DURATION=$((STOP - GO))
	echo -e "\nDuration: $DURATION seconds\n"
}

main "$@"
