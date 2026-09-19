"""Runs SQL against a database pulled with pull-db.sh and prints the result.

    py query-db.py <path/to/user_data.db>              # overview of user data
    py query-db.py <path/to/user_data.db> "<SQL>"      # arbitrary query

Use `py` on Windows, `python3` elsewhere. Python 3.7 compatible (standard
library only). If the local SQLite lacks JSON1 (e.g. Python 3.7 on Windows),
simple json_extract(json, '$.a.b[0]') and json_array_length(json[, path])
fallbacks are registered so game_state_json can still be queried. Long values
are cut to --width characters (default 300, 0 = no limit) so large JSON
columns do not flood the context.
"""
import argparse
import json
import re
import sqlite3
import sys

OVERVIEW = [
    ("tables", "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name"),
    ("completed_levels", "SELECT * FROM completed_levels ORDER BY game_mode_id, level_index"),
    ("game_state",
     "SELECT game_mode_id, level, datetime(updated_at / 1000, 'unixepoch') AS updated_utc, "
     "length(game_state_json) AS json_chars, game_state_json FROM game_state "
     "ORDER BY updated_at DESC"),
]


def _json_path(value, path):
    node = json.loads(value)
    for key, index in re.findall(r"\.([^.\[]+)|\[(\d+)\]", path.lstrip("$")):
        if node is None:
            return None
        node = node.get(key) if key else (node[int(index)] if int(index) < len(node) else None)
    return node


def _json_extract(value, path):
    node = _json_path(value, path)
    return json.dumps(node) if isinstance(node, (dict, list)) else node


def _json_array_length(value, path="$"):
    node = _json_path(value, path)
    return len(node) if isinstance(node, list) else None


def add_json_fallback(connection):
    try:
        connection.execute("SELECT json_extract('{}', '$')")
    except sqlite3.OperationalError:
        connection.create_function("json_extract", 2, _json_extract)
        connection.create_function("json_array_length", 1, _json_array_length)
        connection.create_function("json_array_length", 2, _json_array_length)


def print_rows(cursor, width):
    columns = [d[0] for d in cursor.description or []]
    rows = cursor.fetchall()
    if not columns:
        print("(no result set)")
        return
    print("\t".join(columns))
    for row in rows:
        cells = []
        for value in row:
            text = "NULL" if value is None else str(value)
            if width and len(text) > width:
                text = text[:width] + "...(+%d chars)" % (len(text) - width)
            cells.append(text)
        print("\t".join(cells))
    print("(%d row%s)" % (len(rows), "" if len(rows) == 1 else "s"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("db")
    parser.add_argument("sql", nargs="?")
    parser.add_argument("--width", type=int, default=300)
    args = parser.parse_args()

    connection = sqlite3.connect(args.db)
    add_json_fallback(connection)
    try:
        if args.sql:
            print_rows(connection.execute(args.sql), args.width)
        else:
            for title, sql in OVERVIEW:
                print("== %s" % title)
                try:
                    print_rows(connection.execute(sql), args.width)
                except sqlite3.Error as error:
                    print("error: %s" % error)
                print()
        connection.commit()
    except sqlite3.Error as error:
        print("error: %s" % error, file=sys.stderr)
        sys.exit(1)
    finally:
        connection.close()


if __name__ == "__main__":
    main()
