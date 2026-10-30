package com.hyundaiapp.cruisebreaker

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.hyundaiapp.cruisebreaker.ui.CruiseBreakerNav
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

// DO 선언 앱: 주행 중에도 화면이 보인다 → 주행 중 화면 규정(PRD 5.5) 준수
class MainActivity : ComponentActivity() {
    private val container by lazy { (application as CruiseBreakerApp).container }

    // 차속(CAR_SPEED)은 dangerous 퍼미션 → 런타임 동의. 설명 화면은 S5 첫 실행 동의에서 붙인다(길민종)
    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.signals.onPermissionsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(SPEED_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            speedPermission.launch(SPEED_PERMISSION)
        }
        setContent {
            CruiseBreakerTheme {
                CruiseBreakerNav(container)
            }
        }
    }
}

private const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
