import asyncio
import logging
import sys

logging.basicConfig(
    level=logging.DEBUG, format="%(name)s - %(levelname)s - %(message)s"
)

sys.path.insert(0, ".venv/pyatv_repo")

from tests.fake_device import FakeAppleTV
from pyatv.const import KeyboardFocusState, Protocol
from pyatv.protocols.companion.api import SystemStatus

_FIXED_PORTS = {
    Protocol.Companion: 49152,
    Protocol.MRP: 49153,
    Protocol.DMAP: 49154,
}


async def simulate_keyboard_lifecycle(fake_atv):
    companion_state = fake_atv.get_state(Protocol.Companion)
    companion_usecase = fake_atv.get_usecase(Protocol.Companion)
    mrp_state = fake_atv.get_state(Protocol.MRP)
    text_field_opened = False
    last_power_state = True

    while True:
        device_awake = companion_state.powered_on and mrp_state.powered_on

        if not device_awake:
            if last_power_state:
                companion_usecase.set_system_status(SystemStatus.Asleep)
                companion_usecase.set_rti_focus_state(KeyboardFocusState.Unfocused)
                text_field_opened = False
                print("Mock Apple TV asleep", flush=True)
            last_power_state = False
            await asyncio.sleep(0.1)
            continue

        if not last_power_state:
            companion_usecase.set_system_status(SystemStatus.Awake)
            print("Mock Apple TV awake", flush=True)
        last_power_state = True

        if companion_state.has_paired and not text_field_opened:
            await asyncio.sleep(0.75)
            companion_usecase.set_rti_text("")
            companion_usecase.set_rti_focus_state(KeyboardFocusState.Focused)
            text_field_opened = True
            print("Mock text field opened", flush=True)

        select_pressed = mrp_state.last_button_pressed == "select"
        if text_field_opened and select_pressed:
            await asyncio.sleep(0.25)
            companion_usecase.set_rti_focus_state(KeyboardFocusState.Unfocused)
            print("Mock text field submitted", flush=True)
            mrp_state.last_button_pressed = None
            await asyncio.sleep(1.0)
            companion_usecase.set_rti_text("")
            companion_usecase.set_rti_focus_state(KeyboardFocusState.Focused)
            print("Mock text field reopened", flush=True)

        await asyncio.sleep(0.1)


async def main():
    loop = asyncio.get_running_loop()

    orig_create_server = loop.create_server
    service_start_order = iter(_FIXED_PORTS.values())

    async def fake_create_server(*args, **kwargs):
        port = args[2] if len(args) > 2 else kwargs.get("port")
        if not port:
            kwargs["port"] = next(service_start_order)
        return await orig_create_server(*args, **kwargs)

    loop.create_server = fake_create_server

    fake_atv = FakeAppleTV(loop, test_mode=False)

    _, companion_usecase = fake_atv.add_service(Protocol.Companion)
    _, mrp_usecase = fake_atv.add_service(Protocol.MRP)
    fake_atv.add_service(
        Protocol.DMAP,
        hsgid="00000000-0000-0000-0000-000000000000",
        pairing_guid="0102030405060708090A0B0C0D0E0F10",
        session_id="1234567890ABCDEF",
    )

    companion_usecase.set_installed_apps(
        {
            "com.apple.TVWatchList": "Apple TV",
            "com.apple.TVMusic": "Music",
            "com.netflix.Netflix": "Netflix",
        }
    )
    companion_usecase.set_available_accounts(
        {
            "primary-user": "Phil",
            "guest-user": "Guest",
        }
    )
    companion_usecase.set_system_status(SystemStatus.Awake)
    companion_usecase.set_rti_focus_state(KeyboardFocusState.Unfocused)
    companion_usecase.set_rti_text("")
    mrp_usecase.change_volume_control(True)
    mrp_usecase.example_video(
        paused=False,
        title="For All Mankind",
        total_time=3210,
        position=842,
        app_name="Apple TV",
    )

    await fake_atv.start()
    asyncio.create_task(simulate_keyboard_lifecycle(fake_atv))
    print("Mock Apple TV running...", flush=True)
    print(f"Companion Port: {_FIXED_PORTS[Protocol.Companion]}", flush=True)
    print(f"MRP Port: {_FIXED_PORTS[Protocol.MRP]}", flush=True)
    print("PAIRING PIN: 1111", flush=True)

    while True:
        await asyncio.sleep(3600)


if __name__ == "__main__":
    asyncio.run(main())
