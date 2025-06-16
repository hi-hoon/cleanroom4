package com.project.cleanroom3

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.project.cleanroom3.ui.theme.Cleanroom3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.project.cleanroom3.commands.BluetoothCommand
import com.project.cleanroom3.ui.theme.AlertRed
import com.project.cleanroom3.ui.theme.Black
import com.project.cleanroom3.ui.theme.White12
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

import androidx.compose.material3.Text

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha

@Composable
fun SensorBox(label: String, value: String, isAlert: Boolean = false) {
    val backgroundColor = if (isAlert) AlertRed else Color.White // 배경 흰색

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(100.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.labelMedium)
        }
    }
}



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: CleanroomViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms.all { it.value }
            if (granted) {
                lifecycleScope.launch {
                    viewModel.connectAndListen(this@MainActivity, "HC-06")
                }
            } else {
                Log.e("Permission", "필수 권한이 거부되었습니다.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            Cleanroom3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = White12
                ) {
                    CleanroomUIScreen(viewModel)
                }
            }
        }

        // ✅ Android 12 이상: 권한 요청 → 연결
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            // Android 11 이하: 바로 연결
            lifecycleScope.launch {
                viewModel.connectAndListen(this@MainActivity, "HC-06")
            }
        }
    }
}



@Composable
fun CleanroomUIScreen(viewModel: CleanroomViewModel) {

    val temp by viewModel.temperature
    val humid by viewModel.humidity
    var fanOn by remember { mutableStateOf(false) }
    var doorOpen by remember { mutableStateOf(false) }

    val fireOn by viewModel.fireOn
    val sirenOn by viewModel.sirenOn

    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    Column(modifier = Modifier.padding(16.dp)) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center  // Box 내 자식 정렬
        ) {
            Image(
                painter = painterResource(id = R.drawable.sensor_background),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.7f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Cleanroom System",
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    SensorBox("온도", "${viewModel.temperature.value}℃")
                    SensorBox("습도", "${viewModel.humidity.value}%")
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp)) // 위아래 간격

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    SensorBox("미세먼지", "${viewModel.dust.value}㎍/㎥")
                    SensorBox("pH", viewModel.ph.value)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 온도 입력 박스
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = viewModel.inputTemperature.value,
                        onValueChange = { viewModel.updateInputTemperature(it) },

                        label = { Text("온도 입력 (℃)", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.width(120.dp)
                    )
                    Button(onClick = { viewModel.sendCommandRaw("TEMP:${viewModel.inputTemperature.value}") }) {
                        Text("확인", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 습도 입력 박스
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = viewModel.inputHumidity.value,
                        onValueChange = { viewModel.updateInputHumidity(it) },
                        label = { Text("습도 입력 (%)", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.width(120.dp)
                    )
                    Button(onClick = { viewModel.sendCommandRaw("HUMID:${viewModel.inputHumidity.value}") }) {
                        Text("확인", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))


        val condTemp by viewModel.condTemp
        val condHumid by viewModel.condHumid
        val condDust by viewModel.condDust
        val condPH by viewModel.condPH

        val conditionImages by remember {
            derivedStateOf {
                buildList {
                    if (viewModel.condTemp.value) add(R.drawable.red_warning)
                    if (viewModel.condHumid.value) add(R.drawable.blue_warning)
                    if (viewModel.condDust.value) add(R.drawable.green_warning)
                    if (viewModel.condPH.value) add(R.drawable.yellow_warning)
                }
            }
        }
        var currentIndex by remember { mutableStateOf(0) }


        LaunchedEffect(conditionImages) {
            while (true) {
                delay(1000)
                if (conditionImages.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % conditionImages.size
                } else {
                    currentIndex = 0 // 리셋
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 왼쪽: 사이렌 이미지 (조건 기반)
                if (conditionImages.isNotEmpty()) {
                    Image(
                        painter = painterResource(id = conditionImages[currentIndex]),
                        contentDescription = "위험 경고",
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.siren_off),
                        contentDescription = "사이렌 꺼짐",
                        modifier = Modifier.size(120.dp)
                    )
                }

                // 오른쪽: 불꽃 감지 이미지 (항상 표시)
                Image(
                    painter = painterResource(id = if (fireOn) R.drawable.fire_on else R.drawable.fire_off),
                    contentDescription = "불꽃 감지",
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            val message = when {
                fireOn -> "🔥 화재 감지됨!"
                sirenOn -> "⚠️ 위험 조건 감지됨!"
                else -> "시스템 작동 중"
            }

            val textColor = when {
                fireOn -> Color.Red
                sirenOn -> AlertRed
                else -> Color.Black
            }

            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 팬 제어 박스
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("팬", style = MaterialTheme.typography.bodyLarge)
                    Row {
                        Button(onClick = {
                            fanOn = true
                            viewModel.sendCommand(BluetoothCommand.FAN_ON)
                        }) { Text("작동", style = MaterialTheme.typography.labelMedium) }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = {
                            fanOn = false
                            viewModel.sendCommand(BluetoothCommand.FAN_OFF)
                        }) { Text("중지", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            // 문 제어 박스
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("문", style = MaterialTheme.typography.bodyLarge)
                    Row {
                        Button(onClick = {
                            doorOpen = true
                            viewModel.sendCommand(BluetoothCommand.DOOR_OPEN)
                        }) { Text("열림", style = MaterialTheme.typography.labelMedium) }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = {
                            doorOpen = false
                            viewModel.sendCommand(BluetoothCommand.DOOR_CLOSE)
                        }) { Text("닫힘", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}


