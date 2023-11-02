#!/bin/bash

SOURCE_PATH="/tmp/pg_hba.conf"
DEST_PATH="/var/lib/postgresql/data/pg_hba.conf"
mv -f "$SOURCE_PATH" "$DEST_PATH"