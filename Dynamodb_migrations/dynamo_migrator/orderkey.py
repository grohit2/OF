import re
from typing import NamedTuple, Optional, Tuple

class ParsedKey(NamedTuple):
    date: str
    raw_seq: str
    seq_num: int
    event_id: str


_RE = re.compile(r"(?P<date>\d{4}-\d{2}-\d{2})(?P<seq>H?\d{10,11})_(?P<ev>\d{6})$")

def _decode(raw: str) -> int:
    return int(raw[1:]) - 10_000_000_000_000

def parse(key: str) -> Optional[ParsedKey]:
    m = _RE.fullmatch(key)
    if not m:
        return None
    raw = m["seq"]
    num = _decode(raw) if raw.startswith("H") else int(raw)
    return ParsedKey(m["date"], raw, num, m["ev"])

def _enc(num: int) -> str:
    """Pad to 13 digits, prefix 'H' for negative."""
    return str(num).zfill(13) if num >= 0 else "H" + str(10_000_000_000_000 + num).zfill(13)

def build_new(old: str) -> Tuple[str, int]:
    p = parse(old)
    if not p:
        raise ValueError(f"bad orderKey {old}")
    return f"{p.date}{_enc(p.seq_num)}_{p.event_id}", p.seq_num