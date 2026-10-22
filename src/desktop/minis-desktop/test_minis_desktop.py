#!/usr/bin/env python3
"""Protocol smoke tests against minis_desktop.py (stdlib only)."""

from __future__ import annotations

import json
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def wait_port(port: int, timeout: float = 5.0) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.2):
                return
        except OSError:
            time.sleep(0.05)
    raise RuntimeError(f"server did not start on {port}")


def http(method: str, url: str, body: dict | None = None, token: str | None = None) -> tuple[int, dict]:
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw or "{}")
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        try:
            payload = json.loads(raw or "{}")
        except json.JSONDecodeError:
            payload = {"error": raw}
        return exc.code, payload


def main() -> int:
    sock = socket.socket()
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
    sock.close()

    proc = subprocess.Popen(
        [sys.executable, str(ROOT / "minis_desktop.py"), "--port", str(port)],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    try:
        wait_port(port)
        code, hello = http("GET", f"http://127.0.0.1:{port}/v1/hello")
        assert code == 200, hello
        assert hello["os"] in {"mac", "linux", "windows"}
        assert hello["allowDiscover"] is True

        code, pair = http("POST", f"http://127.0.0.1:{port}/v1/pair", {
            "clientId": "test",
            "clientName": "pytest",
        })
        assert code == 200, pair
        token = pair["token"]
        assert token

        code, status = http("GET", f"http://127.0.0.1:{port}/v1/status", token=token)
        assert code == 200 and status.get("ok") is True

        code, denied = http("GET", f"http://127.0.0.1:{port}/v1/status")
        assert code == 401

        code, exe = http("POST", f"http://127.0.0.1:{port}/v1/exec", {
            "command": "echo minis-desktop-ok",
            "timeout": 5,
        }, token=token)
        assert code == 200, exe
        assert exe["exitCode"] == 0
        assert "minis-desktop-ok" in exe["stdout"]
        print("ok")
        return 0
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()


if __name__ == "__main__":
    raise SystemExit(main())
