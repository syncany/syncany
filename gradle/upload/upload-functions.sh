SYNCANY_API_ENDPOINT="https://api.syncany.org/v3"
SYNCANY_API_ENDPOINT="http://tests.lan/syncany-website/api.syncany.org/html/v3"

if [ "$SYNCANY_API_KEY" == "" ]; then
	echo "ERROR: SYNCANY_API_KEY environment variable not set."
	exit 1
fi

urlencode() {
    # urlencode <string>

    local length="${#1}"
    for (( i = 0; i < length; i++ )); do
        local c="${1:i:1}"
        case $c in
            [a-zA-Z0-9._-]) printf "$c" ;;
            *) printf '%%%02X' "'$c"
        esac
    done
}

upload_file() {
	# upload_file <filename> <type> <request> <snapshot> <os> <arch>

	filename="$1"
	type="$2"
	request="$3"
	snapshot="$4"
	os="$5"
	arch="$6"

	if [ -z "$filename" -o -z "$type" -o -z "$request" -o -z "$snapshot" -o -z "$os" -o -z "$arch" ]; then
		echo "ERROR: Invalid arguments for upload_file. None of the arguments can be zero."
		exit 2
	fi

	if [ ! -f "$filename" ]; then
		echo "ERROR: Type '$type': File $filename does not exist. Skipping."
		exit 3
	fi
	
	basename=$(basename "$filename")
	basename_encoded=$(urlencode $basename)
	checksum=$(sha256sum "$filename" | awk '{ print $1 }')
		
	method="PUT"
	methodArgs="type=$type&filename=$basename_encoded&snapshot=$snapshot&os=$os&arch=$arch&checksum=$checksum"

	time=$(date +%s)
	rand=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 16 | head -n 1)

	protected="$method:$request:$methodArgs:$time:$rand"
	signature=$(echo -n "$protected" | openssl dgst -sha256 -hmac $SYNCANY_API_KEY | awk '{ print $2 }')

	curl -v -T "$filename" "$SYNCANY_API_ENDPOINT/$request?$methodArgs&time=$time&rand=$rand&signature=$signature"

	exit_code=$?

	if [ $exit_code -ne 0 ]; then
		echo "ERROR: Upload failed. curl exit code was $exit_code."
		exit 5
	fi
}

get_property() {
	# get_property <properties-file> <property-name>

	properties_file="$1"
	property_name="$2"

	cat "$properties_file" | grep "$property_name=" | sed -r 's/.+=//'
}

get_os_from_filename() {
	# get_os_from_filename <filename>

	filename="$1"

	if [ -n "$(echo $filename | grep linux)" ]; then
		echo "linux"
	elif [ -n "$(echo $filename | grep windows)" ]; then
		echo "windows"
	elif [ -n "$(echo $filename | grep macosx)" ]; then
		echo "macosx"
	else
		echo "all"
	fi
}

get_arch_from_filename() {
	# get_arch_from_filename <filename>

	filename="$1"

	if [ -n "$(echo $filename | grep x86_64)" -o -n "$(echo $filename | grep amd64)" ]; then
		echo "x86_64"
	elif [ -n "$(echo $filename | grep x86)" -o -n "$(echo $filename | grep i386)" ]; then
		echo "x86"
	else
		echo "all"
	fi
}

