import json
from pathlib import Path
from typing import Optional

from .cfg import get

_PATH = Path(get("scan", "checkpoint_path", ".dynamo_scan.ckpt.json"))

def save(cursor: Optional[dict]) -> None:
    if cursor:
        _PATH.write_text(json.dumps(cursor))
    else:
        _PATH.unlink(missing_ok=True)

def load(reset: bool = False) -> Optional[dict]:
    if reset:
        _PATH.unlink(missing_ok=True)
        return None
    try:
        return json.loads(_PATH.read_text())
    except FileNotFoundError:
        return None