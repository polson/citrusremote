import asyncio
import contextlib
from dataclasses import dataclass
import inspect
import ipaddress
import json
import logging
import threading
from typing import Any
import warnings

import pyatv
import pyatv.conf
import pyatv.const

logging.basicConfig(level=logging.INFO, format="%(name)s - %(levelname)s - %(message)s")
logging.getLogger("pyatv").setLevel(logging.WARNING)
warnings.filterwarnings(
    "ignore",
    message=r"Call to deprecated function volume_(up|down)\.",
    category=DeprecationWarning,
)
_LOGGER = logging.getLogger(__name__)

_loop = None
_CREDENTIALS_VERSION = 2
_PRIMARY_PROTOCOL = pyatv.const.Protocol.Companion
_DEFAULT_UNMUTE_VOLUME = 50.0
_last_known_volume: dict[str, float] = {}
_LEGACY_MOCK_CREDENTIAL_PREFIX = "TEST_MOCK_CREDENTIALS_"
_MOCK_DEVICE_IP = "10.0.2.2"
_MUTE_VERIFY_DELAY_SECONDS = 0.25
_MUTE_ZERO_THRESHOLD = 1.0


_MODEL_MAP = {
    "Gen1": "Apple TV (1st Gen)",
    "AppleTVGen1": "Apple TV (1st Gen)",
    "Gen2": "Apple TV (2nd Gen)",
    "AppleTVGen2": "Apple TV (2nd Gen)",
    "Gen3": "Apple TV (3rd Gen)",
    "Gen4": "Apple TV HD",
    "Gen4K": "Apple TV 4K (1st Gen)",
    "AppleTV4KGen2": "Apple TV 4K (2nd Gen)",
    "Gen4KGen2": "Apple TV 4K (2nd Gen)",
    "AppleTV4KGen3": "Apple TV 4K (3rd Gen)",
    "Gen4KGen3": "Apple TV 4K (3rd Gen)",
    "AppleTV1,1": "Apple TV (1st Gen)",
    "AppleTV2,1": "Apple TV (2nd Gen)",
    "AppleTV3,1": "Apple TV (3rd Gen)",
    "AppleTV3,2": "Apple TV (3rd Gen)",
    "AppleTV5,3": "Apple TV HD",
    "AppleTV6,2": "Apple TV 4K",
    "AppleTV11,1": "Apple TV 4K (2nd Gen)",
    "AppleTV14,1": "Apple TV 4K (3rd Gen)",
    "AudioAccessory1,1": "HomePod",
    "AudioAccessory1,2": "HomePod",
    "AudioAccessory5,1": "HomePod mini",
    "AudioAccessory6,1": "HomePod (2nd Gen)",
    "HomePod": "HomePod",
    "HomePodMini": "HomePod mini",
    "HomePodGen2": "HomePod (2nd Gen)",
    "AirPortExpress": "AirPort Express",
    "AirPortExpressGen2": "AirPort Express (2nd Gen)",
    "Music": "Music / iTunes",
    "AppleTV": "Apple TV",
}

_MODEL_LOOKUP = {k.lower(): v for k, v in _MODEL_MAP.items()}


def _run_event_loop(loop):
    asyncio.set_event_loop(loop)
    loop.run_forever()


def _get_loop():
    global _loop
    if _loop is None:
        _loop = asyncio.new_event_loop()
        threading.Thread(
            target=_run_event_loop, args=(_loop,), daemon=True
        ).start()
    return _loop


def run_coroutine(coro):
    loop = _get_loop()
    future = asyncio.run_coroutine_threadsafe(coro, loop)
    return future.result()


def _error_json(message):
    return json.dumps({"status": "error", "message": str(message)})


def _success_json(**kwargs):
    return json.dumps({"status": "success", **kwargs})


def _run_json(coro):
    try:
        return run_coroutine(coro)
    except Exception as e:
        return _error_json(e)


@contextlib.asynccontextmanager
async def _connect_apple_tv(conf):
    atv = await pyatv.connect(conf, asyncio.get_running_loop())
    try:
        yield atv
    finally:
        atv.close()


@dataclass(slots=True)
class _PairingSession:
    handler: Any
    protocol: pyatv.const.Protocol
    ip_address: str
    is_mock: bool

    async def close(self):
        if not self.handler:
            return
        try:
            close_result = self.handler.close()
            if inspect.isawaitable(close_result):
                await close_result
        except Exception:
            pass


_active_session: _PairingSession | None = None


def _is_mock_device(ip_address):
    return ip_address == _MOCK_DEVICE_IP


async def _reset_pairing_state(close_handler=False):
    global _active_session
    session = _active_session
    _active_session = None
    if close_handler and session:
        await session.close()


def _create_mock_configuration():
    conf = pyatv.conf.AppleTV(ipaddress.IPv4Address(_MOCK_DEVICE_IP), "Mock Apple TV")
    conf.add_service(
        pyatv.conf.ManualService(
            "companion-id", pyatv.const.Protocol.Companion, 49152, {}
        )
    )
    conf.add_service(
        pyatv.conf.ManualService("mrp-id", pyatv.const.Protocol.MRP, 49153, {})
    )
    return conf


async def _discover_configuration(ip_address):
    loop = asyncio.get_running_loop()
    if _is_mock_device(ip_address):
        return [_create_mock_configuration()]
    hosts = [ip_address] if ip_address else None
    return await pyatv.scan(loop, hosts=hosts)


async def _discover_single_device(ip_address):
    devices = await _discover_configuration(ip_address)
    if not devices:
        raise RuntimeError("No Apple TV found")
    return devices[0]


def _protocol_from_identifier(protocol_identifier):
    match protocol_identifier:
        case None:
            return None
        case pyatv.const.Protocol() as protocol:
            return protocol
        case int(protocol_val):
            return pyatv.const.Protocol(protocol_val)
        case str(protocol_str):
            stripped = protocol_str.strip()
            if not stripped:
                return None
            if stripped.isdigit():
                return pyatv.const.Protocol(int(stripped))
            try:
                return pyatv.const.Protocol[stripped]
            except KeyError:
                lowered = stripped.lower()
                for protocol in pyatv.const.Protocol:
                    if protocol.name.lower() == lowered:
                        return protocol
    raise ValueError(f"Unsupported protocol identifier: {protocol_identifier}")


def _build_credentials_payload(credential_string, protocol):
    return {
        "version": _CREDENTIALS_VERSION,
        "primary_protocol": protocol.name,
        "protocol_credentials": {
            protocol.name: {
                "protocol": protocol.value,
                "protocol_name": protocol.name,
                "credential_string": credential_string,
            }
        },
    }


def _parse_credentials_payload(credentials_json):
    if not credentials_json:
        return {}

    creds_data = (
        json.loads(credentials_json)
        if isinstance(credentials_json, str)
        else credentials_json
    )
    if not isinstance(creds_data, dict):
        raise ValueError("Credentials payload must be a JSON object")

    # Legacy format: {"credential_string": "...", "protocol": 2}
    if "credential_string" in creds_data:
        protocol = _protocol_from_identifier(
            creds_data.get("protocol_name") or creds_data.get("protocol")
        )
        if protocol is None:
            raise ValueError("Legacy credentials payload is missing protocol")
        return {protocol: creds_data["credential_string"]}

    protocol_credentials = creds_data.get("protocol_credentials")
    if not isinstance(protocol_credentials, dict):
        return {}

    parsed_credentials = {}
    for protocol_key, entry in protocol_credentials.items():
        if isinstance(entry, dict):
            protocol = _protocol_from_identifier(
                entry.get("protocol_name") or entry.get("protocol") or protocol_key
            )
            credential_string = entry.get("credential_string")
            if protocol and credential_string:
                parsed_credentials[protocol] = credential_string

    return parsed_credentials


def _has_legacy_mock_credentials(credentials_json):
    parsed_credentials = _parse_credentials_payload(credentials_json)
    return any(
        credential.startswith(_LEGACY_MOCK_CREDENTIAL_PREFIX)
        for credential in parsed_credentials.values()
    )


def _apply_credentials(conf, credentials_json, preferred_protocol=_PRIMARY_PROTOCOL):
    parsed_credentials = _parse_credentials_payload(credentials_json)
    if not parsed_credentials:
        return []

    apply_order = (
        [preferred_protocol] + [p for p in parsed_credentials if p != preferred_protocol]
        if preferred_protocol in parsed_credentials
        else list(parsed_credentials.keys())
    )
    return [
        protocol
        for protocol in apply_order
        if conf.set_credentials(protocol, parsed_credentials[protocol])
    ]


async def _build_configuration(ip_address="", credentials_json=""):
    if (
        credentials_json
        and _has_legacy_mock_credentials(credentials_json)
        and not _is_mock_device(ip_address)
    ):
        raise RuntimeError(
            "Stored mock credentials cannot be used with a real Apple TV. "
            "Clear the device credentials in the app and pair again."
        )

    conf = await _discover_single_device(ip_address)

    if credentials_json:
        try:
            applied_protocols = _apply_credentials(conf, credentials_json)
            if applied_protocols:
                _LOGGER.debug(
                    "Applied credentials for protocols: %s",
                    ", ".join(protocol.name for protocol in applied_protocols),
                )
            else:
                _LOGGER.debug("No matching credentials were applied to configuration")
        except Exception as ex:
            raise RuntimeError(f"Error applying credentials: {ex}") from ex

    return conf


def _select_pairing_protocol(conf):
    if conf.get_service(_PRIMARY_PROTOCOL):
        return _PRIMARY_PROTOCOL

    discovered = [service.protocol.name for service in conf.services]
    discovered_message = ", ".join(discovered) if discovered else "none"
    raise RuntimeError(
        "Companion service is not available on this Apple TV. "
        "CitrusRemote requires Companion pairing for keyboard input and "
        f"remote buttons. Discovered protocols: {discovered_message}."
    )


def _format_model_name(conf):
    device_info = getattr(conf, "device_info", None)
    if not device_info or not hasattr(device_info, "model"):
        return "Unknown Apple TV"

    model = device_info.model
    raw_model = getattr(model, "name", str(model))
    raw_model_str = str(getattr(device_info, "raw_model", "") or "")
    model_name = (
        _MODEL_MAP.get(raw_model)
        or _MODEL_MAP.get(raw_model_str)
        or _MODEL_LOOKUP.get(raw_model.lower())
    )
    return model_name or f"Apple TV ({raw_model})"


async def _async_scan_for_devices():
    loop = asyncio.get_running_loop()
    atvs = await pyatv.scan(loop, timeout=3)
    devices = [
        {
            "name": conf.name,
            "address": str(conf.address),
            "model": _format_model_name(conf),
        }
        for conf in atvs
        if getattr(getattr(conf, "device_info", None), "operating_system", None)
        == pyatv.const.OperatingSystem.TvOS
    ]
    return json.dumps(devices)


def scan_for_devices():
    """
    Scans the local network for Apple TV devices.
    Returns a JSON string representation of a list of devices.
    """
    try:
        return run_coroutine(_async_scan_for_devices())
    except Exception as e:
        return json.dumps([{"error": str(e)}])


async def _async_initiate_pairing(ip_address):
    global _active_session
    loop = asyncio.get_running_loop()

    await _reset_pairing_state(close_handler=True)

    try:
        conf = await _discover_single_device(ip_address)
        protocol = _select_pairing_protocol(conf)
        handler = await pyatv.pair(conf, protocol, loop)
        await asyncio.wait_for(handler.begin(), timeout=15.0)
        _active_session = _PairingSession(
            handler=handler,
            protocol=protocol,
            ip_address=ip_address,
            is_mock=_is_mock_device(ip_address),
        )
        return _success_json(message=f"Pairing initiated via {protocol.name}. Enter PIN.")
    except asyncio.TimeoutError:
        await _reset_pairing_state(close_handler=True)
        return _error_json("Pairing initiation timed out. Please try again.")
    except Exception as e:
        await _reset_pairing_state(close_handler=True)
        return _error_json(e)


def initiate_pairing(ip_address):
    """
    Initiates pairing with the Apple TV.
    Returns JSON describing success or error.
    """
    return _run_json(_async_initiate_pairing(ip_address))


async def _async_finish_pairing(device_ip, pin_code):
    global _active_session
    if not _active_session:
        return _error_json("No active pairing session")
    if _active_session.ip_address != device_ip:
        return _error_json(
            "Pairing session belongs to a different device. "
            "Restart pairing for the selected Apple TV."
        )

    try:
        _LOGGER.debug("Setting PIN and calling finish()")
        _active_session.handler.pin(str(pin_code))
        _LOGGER.debug("Calling finish(), waiting for M5/M6 exchange")
        await asyncio.wait_for(_active_session.handler.finish(), timeout=15.0)
        _LOGGER.debug("finish() completed successfully")

        credentials = _active_session.handler.service.credentials
        protocol = _active_session.protocol or _PRIMARY_PROTOCOL
        if (
            credentials
            and str(credentials).startswith(_LEGACY_MOCK_CREDENTIAL_PREFIX)
            and not _active_session.is_mock
        ):
            await _reset_pairing_state(close_handler=True)
            return _error_json(
                "Mock credentials were returned for a real Apple TV pairing session. "
                "Restart pairing and try again."
            )

        await _reset_pairing_state(close_handler=False)
        return _success_json(credentials=_build_credentials_payload(credentials, protocol))
    except asyncio.TimeoutError:
        await _reset_pairing_state(close_handler=True)
        return _error_json("Pairing completion timed out. Please check PIN and try again.")
    except Exception as e:
        await _reset_pairing_state(close_handler=True)
        _LOGGER.exception("Pairing error: %s", e)
        error_msg = str(e)
        cause = e.__cause__
        if cause and (
            "AssertionError" in str(type(cause))
            or "chacha20" in str(cause).lower()
            or "aead" in str(cause).lower()
        ):
            error_msg = (
                "Cryptography error on Android while establishing Companion pairing. "
                "The device crypto backend rejected ChaCha20-Poly1305. "
                "Try restarting the app and Apple TV, then pair again."
            )
        return _error_json(error_msg)


def finish_pairing(device_ip, pin_code):
    """
    Finishes the pairing process with the given PIN.
    Returns JSON containing the credentials if successful.
    """
    return _run_json(_async_finish_pairing(device_ip, pin_code))


async def _async_cancel_pairing():
    await _reset_pairing_state(close_handler=True)
    return _success_json(message="Pairing cancelled")


def cancel_pairing():
    """
    Cancels the pairing process.
    """
    return _run_json(_async_cancel_pairing())


async def _async_validate_credentials(ip_address, credentials_json=""):
    conf = await _build_configuration(ip_address, credentials_json)
    async with _connect_apple_tv(conf):
        return _success_json(message=f"Validated credentials for {conf.name}")


def validate_credentials(ip_address="", credentials_json=""):
    return _run_json(_async_validate_credentials(ip_address, credentials_json))



# Command name -> (pyatv remote_control method, feature name or None)
_COMMAND_MAP = {
    "up": ("up", pyatv.const.FeatureName.Up, None),
    "down": ("down", pyatv.const.FeatureName.Down, None),
    "left": ("left", pyatv.const.FeatureName.Left, None),
    "left_hold": ("left", pyatv.const.FeatureName.Left, pyatv.const.InputAction.Hold),
    "right": ("right", pyatv.const.FeatureName.Right, None),
    "right_hold": ("right", pyatv.const.FeatureName.Right, pyatv.const.InputAction.Hold),
    "select": ("select", pyatv.const.FeatureName.Select, None),
    "menu": ("menu", pyatv.const.FeatureName.Menu, None),
    "menu_hold": ("menu", pyatv.const.FeatureName.Menu, pyatv.const.InputAction.Hold),
    "home": ("home", pyatv.const.FeatureName.Home, None),
    "play_pause": ("play_pause", pyatv.const.FeatureName.PlayPause, None),
    "volume_up": ("volume_up", pyatv.const.FeatureName.VolumeUp, None),
    "volume_down": ("volume_down", pyatv.const.FeatureName.VolumeDown, None),
    "mute": ("mute", None, None),
}


async def _handle_mute(atv, conf, ip_address):
    if not (
        atv.features.in_state(pyatv.const.FeatureState.Available, pyatv.const.FeatureName.Volume)
        and atv.features.in_state(pyatv.const.FeatureState.Available, pyatv.const.FeatureName.SetVolume)
    ):
        discovered = [s.protocol.name for s in conf.services]
        return (
            "Error: mute requires readable and writable volume support. "
            f"Discovered protocols: {', '.join(discovered)}."
        )

    cache_key = ip_address or str(conf.address)
    current = atv.audio.volume
    if current <= _MUTE_ZERO_THRESHOLD and cache_key not in _last_known_volume:
        return (
            "Error: mute needs a known starting volume, but Apple TV did not "
            "report one. Use volume up/down once, then try mute again."
        )

    if current > 0:
        _last_known_volume[cache_key] = current
        await atv.audio.set_volume(0)
        await asyncio.sleep(_MUTE_VERIFY_DELAY_SECONDS)
        if atv.audio.volume > _MUTE_ZERO_THRESHOLD:
            return (
                "Error: mute command was acknowledged, but Apple TV volume did "
                "not reach zero. This setup may not support absolute volume mute."
            )
        return f"Success: Muted {conf.name}"

    restored_volume = _last_known_volume.get(cache_key, _DEFAULT_UNMUTE_VOLUME)
    await atv.audio.set_volume(restored_volume)
    await asyncio.sleep(_MUTE_VERIFY_DELAY_SECONDS)
    if atv.audio.volume <= _MUTE_ZERO_THRESHOLD:
        return (
            "Error: unmute command was acknowledged, but Apple TV volume did "
            "not rise from zero."
        )
    return f"Success: Unmuted {conf.name}"


async def _async_send_command(ip_address, credentials_json, command):
    command_spec = _COMMAND_MAP.get(command)
    if command_spec is None:
        return f"Error: Unknown command '{command}'"

    conf = await _build_configuration(ip_address, credentials_json)

    async with _connect_apple_tv(conf) as atv:
        method_name, feature_name, input_action = command_spec

        if feature_name and not atv.features.in_state(
            pyatv.const.FeatureState.Available, feature_name
        ):
            discovered = [s.protocol.name for s in conf.services]
            return f"Error: {command} feature is not available. Discovered protocols: {', '.join(discovered)}."

        if command == "mute":
            return await _handle_mute(atv, conf, ip_address)

        remote_method = getattr(atv.remote_control, method_name)
        if input_action is None:
            await remote_method()
        else:
            await remote_method(action=input_action)
        return f"Success: Sent {command} to {conf.name}"


def send_command(ip_address="", credentials_json="", command=""):
    """
    Connects to an Apple TV and sends a command.
    Supported commands: up, down, left, right, select, menu, home,
                        play_pause, volume_up, volume_down, mute
    """
    try:
        return run_coroutine(_async_send_command(ip_address, credentials_json, command))
    except Exception as e:
        return f"Error: {e}"
