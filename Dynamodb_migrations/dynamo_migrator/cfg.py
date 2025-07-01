from __future__ import annotations

import logging
from pathlib import Path
from typing import Any, Dict

import yaml

_CFG_PATH = Path(__file__).with_suffix(".yaml").parent.parent / "config.yaml"


def _load_yaml(path: Path) -> Dict[str, Any]:
    with path.open() as fh:
        return yaml.safe_load(fh)


_config = _load_yaml(_CFG_PATH)


def get(section: str, key: str, default=None):
    """Fetch `section.key` from config.yaml (dot‑style)."""
    return _config.get(section, {}).get(key, default)


def configure_logging() -> None:
    lvl = get("logging", "level", "INFO").upper()
    logging.basicConfig(
        level=lvl,
        format="%(asctime)s %(levelname)s %(message)s",
        datefmt="%H:%M:%S",
    )