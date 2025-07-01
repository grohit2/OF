import csv
from pathlib import Path
from typing import Dict, List

FIELDS: List[str] = [
    "pk", "old_sk", "read",
    "new_sk_created", "new_sk_inserted", "new_sk_read",
    "old_sk_deleted", "processed_pk", "processed_sk",
]

def ensure_header(path: Path) -> None:
    if not path.exists():
        with path.open("w", newline="") as fh:
            csv.DictWriter(fh, FIELDS).writeheader()

def append(path: Path, row: Dict[str, str]) -> None:
    with path.open("a", newline="") as fh:
        csv.DictWriter(fh, FIELDS).writerow(row)