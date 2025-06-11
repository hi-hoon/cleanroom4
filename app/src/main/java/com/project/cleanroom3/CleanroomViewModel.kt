package com.project.cleanroom3

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.cleanroom3.commands.BluetoothCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay

@HiltViewModel
class CleanroomViewModel @Inject constructor() : ViewModel() {

    private val bluetoothService = BluetoothService()

    private val _sirenOn = mutableStateOf(false)
    val sirenOn: State<Boolean> get() = _sirenOn

    private val _fireOn = mutableStateOf(false)
    val fireOn: State<Boolean> get() = _fireOn

    private val _temperature = mutableStateOf("")
    val temperature: State<String> get() = _temperature

    private val _humidity = mutableStateOf("")
    val humidity: State<String> get() = _humidity

    private val _inputTemperature = mutableStateOf("")
    val inputTemperature: State<String> get() = _inputTemperature

    private val _inputHumidity = mutableStateOf("")
    val inputHumidity: State<String> get() = _inputHumidity


    private val _dust = mutableStateOf("")
    val dust: State<String> get() = _dust

    private val _ph = mutableStateOf("")
    val ph: State<String> get() = _ph

    var condTemp = mutableStateOf(false)
    var condHumid = mutableStateOf(false)
    var condDust = mutableStateOf(false)
    var condPH = mutableStateOf(false)


    fun connectAndListen(context: Context, deviceName: String) {
        viewModelScope.launch {
            Log.d("Bluetooth", "연결 시도 중: $deviceName")  // ✅ 로그 추가

            val connected = bluetoothService.connectToDevice(context, deviceName)
            if (!connected) {
                Log.e("Bluetooth", "연결 실패 또는 권한 없음")
                return@launch
            }

            while (true) {
                val raw = bluetoothService.readLine() ?: continue

                val messages = raw.split("\n")
                for (msg in messages) {
                    val trimmed = msg.trim()
                    if (trimmed.isEmpty()) continue

                    Log.d("Bluetooth", "수신된 메시지: $trimmed")

                    when {
                        trimmed.startsWith("TEMP=") -> {
                            val value = trimmed.removePrefix("TEMP=").trim()
                            _temperature.value = value
                        }

                        trimmed.startsWith("HUMID=") -> {
                            val value = trimmed.removePrefix("HUMID=").trim()
                            _humidity.value = value
                        }

                        trimmed.startsWith("DUST=") -> {
                            val value = trimmed.removePrefix("DUST=").trim()
                            _dust.value = value
                        }

                        trimmed.startsWith("PH=") -> {
                            val value = trimmed.removePrefix("PH=").trim()
                            _ph.value = value
                        }

                        trimmed.startsWith("COND_") -> {
                            when (trimmed) {
                                "COND_TEMP" -> condTemp.value = true
                                "COND_HUMID" -> condHumid.value = true
                                "COND_DUST" -> condDust.value = true
                                "COND_PH" -> condPH.value = true
                            }
                            _sirenOn.value = true  // 조건 발생 시 사이렌 활성화
                        }

                        trimmed == "FLAME" -> {
                            _fireOn.value = true
                            _sirenOn.value = true
                        }

                        trimmed == "FLAME_OFF" -> {
                            _fireOn.value = false
                            _sirenOn.value = false
                        }

                        trimmed == "SAFE" -> {
                            condTemp.value = false
                            condHumid.value = false
                            condDust.value = false
                            condPH.value = false
                            _sirenOn.value = false
                        }
                    }
                }

                delay(100)  // 너무 빠른 루프 방지
            }
        }
    }

    // ✅ Enum 기반 명령 전송 (정적)
    fun sendCommand(command: BluetoothCommand) {
        viewModelScope.launch {
            try {
                bluetoothService.writeMessage(command.value)
            } catch (e: Exception) {
                Log.e("ViewModel", "명령 전송 실패: ${command.name}", e)
            }
        }
    }

    // ✅ 문자열 기반 명령 전송 (동적)
    fun sendCommandRaw(raw: String) {
        viewModelScope.launch {
            try {
                bluetoothService.writeMessage(raw)
            } catch (e: Exception) {
                Log.e("ViewModel", "Raw 명령 전송 실패: $raw", e)
            }
        }
    }
    fun updateTemperature(newValue: String) {
        _temperature.value = newValue
    }

    fun updateHumidity(newValue: String) {
        _humidity.value = newValue
    }

    fun updateInputTemperature(value: String) {
        _inputTemperature.value = value
    }

    fun updateInputHumidity(value: String) {
        _inputHumidity.value = value
    }

    override fun onCleared() {
        bluetoothService.close()
        super.onCleared()
    }
}
