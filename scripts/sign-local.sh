#!/bin/sh
# Uses the local signing material without printing secrets or placing them in CLI arguments.
set -eu
cd "$(dirname "$0")/.."
python3 - "$@" <<'PY'
import os, subprocess, sys
from pathlib import Path
root = Path(os.environ.get('CHRISTMAS_SIGNING_DIR', str(Path.home()/'.config/christmas-code/signing')))
env = os.environ.copy()
env['PRIVATE_KEY'] = (root/'private.pem').read_text()
env['CERTIFICATE_CHAIN'] = (root/'chain.crt').read_text()
env['PRIVATE_KEY_PASSWORD'] = (root/'password').read_text().strip()
subprocess.run(['./dev.sh', 'signPlugin', 'verifyPluginSignature', *sys.argv[1:]], env=env, check=True)
PY
