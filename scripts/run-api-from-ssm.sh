#!/usr/bin/env bash
set +x
set -euo pipefail

if [[ $# -lt 3 || $2 != "--" ]]; then
  printf 'Usage: bash %s /toadstool/prod -- [docker run options] IMAGE [COMMAND...]\n' "$0" >&2
  exit 2
fi

prefix="${1%/}/"
shift 2
if [[ ! $prefix =~ ^/[A-Za-z0-9_./-]+/$ || $prefix == *"//"* ]]; then
  printf 'Use an absolute Parameter Store path, such as /toadstool/prod.\n' >&2
  exit 2
fi

for command in aws jq docker mktemp; do
  if ! command -v "$command" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$command" >&2
    exit 1
  fi
done

umask 077
temp_dir=$(mktemp -d "${TMPDIR:-/dev/shm}/toadstool-ssm.XXXXXX")
cleanup() {
  rm -f -- "$temp_dir/parameters.json" "$temp_dir/container.env"
  rmdir -- "$temp_dir"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

aws ssm get-parameters-by-path \
  --path "$prefix" \
  --recursive \
  --with-decryption \
  --region "${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}" \
  --output json \
  --no-cli-pager > "$temp_dir/parameters.json"

jq -er --arg prefix "$prefix" '
  if (.Parameters | type) != "array" or (.Parameters | length) == 0 then
    error("No parameters returned; check the path and AWS region.")
  else .Parameters end
  | map(
      if (.Name | startswith($prefix)) then
        {key: (.Name | ltrimstr($prefix)), value: .Value}
      else error("A parameter is outside the requested path.") end
    ) as $entries
  | if any($entries[];
      (.key | test("^[A-Za-z_][A-Za-z0-9_]*$") | not)
      or (.value | type) != "string"
      or (.value | test("[\r\n\u0000]"))) then
      error("Use direct-child environment variable names and single-line values without NUL characters.")
    elif ($entries | group_by(.key) | any(.[]; length > 1)) then
      error("Duplicate environment variable names.")
    else $entries end
  | . as $entries
  | (["DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD",
      "FAUXNANCE_BASE_URL", "FAUXNANCE_API_KEY", "FINNHUB_API_KEY"]
     - [$entries[] | select(.value != "") | .key]) as $missing
  | if ($missing | length) > 0 then
      error("Missing required parameters: " + ($missing | join(", ")))
    else $entries[] | "\(.key)=\(.value)" end
' "$temp_dir/parameters.json" > "$temp_dir/container.env"

docker run --env-file "$temp_dir/container.env" "$@"
