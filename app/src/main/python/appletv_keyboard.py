import asyncio
import contextlib
from dataclasses import dataclass
import logging
import time
from typing import Any

import pyatv
import pyatv.const

from appletv_remote import _build_configuration, run_coroutine

_LOGGER = logging.getLogger(__name__)

_ERR_KEYBOARD_FOCUS = "Error: Open a text field on Apple TV first, then try typing again."
_SESSION_TIMEOUT_SECONDS = 60.0


@dataclass(slots=True)
class _KeyboardSession:
    atv: Any
    conf: Any
    ip_address: str
    last_active: float

    async def close(self):
        with contextlib.suppress(Exception):
            self.atv.close()


_active_session: _KeyboardSession | None = None
_session_lock: asyncio.Lock | None = None


def _get_session_lock():
    global _session_lock
    if _session_lock is None:
        _session_lock = asyncio.Lock()
    return _session_lock


async def _close_session_internal():
    global _active_session
    session = _active_session
    _active_session = None
    if session is not None:
        await session.close()


async def _get_or_create_session(ip_address, credentials_json):
    global _active_session
    now = time.monotonic()
    if _active_session is not None:
        if _active_session.ip_address == ip_address and (now - _active_session.last_active) < _SESSION_TIMEOUT_SECONDS:
            _active_session.last_active = now
            return _active_session
        await _close_session_internal()

    conf = await _build_configuration(ip_address, credentials_json)
    atv = await pyatv.connect(conf, asyncio.get_running_loop())
    session = _KeyboardSession(
        atv=atv,
        conf=conf,
        ip_address=ip_address,
        last_active=now,
    )
    _active_session = session
    return session


async def _with_keyboard(ip_address, credentials_json, action, feature=None):
    lock = _get_session_lock()
    async with lock:
        try:
            session = await _get_or_create_session(ip_address, credentials_json)
            if session.atv.keyboard.text_focus_state == pyatv.const.KeyboardFocusState.Unfocused:
                await _close_session_internal()
                return _ERR_KEYBOARD_FOCUS
            if feature and not session.atv.features.in_state(pyatv.const.FeatureState.Available, feature):
                await _close_session_internal()
                return _ERR_KEYBOARD_FOCUS
            return await action(session.atv, session.conf)
        except Exception as ex:
            _LOGGER.exception("Keyboard command failed")
            await _close_session_internal()
            return f"Error: {ex}"


def _safe_run(coro):
    try:
        return run_coroutine(coro)
    except Exception as ex:
        return f"Error: {ex}"


def open_keyboard_session(ip_address="", credentials_json=""):
    async def open_session(atv, conf):
        return f"Success: Connected to {conf.name}"

    return _safe_run(
        _with_keyboard(
            ip_address,
            credentials_json,
            open_session,
        )
    )


def close_keyboard_session(ip_address=""):
    async def close_session():
        lock = _get_session_lock()
        async with lock:
            global _active_session
            if _active_session is not None and (not ip_address or _active_session.ip_address == ip_address):
                await _close_session_internal()
            return "Success: Keyboard session closed"

    return _safe_run(close_session())


def get_keyboard_text(ip_address="", credentials_json=""):
    async def get_text(atv, _conf):
        text = await atv.keyboard.text_get()
        return text or ""

    return _safe_run(
        _with_keyboard(
            ip_address,
            credentials_json,
            get_text,
            feature=pyatv.const.FeatureName.TextGet,
        )
    )


def set_keyboard_text(ip_address="", credentials_json="", text=""):
    async def set_text(atv, conf):
        await atv.keyboard.text_set(text)
        return f"Success: Updated text on {conf.name}"

    return _safe_run(
        _with_keyboard(
            ip_address,
            credentials_json,
            set_text,
            feature=pyatv.const.FeatureName.TextSet,
        )
    )


def clear_keyboard_text(ip_address="", credentials_json=""):
    async def clear_text(atv, conf):
        await atv.keyboard.text_clear()
        return f"Success: Cleared text on {conf.name}"

    return _safe_run(
        _with_keyboard(
            ip_address,
            credentials_json,
            clear_text,
            feature=pyatv.const.FeatureName.TextClear,
        )
    )


def submit_keyboard_text(ip_address="", credentials_json="", text=""):
    async def submit_text(atv, conf):
        await atv.keyboard.text_set(text)
        await atv.remote_control.select()
        return f"Success: Submitted text on {conf.name}"

    return _safe_run(
        _with_keyboard(
            ip_address,
            credentials_json,
            submit_text,
            feature=pyatv.const.FeatureName.TextSet,
        )
    )
