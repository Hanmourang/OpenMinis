#!/usr/bin/env python3
"""Minis desktop companion — advertise on LAN and run approved shell commands."""

from __future__ import annotations

import argparse
import json
import os
import platform
import secrets
import socket
import subprocess
import sys
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

DEFAULT_PORT = 18765
SERVICE_TYPE = "_minis-desktop._tcp.local."
APP_DIR = Path.home() / ".minis-desktop"
TOKEN_PATH = APP_DIR / "tokens.json"


def device_id() -> str:
    APP_DIR.mkdir(parents=True, exist_ok=True)
    path = APP_DIR / "device-id"
    if path.exists():
        return path.read_text().strip()
    value = str(uuid.uuid4())
    path.write_text(value)
    return value


def os_name() -> str:
    system = platform.system().lower()
    if system == "darwin":
        return "mac"
    if system.startswith("win"):
        return "windows"
    return "linux"


def hostname() -> str:
    host = socket.gethostname()
    return host if host.endswith(".local") else f"{host}.local"


def load_tokens() -> dict[str, Any]:
    if not TOKEN_PATH.exists():
        return {"tokens": {}}
    try:
        return json.loads(TOKEN_PATH.read_text())
    except json.JSONDecodeError:
        return {"tokens": {}}


def save_tokens(data: dict[str, Any]) -> None:
    APP_DIR.mkdir(parents=True, exist_ok=True)
    TOKEN_PATH.write_text(json.dumps(data, indent=2))


class CompanionState:
    def __init__(self, allow_discover: bool) -> None:
        self.allow_discover = allow_discover
        self.id = device_id()
        self.name = hostname()
        self.os = os_name()
        self.version = "1.0.0"
        self.lock = threading.Lock()

    def pair(self, client_id: str, client_name: str) -> dict[str, Any]:
        if not self.allow_discover:
            raise PermissionError("discovery disabled")
        token = secrets.token_urlsafe(24)
        data = load_tokens()
        data.setdefault("tokens", {})[token] = {
            "clientId": client_id,
            "clientName": client_name,
            "createdAt": time.time(),
        }
        save_tokens(data)
        return {
            "token": token,
            "deviceId": self.id,
            "name": self.name,
            "os": self.os,
        }

    def check(self, token: str) -> bool:
        return token in load_tokens().get("tokens", {})

    def unpair(self, token: str) -> None:
        data = load_tokens()
        data.get("tokens", {}).pop(token, None)
        save_tokens(data)

    def paired(self) -> bool:
        return bool(load_tokens().get("tokens"))


def run_command(command: str, timeout: int) -> dict[str, Any]:
    try:
        proc = subprocess.run(
            command,
            shell=True,
            capture_output=True,
            text=True,
            timeout=max(1, timeout),
        )
        return {
            "stdout": proc.stdout,
            "stderr": proc.stderr,
            "exitCode": proc.returncode,
        }
    except subprocess.TimeoutExpired as exc:
        return {
            "stdout": exc.stdout or "",
            "stderr": (exc.stderr or "") + "\n[timeout]",
            "exitCode": 124,
        }


class Handler(BaseHTTPRequestHandler):
    state: CompanionState

    def log_message(self, fmt: str, *args: Any) -> None:
        sys.stderr.write("minis-desktop: " + (fmt % args) + "\n")

    def _json(self, code: int, payload: dict[str, Any]) -> None:
        raw = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()
        self.wfile.write(raw)

    def _body(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            return {}
        raw = self.rfile.read(length)
        try:
            data = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            return {}
        return data if isinstance(data, dict) else {}

    def _token(self) -> str | None:
        header = self.headers.get("Authorization") or ""
        if header.lower().startswith("bearer "):
            return header[7:].strip()
        return None

    def do_OPTIONS(self) -> None:  # noqa: N802
        self._json(204, {})

    def do_GET(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        if path == "/v1/hello":
            self._json(200, {
                "id": self.state.id,
                "name": self.state.name,
                "os": self.state.os,
                "version": self.state.version,
                "allowDiscover": self.state.allow_discover,
                "paired": self.state.paired(),
            })
            return
        if path == "/v1/status":
            token = self._token()
            if not token or not self.state.check(token):
                self._json(401, {"error": "unauthorized"})
                return
            self._json(200, {
                "ok": True,
                "name": self.state.name,
                "os": self.state.os,
                "paired": True,
            })
            return
        self._json(404, {"error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        body = self._body()
        if path == "/v1/pair":
            try:
                result = self.state.pair(
                    str(body.get("clientId") or "unknown"),
                    str(body.get("clientName") or "Minis"),
                )
            except PermissionError as exc:
                self._json(403, {"error": str(exc)})
                return
            self._json(200, result)
            return
        token = self._token()
        if not token or not self.state.check(token):
            self._json(401, {"error": "unauthorized"})
            return
        if path == "/v1/unpair":
            self.state.unpair(token)
            self._json(200, {"ok": True})
            return
        if path == "/v1/exec":
            command = str(body.get("command") or "")
            timeout = int(body.get("timeout") or 60)
            if not command.strip():
                self._json(400, {"error": "missing command"})
                return
            self._json(200, run_command(command, timeout))
            return
        self._json(404, {"error": "not found"})


def advertise(port: int, state: CompanionState) -> Any:
    try:
        from zeroconf import ServiceInfo, Zeroconf
    except ImportError:
        sys.stderr.write("minis-desktop: zeroconf not installed; LAN browse disabled, use host:port\n")
        return None

    info = ServiceInfo(
        SERVICE_TYPE,
        f"{state.name}.{SERVICE_TYPE}",
        port=port,
        properties={
            "id": state.id,
            "name": state.name,
            "os": state.os,
            "ver": state.version,
            "pair": "1" if state.paired() else "0",
        },
        server=f"{state.name}.",
    )
    zc = Zeroconf()
    zc.register_service(info)
    return zc


def main() -> int:
    parser = argparse.ArgumentParser(description="Minis desktop companion")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--no-discover", action="store_true", help="Reject new pairing")
    args = parser.parse_args()

    state = CompanionState(allow_discover=not args.no_discover)
    Handler.state = state
    server = ThreadingHTTPServer(("0.0.0.0", args.port), Handler)
    zc = advertise(args.port, state)
    print(f"minis-desktop listening on 0.0.0.0:{args.port}", flush=True)
    print(f"  id={state.id} name={state.name} os={state.os}", flush=True)
    print("  allow discover:" + (" yes" if state.allow_discover else " no"), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        if zc is not None:
            zc.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
