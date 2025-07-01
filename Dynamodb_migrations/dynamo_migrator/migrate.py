import logging
from decimal import Decimal
from pathlib import Path
from typing import Dict, Tuple

import boto3
from botocore.exceptions import ClientError
from tqdm import tqdm

from .cfg import get
from .csv_audit import FIELDS as AUDIT_FIELDS, append as csv_append, ensure_header
from .dynamo_scan import batches
from .orderkey import build_new

def _migrate_item(table, item: Dict, pk: str, sk: str) -> Tuple[Dict[str, str], bool]:
    aud = {k: "" for k in AUDIT_FIELDS}
    pk_val, old_sk = item[pk], item[sk]
    aud.update(pk=pk_val, old_sk=old_sk, read="yes")

    try:
        new_sk, seq = build_new(old_sk)
        aud["new_sk_created"] = "yes"
    except ValueError:
        aud["new_sk_created"] = "format_error"
        return aud, False

    clone = dict(item)
    clone[sk] = new_sk
    clone["postedTransactionSequenceNumber"] = Decimal(str(seq))

    try:
        table.put_item(
            Item=clone,
            ConditionExpression="attribute_not_exists(#p) AND attribute_not_exists(#s)",
            ExpressionAttributeNames={"#p": pk, "#s": sk},
        )
        aud["new_sk_inserted"] = "yes"
    except ClientError as e:
        code = e.response["Error"]["Code"]
        aud["new_sk_inserted"] = "duplicate" if code == "ConditionalCheckFailedException" else f"error:{code}"
        return aud, False

    try:
        resp = table.get_item(Key={pk: pk_val, sk: new_sk})
        ok = "Item" in resp and int(resp["Item"]["postedTransactionSequenceNumber"]) == seq
        aud["new_sk_read"] = "yes" if ok else "seq_mismatch"
        if not ok:
            return aud, False
    except ClientError as e:
        aud["new_sk_read"] = f"error:{e.response['Error']['Code']}"
        return aud, False

    try:
        table.delete_item(Key={pk: pk_val, sk: old_sk})
        aud.update(old_sk_deleted="yes", processed_pk=pk_val, processed_sk=new_sk)
    except ClientError as e:
        aud["old_sk_deleted"] = f"error:{e.response['Error']['Code']}"
        return aud, False

    return aud, True


def run(
    table_name: str,
    csv_path: Path,
    pk_attr: str,
    sk_attr: str,
    region: str,
    profile: str | None,
    reset_scan: bool,
) -> None:
    session = boto3.Session(profile_name=profile, region_name=region)
    table = session.resource("dynamodb").Table(table_name)

    ensure_header(csv_path)
    total = success = failure = 0
    for batch in tqdm(batches(table, reset_scan), desc="Migrating", unit="batch"):
        for item in batch:
            row, ok = _migrate_item(table, item, pk_attr, sk_attr)
            csv_append(csv_path, row)
            total += 1
            success += ok
            failure += not ok

    logging.info(
        "Finished %d items | ✓ %d | ✗ %d | audit: %s",
        total,
        success,
        failure,
        csv_path,
    )