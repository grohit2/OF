# Dynamo Seq Migrator

A production‑style project skeleton that migrates DynamoDB sort keys
from 11‑digit (or “H‑prefixed” negative) sequences to 13‑digit positive
sequences. Configuration lives in **config.yaml**; code is self‑contained
and import‑free outside the package itself.

## Quick start

```bash
pip install -r requirements.txt
python cli.py --table MyTable
```

See the inline doc‑strings and the `config.yaml` comments for details.