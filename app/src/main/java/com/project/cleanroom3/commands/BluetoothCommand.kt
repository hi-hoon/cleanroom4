package com.project.cleanroom3.commands

enum class BluetoothCommand(val value: String) {
    FAN_ON("FAN_ON"),
    FAN_OFF("FAN_OFF"),
    DOOR_OPEN("DOOR_OPEN"),
    DOOR_CLOSE("DOOR_CLOSE"),
    TEMP_CHECK("TEMP_CHECK"),
    HUMID_CHECK("HUMID_CHECK")
    // ⚙️ 필요에 따라 추가 가능
}
