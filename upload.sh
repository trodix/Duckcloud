#!/bin/bash

main() {
	GO=$SECONDS
	START=0
	END=$2
	PATHFILE=$1

	echo $START
	echo $END
	echo $PATHFILE

	for (( i=$START; i<=$END; i++ ))
	do
		echo -e "\n======== $i ========\n"
		curl --location --request POST 'http://localhost:8010/upload' \
			--form "file=@$PATHFILE"
	done
	STOP=$SECONDS

	DURATION=$((STOP - GO))
	echo -e "\nDuration: $DURATION seconds\n"
}

main $@
