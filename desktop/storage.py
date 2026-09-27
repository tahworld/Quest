"""Local Windows test data. Each operation owns its SQLite connection."""

import json
import os
from pathlib import Path
import sqlite3
import time
from contextlib import contextmanager


def data_path():
    base = Path(os.environ.get("LOCALAPPDATA", Path.home() / ".quest")) / "Quest"
    base.mkdir(parents=True, exist_ok=True)
    return base / "quest-desktop.db"


class Store:
    def __init__(self, path=None):
        self.path = str(path or data_path())
        with self.connect() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS direction (
                    id INTEGER PRIMARY KEY CHECK(id = 1), name TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS quest (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    direction TEXT NOT NULL, intention TEXT NOT NULL,
                    conditions TEXT NOT NULL, draft TEXT NOT NULL,
                    status TEXT NOT NULL, created_at INTEGER NOT NULL,
                    result_text TEXT, completed_at INTEGER
                );
                CREATE TABLE IF NOT EXISTS rejection (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    quest_id INTEGER NOT NULL, reason TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
            """)

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.path)
        try:
            with db:
                yield db
        finally:
            db.close()

    def direction(self):
        with self.connect() as db:
            row = db.execute("SELECT name FROM direction WHERE id=1").fetchone()
        return row[0] if row else ""

    def save_direction(self, name):
        with self.connect() as db:
            db.execute("INSERT INTO direction(id,name) VALUES(1,?) "
                       "ON CONFLICT(id) DO UPDATE SET name=excluded.name", (name.strip(),))

    def create_quest(self, direction, intention, conditions, draft):
        with self.connect() as db:
            cursor = db.execute(
                "INSERT INTO quest(direction,intention,conditions,draft,status,created_at) "
                "VALUES(?,?,?,?,?,?)",
                (direction, intention, json.dumps(conditions, ensure_ascii=False),
                 json.dumps(draft, ensure_ascii=False), "PENDING", int(time.time())),
            )
            return cursor.lastrowid

    def quest(self, quest_id):
        with self.connect() as db:
            db.row_factory = sqlite3.Row
            row = db.execute("SELECT * FROM quest WHERE id=?", (quest_id,)).fetchone()
        if row is None:
            return None
        result = dict(row)
        result["conditions"] = json.loads(result["conditions"])
        result["draft"] = json.loads(result["draft"])
        return result

    def latest_active(self):
        with self.connect() as db:
            row = db.execute("SELECT id FROM quest WHERE status='ACTIVE' "
                             "ORDER BY id DESC LIMIT 1").fetchone()
        return self.quest(row[0]) if row else None

    def recent(self, limit=5):
        with self.connect() as db:
            rows = db.execute("SELECT id FROM quest ORDER BY id DESC LIMIT ?", (limit,)).fetchall()
        return [self.quest(row[0]) for row in rows]

    def transition(self, quest_id, old_status, new_status, result=None):
        with self.connect() as db:
            cursor = db.execute(
                "UPDATE quest SET status=?, result_text=?, completed_at=? "
                "WHERE id=? AND status=?",
                (new_status, result, int(time.time()) if new_status == "COMPLETED" else None,
                 quest_id, old_status),
            )
            return cursor.rowcount == 1

    def reject(self, quest_id, reason):
        with self.connect() as db:
            cursor = db.execute("UPDATE quest SET status='ABANDONED' "
                                "WHERE id=? AND status='PENDING'", (quest_id,))
            if cursor.rowcount != 1:
                return False
            db.execute("INSERT INTO rejection(quest_id,reason,created_at) VALUES(?,?,?)",
                       (quest_id, reason, int(time.time())))
            return True

    def recent_rejections(self, limit=3):
        with self.connect() as db:
            rows = db.execute("SELECT q.draft,r.reason FROM rejection r "
                              "JOIN quest q ON q.id=r.quest_id ORDER BY r.id DESC LIMIT ?",
                              (limit,)).fetchall()
        return [{"title": json.loads(draft)["title"], "reason": reason}
                for draft, reason in rows]
