import logging

import pyatv
import pyatv.const

from appletv_remote import _build_configuration, _connect_apple_tv, run_coroutine

_LOGGER = logging.getLogger(__name__)

_ERR_KEYBOARD_FOCUS = "Error: Open a text field on Apple TV first, then try typing again."


async def _with_keyboard(ip_address, credentials_json, action, feature=None):
    conf, error = await _build_configuration(ip_address, credentials_json)
    if error:
        return error

    async with _connect_apple_tv(conf) as atv:
        try:
            if atv.keyboard.text_focus_state == pyatv.const.KeyboardFocusState.Unfocused:
                return _ERR_KEYBOARD_FOCUS
            if feature and not atv.features.in_state(pyatv.const.FeatureState.Available, feature):
                return _ERR_KEYBOARD_FOCUS
            return await action(atv, conf)
        except Exception as ex:
            _LOGGER.exception("Keyboard command failed")
            return f"Error: {ex}"


def _safe_run(coro):
    try:
        return run_coroutine(coro)
    except Exception as ex:
        return f"Error: {ex}"


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

