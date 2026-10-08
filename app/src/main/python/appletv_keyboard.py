import asyncio
import logging

import pyatv
import pyatv.const

from appletv_remote import _build_configuration, run_coroutine

_LOGGER = logging.getLogger(__name__)


def _keyboard_focus_error():
    return "Error: Open a text field on Apple TV first, then try typing again."


async def _with_keyboard(ip_address, credentials_json, action):
    conf, error = await _build_configuration(ip_address, credentials_json)
    if error:
        return error

    atv = await pyatv.connect(conf, asyncio.get_running_loop())

    try:
        focus_state = atv.keyboard.text_focus_state
        if focus_state == pyatv.const.KeyboardFocusState.Unfocused:
            return _keyboard_focus_error()

        return await action(atv, conf)
    except Exception as ex:
        _LOGGER.exception("Keyboard command failed")
        return f"Error: {ex}"
    finally:
        atv.close()


async def _async_get_keyboard_text(ip_address, credentials_json=""):
    async def get_text(atv, _conf):
        if not atv.features.in_state(
            pyatv.const.FeatureState.Available, pyatv.const.FeatureName.TextGet
        ):
            return _keyboard_focus_error()

        text = await atv.keyboard.text_get()
        return text or ""

    return await _with_keyboard(ip_address, credentials_json, get_text)


async def _async_set_keyboard_text(ip_address, credentials_json="", text=""):
    async def set_text(atv, conf):
        if not atv.features.in_state(
            pyatv.const.FeatureState.Available, pyatv.const.FeatureName.TextSet
        ):
            return _keyboard_focus_error()

        await atv.keyboard.text_set(text)
        return f"Success: Updated text on {conf.name}"

    return await _with_keyboard(ip_address, credentials_json, set_text)


async def _async_clear_keyboard_text(ip_address, credentials_json=""):
    async def clear_text(atv, conf):
        if not atv.features.in_state(
            pyatv.const.FeatureState.Available, pyatv.const.FeatureName.TextClear
        ):
            return _keyboard_focus_error()

        await atv.keyboard.text_clear()
        return f"Success: Cleared text on {conf.name}"

    return await _with_keyboard(ip_address, credentials_json, clear_text)


async def _async_submit_keyboard_text(ip_address, credentials_json="", text=""):
    async def submit_text(atv, conf):
        if not atv.features.in_state(
            pyatv.const.FeatureState.Available, pyatv.const.FeatureName.TextSet
        ):
            return _keyboard_focus_error()

        await atv.keyboard.text_set(text)
        await atv.remote_control.select()
        return f"Success: Submitted text on {conf.name}"

    return await _with_keyboard(ip_address, credentials_json, submit_text)


def get_keyboard_text(ip_address="", credentials_json=""):
    try:
        return run_coroutine(_async_get_keyboard_text(ip_address, credentials_json))
    except Exception as ex:
        return f"Error: {ex}"


def set_keyboard_text(ip_address="", credentials_json="", text=""):
    try:
        return run_coroutine(_async_set_keyboard_text(ip_address, credentials_json, text))
    except Exception as ex:
        return f"Error: {ex}"


def clear_keyboard_text(ip_address="", credentials_json=""):
    try:
        return run_coroutine(_async_clear_keyboard_text(ip_address, credentials_json))
    except Exception as ex:
        return f"Error: {ex}"


def submit_keyboard_text(ip_address="", credentials_json="", text=""):
    try:
        return run_coroutine(_async_submit_keyboard_text(ip_address, credentials_json, text))
    except Exception as ex:
        return f"Error: {ex}"
