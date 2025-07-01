from typing import Dict, Generator, List

from .cfg import get
from .checkpoint import load, save

_LIMIT = get("scan", "limit", 1000)
_BATCH = get("scan", "batch_size", 25)

def batches(table, reset_scan: bool = False) -> Generator[List[Dict], None, None]:
    cursor = load(reset_scan)
    while True:
        page = table.scan(
            Limit=_LIMIT,
            **({"ExclusiveStartKey": cursor} if cursor else {}),
        )
        items = page.get("Items", [])
        for i in range(0, len(items), _BATCH):
            yield items[i : i + _BATCH]

        cursor = page.get("LastEvaluatedKey")
        save(cursor)
        if not cursor:
            break