package com.project.cleanroom3

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import android.util.Log
import java.io.IOException
import java.lang.SecurityException
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader

class BluetoothService {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun connectToDevice(context: Context, deviceName: String): Boolean = withContext(Dispatchers.IO) {

        if (!context.hasBluetoothPermission()) {
            Log.e("Bluetooth", "권한 없음 - 연결 중단")
            return@withContext false
        }

        try {
            Log.d("Bluetooth", "페어링된 기기 목록 확인 중...")
            val device = bluetoothAdapter?.bondedDevices?.find { it.name == deviceName }

            if (device == null) {
                Log.e("Bluetooth", "기기 '$deviceName' 를 찾을 수 없음")
                return@withContext false
            }

            Log.d("Bluetooth", "기기 찾음: ${device.name} (${device.address})")
            socket = device.createRfcommSocketToServiceRecord(uuid)
            Log.d("Bluetooth", "소켓 생성 성공")

            bluetoothAdapter?.cancelDiscovery()

            Log.d("Bluetooth", "소켓 연결 시도 중...")
            socket?.connect()
            Log.d("Bluetooth", "소켓 연결 성공")

            delay(500) // ✅ 아두이노가 초기값을 보낼 시간 확보 (0.5초)

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            reader = BufferedReader(InputStreamReader(inputStream)) // ✨ 추가

            Log.d("Bluetooth", "블루투스 연결 성공")

            return@withContext true
        } catch (e: SecurityException) {
            Log.e("Bluetooth", "Permission denied", e)
        } catch (e: IOException) {
            Log.e("Bluetooth", "Connection failed", e)
        }

        return@withContext false
    }

    suspend fun readLine(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val line = reader?.readLine()
            Log.d("Bluetooth", "수신된 라인: $line")
            line
        } catch (e: Exception) {
            Log.e("Bluetooth", "readLine 오류", e)
            null
        }
    }

    suspend fun writeMessage(message: String) = withContext(Dispatchers.IO) {
        Log.d("Bluetooth", "메시지 전송: $message")
        outputStream?.write((message + "\n").toByteArray())
    }

    fun close() {
        Log.d("Bluetooth", "블루투스 연결 종료")
        inputStream?.close()
        outputStream?.close()
        socket?.close()
    }

    private fun Context.hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }
}