package edu.scut.generator

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import cn.wandersnail.bluetooth.BTManager
import cn.wandersnail.bluetooth.ConnectCallback
import cn.wandersnail.bluetooth.DiscoveryListener
import cn.wandersnail.bluetooth.EventObserver
import edu.scut.generator.global.Constant
import edu.scut.generator.ui.main.MainFragment
import edu.scut.generator.ui.main.MainViewModel
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), EventObserver {

    private lateinit var viewModel: MainViewModel
    private var btManager: BTManager? = null
    private val tag = MainActivity::class.java.name

    private val discoveryListener = object : DiscoveryListener {
        private val deviceList = arrayListOf<BluetoothDevice>()

        override fun onDiscoveryStart() {
            debug("搜索开始 监听器 = ${this.hashCode()}")
            deviceList.clear()
            viewModel.bluetoothDiscovering.postValue(View.VISIBLE)
        }

        override fun onDiscoveryStop() {
            debug("搜索结束 共找到${deviceList.size}个设备")
            if (deviceList.isNotEmpty()) {
                viewModel.bluetoothDiscovering.postValue(View.GONE)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("选择要连接的设备")
                    .setItems(deviceList.map { it.name }.toTypedArray()) { dialog, which ->
                        val connection =
                            btManager?.createConnection(deviceList[which], this@MainActivity)
                        viewModel.bluetoothConnection.value = connection
                        connection?.connect(UUID.randomUUID(), object : ConnectCallback {
                            override fun onSuccess() {
                                debug("与 ${connection.device.name} 连接成功")
                            }

                            override fun onFail(errMsg: String, e: Throwable?) {
                                Log.e(tag, "与 ${connection.device.name} 连接失败 错误信息 = $errMsg")
                            }
                        })
                        dialog.dismiss()
                    }
                    .setNeutralButton("取消") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        override fun onDeviceFound(device: BluetoothDevice, rssi: Int) {
            debug("找到设备 device.name = ${device.name}, rssi = $rssi")
            if (device.name != null && deviceList.contains(device).not()) deviceList.add(device)
        }

        override fun onDiscoveryError(errorCode: Int, errorMsg: String) {
            Log.e(tag, "搜索错误 错误代码 = $errorCode, 错误信息 = $errorMsg")
            viewModel.bluetoothDiscovering.postValue(View.GONE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProvider(this).get("MainViewModel", MainViewModel::class.java)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        if (permissionsAllGranted().not()) requestPermissions()
        else initBluetooth()
    }

    override fun onDestroy() {
        super.onDestroy()
        debug("销毁蓝牙")
        btManager?.destroy()
    }

    private fun initBluetooth() {
        debug("初始化蓝牙")
        if (btManager == null) {
            debug("实例化和初始化蓝牙")
            btManager = BTManager.getInstance().apply { initialize(application) }
            BTManager.isDebugMode = true
            btManager!!.addDiscoveryListener(discoveryListener)
        }
        startDiscovery()
    }

    private fun startDiscovery() {
        debug("开始搜索 startDiscovery()")
        if (btManager?.bluetoothAdapter!!.isEnabled.not()) {
            startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                Constant.startBluetoothCode
            )
        } else {
            btManager?.startDiscovery()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.manualDiscover -> startDiscovery()
            R.id.exit -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissionsAllGranted().not()) requestPermissions()
        else initBluetooth()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        debug("onActivityResult requestCode = $requestCode, resultCode = $resultCode")
        when (requestCode) {
            Constant.startBluetoothCode -> startDiscovery()
        }
    }

    /**
     * 申请权限
     */
    private fun requestPermissions() {
        debug("申请权限对话框")
        AlertDialog.Builder(this)
            .setTitle("申请权限")
            .setMessage("软件需要权限才能正常运行，请授予相应的权限")
            .setPositiveButton("授权") { _, _ ->
                requestPermissions(
                    Constant.needPermissions,
                    Constant.requestPermissionCode
                )
            }
            .setNegativeButton("拒绝") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * 权限全部授予
     * @return 授权结果
     */
    private fun permissionsAllGranted(): Boolean {
        for (i in Constant.needPermissions) {
            if (checkSelfPermission(i) == PackageManager.PERMISSION_DENIED) {
                debug("权限授予失败")
                return false
            }
        }
        debug("权限全部授予成功")
        return true
    }

    private fun debug(any: Any) = edu.scut.generator.global.debug(tag, any)
}