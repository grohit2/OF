import argparse
import logging
import sys
from pathlib import Path

from dynamo_migrator import cfg, migrate

def _parse() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Dynamo orderKey 11→13 digit migrator")
    p.add_argument("--table", required=True)
    p.add_argument("--pk", default="pk")
    p.add_argument("--sk", default="sk")
    p.add_argument("--csv", default=cfg.get("csv", "path"))
    p.add_argument("--profile", default=cfg.get("aws", "profile"))
    p.add_argument("--region", default=cfg.get("aws", "region"))
    p.add_argument("--reset-scan", action="store_true", help="ignore saved cursor")
    p.add_argument("-v", "--verbose", action="store_true")
    return p.parse_args()

def main() -> None:
    args = _parse()
    cfg.configure_logging()
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    try:
        migrate.run(
            table_name=args.table,
            csv_path=Path(args.csv),
            pk_attr=args.pk,
            sk_attr=args.sk,
            region=args.region,
            profile=args.profile,
            reset_scan=args.reset_scan,
        )
    except KeyboardInterrupt:
        logging.warning("Interrupted – checkpoint saved.")
        sys.exit(130)

if __name__ == "__main__":
    main()