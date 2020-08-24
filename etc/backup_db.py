#!/usr/bin/env python3

from datetime import datetime
import os.path
import sys

import boto3


def backup_dir(db_dir):
    return db_dir + '-BACKUP'

def day_abbrev():
    return datetime.now().strftime('%Y%m%d').lower()


def weekday_abbrev():
    return datetime.now().strftime('%a').lower()


def warn(txt):
    print("WARNING: {}!".format(txt), file=sys.stderr, flush=True)


def daily_backup(db_dir):
    bak_dir = backup_dir(db_dir)
    if os.system(f"rm -rf {bak_dir}"
                 + f" && mkdir -p {bak_dir}/db"
                 + f" && mkdir -p {bak_dir}/event-log"
                 + f" && mdb_copy {db_dir}/db {bak_dir}/db"
                 + f" && mdb_copy {db_dir}/event-log {bak_dir}/event-log"):
        warn("Unable to create local backup.")
        return
    upload_current_backup(db_dir, 'daily', weekday_abbrev())


def weekly_backup(db_dir):
    # This assumes that the daily backup has already been performed today.
    upload_current_backup(db_dir, 'weekly', day_abbrev())


def upload_current_backup(db_dir, directory, name):
    tar_name = f"{name}.tar.xz"
    backup_base = os.path.basename(backup_dir(db_dir))
    backup_name = os.path.dirname(backup_dir(db_dir))
    cmd = f"tar -cJvf {tar_name} {backup_base} -C {backup_name}"
    print(cmd)
    retcode = os.system(f"tar -C {backup_name} -cJvf {tar_name} {backup_base}")
    if retcode != 0:
        warn("Couldn't tar weekly backup")
        return
    s3 = boto3.client('s3')
    print("Uploading...")
    s3.upload_file(tar_name, 'relembra-crux', f'{directory}/{tar_name}')
    os.unlink(tar_name)
    print("Done.")


if __name__ == '__main__':
    _, freq, db_dir = sys.argv
    if freq == 'daily':
        daily_backup(db_dir)
    elif freq == 'weekly':
        weekly_backup(db_dir)
    else:
        assert False
