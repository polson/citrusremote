package com.reggiesoft.citrusremote.data.model

enum class RemoteCommand(val command: String) {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    LEFT_HOLD("left_hold"),
    RIGHT("right"),
    RIGHT_HOLD("right_hold"),
    SELECT("select"),
    MENU("menu"),
    MENU_HOLD("menu_hold"),
    HOME("home"),
    PLAY_PAUSE("play_pause"),
    VOLUME_UP("volume_up"),
    VOLUME_DOWN("volume_down"),
    MUTE("mute")
}
