package edu.scut.generator

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cn.wandersnail.bluetooth.*
import cn.wandersnail.commons.observer.Observe
import com.github.mikephil.charting.data.Entry
import edu.scut.generator.global.Constant
import edu.scut.generator.global.debug
import edu.scut.generator.global.prepareCommand
import edu.scut.generator.ui.main.GeneratorItem
import edu.scut.generator.ui.main.MainFragment
import edu.scut.generator.ui.main.MainViewModel
import kotlinx.coroutines.*
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity(), EventObserver {

    private lateinit var viewModel: MainViewModel
    private var btManager: BTManager = BTManager.getInstance()
    private val tag = "MainActivity"

    private val readBytes: Queue<Byte> = LinkedList()

    private lateinit var refreshGenerators: Job
    private lateinit var refreshLineChat: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(MainViewModel::class.java)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        refreshGenerators = GlobalScope.launch {
            while (isActive) {
                delay(10L)
                if (SystemClock.elapsedRealtime() - viewModel.lastReadBluetoothTime.value!! >= 200L) {
//                    debug("断开连接")
                } else {
                    val list = viewModel.generatorItemList.value!!
                    viewModel.generatorItemList.postValue(list)
                    val thisGeneratorItem =
                        list.find { it.id == viewModel.thisGeneratorItem.value?.id }
                    viewModel.thisGeneratorItem.postValue(thisGeneratorItem)
                }
            }
        }

        refreshLineChat = GlobalScope.launch {
            while (isActive) {
                val powerData = viewModel.powerLineChatData.value!!
                while (powerData.size > Constant.MaxPointNumberMeanwhile) powerData.removeAt(0)
                val differTData = viewModel.differTLineChatData.value!!
                while (differTData.size > Constant.MaxPointNumberMeanwhile) differTData.removeAt(0)
                val revData = viewModel.revLineChatData.value!!
                while (revData.size > Constant.MaxPointNumberMeanwhile) revData.removeAt(0)

                val xValue =
                    (SystemClock.elapsedRealtime() - viewModel.entryDetailTime.value!!) / 1000f
                val generatorItem = viewModel.thisGeneratorItem.value
                powerData.add(Entry(xValue, generatorItem?.power?.toFloat() ?: 0f))
                differTData.add(
                    Entry(
                        xValue,
                        generatorItem?.temperatureDifference?.toFloat() ?: 0f
                    )
                )
                revData.add(Entry(xValue, generatorItem?.rev?.toFloat() ?: 0f))

                viewModel.powerLineChatData.postValue(powerData)
                viewModel.differTLineChatData.postValue(differTData)
                viewModel.revLineChatData.postValue(revData)
                delay(200L)
//                debug(powerData.toTypedArray().contentDeepToString())
            }
        }

//        viewModel.commandTextVisibility.value = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (permissionsAllGranted().not()) requestPermissions()
        else initBluetooth()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshGenerators.cancel()
        refreshLineChat.cancel()
        btManager.destroy()
//        exitProcess(0)
    }

    @Observe
    override fun onRead(device: BluetoothDevice, value: ByteArray) {
        super.onRead(device, value)
        log(
            "onRead, device = ${device.name}, " +
                    "value = ${value.toString(Charset.defaultCharset())}"
        )
        if (device == viewModel.bluetoothConnection.value?.device) {
            processDataFromBLT(value)
        } else {
            log("收到非目标设备的数据")
        }
    }

    private fun processDataFromBLT(byteArray: ByteArray) {
        byteArray.forEach {
            readBytes.offer(it) //将收到的信息插入队列
        }
        while (readBytes.isNotEmpty() && readBytes.peek() != Constant.MessageStartCode) {
            readBytes.poll() //保持队列的顶端是起始符
        }
        if (readBytes.contains(Constant.MessageStopCode)) {
            var input = ""
            while (true) {
                val poll = readBytes.poll()!! //载入从起始符到终止符之间的内容
                input += poll.toChar()
                if (poll == Constant.MessageStopCode) break
            }
            GlobalScope.launch {
                //解码成发电机信息
                val generators = GeneratorItem.decodeGeneratorArray(input) //解码成发电机信息
                viewModel.generatorItemList.postValue(generators.toMutableList())
                viewModel.lastReadBluetoothTime.postValue(SystemClock.elapsedRealtime())
                log("收到发电机信息 = ${generators.contentDeepToString()}")
            }
        }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        super.onConnectionStateChanged(device, state)
        val stateText = when (state) {
            Connection.STATE_CONNECTED -> "已连接"
            Connection.STATE_CONNECTING -> "正在连接"
            Connection.STATE_DISCONNECTED -> "断开连接"
            Connection.STATE_PAIRED -> "已配对"
            Connection.STATE_PAIRING -> "正在配对"
            Connection.STATE_RELEASED -> "已释放"
            else -> "未知状态"
        }
        log("${device.name}连接状态改变 状态 = $stateText")
        viewModel.bluetoothState.postValue("与${device.name} $stateText")
    }

    private fun initBluetooth() {
        if (viewModel.discoveryListener.value == null) {
            log("初始化蓝牙")
            btManager = BTManager.getInstance().apply { initialize(application) }
            BTManager.isDebugMode = false
            viewModel.discoveryListener.value = object : DiscoveryListener {
                private val deviceList = arrayListOf<BluetoothDevice>()

                override fun onDiscoveryStart() {
                    log("搜索开始")
                    deviceList.clear()
                    viewModel.bluetoothDiscovering.postValue(View.VISIBLE)
                }

                override fun onDiscoveryStop() {
                    log("搜索结束 共找到${deviceList.size}个设备")
                    if (deviceList.isNotEmpty()) {
                        viewModel.bluetoothDiscovering.postValue(View.INVISIBLE)
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("选择要连接的设备")
                            .setItems(deviceList.map { it.name }
                                .toTypedArray()) { dialog, which ->
                                val connection =
                                    btManager.createConnection(deviceList[which], this@MainActivity)
                                viewModel.bluetoothConnection.value = connection!!
                                connection.connect(null, object : ConnectCallback {
                                    override fun onSuccess() {
                                        log("与 ${connection.device.name} 连接成功")
                                    }

                                    override fun onFail(errMsg: String, e: Throwable?) {
                                        log("与 ${connection.device.name} 连接失败 错误信息 = $errMsg")
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
                    log("找到设备 name = ${device.name}, rssi = $rssi")
                    if (device.name != null && deviceList.contains(device).not()) {
                        deviceList.add(device)
                        if (device.name == Constant.DefaultDevice)
                            btManager.stopDiscovery()
                    }
                }

                override fun onDiscoveryError(errorCode: Int, errorMsg: String) {
                    log("搜索错误 错误代码 = $errorCode, 错误信息 = $errorMsg")
                    viewModel.bluetoothDiscovering.postValue(View.INVISIBLE)
                }
            }
            btManager.addDiscoveryListener(viewModel.discoveryListener.value!!)
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        if (btManager.bluetoothAdapter!!.isEnabled.not()) {
            startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                Constant.startBluetoothCode
            )
        } else {
            log("开始搜索 startDiscovery()")
            btManager.startDiscovery()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tempTest -> {
            }
            R.id.debugMode -> {
                viewModel.commandTextVisibility.value = View.VISIBLE
            }
            R.id.simulationData -> {
                GlobalScope.launch {
                    while (isActive) {
                        val random = Math.random() - 0.5
                        val generatorItem =
                            GeneratorItem(
                                id = Constant.TempUUid,
                                power = 20.0 + 6 * random,
                                temperatureDifference = 5.0 + 3 * random,
                                rev = 450.0 + 300 * random
                            )
                        var message = GeneratorItem.encodeGeneratorArray(arrayOf(generatorItem))
                        message = message.replace(".", " ??? ")
                        log(message)
                        processDataFromBLT(message.toByteArray(Charset.defaultCharset()))
                        delay(200L)
                    }
                }
            }
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
        log("onActivityResult requestCode = $requestCode, resultCode = $resultCode")
        when (requestCode) {
            Constant.startBluetoothCode -> startDiscovery()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        AlertDialog.Builder(this)
//            .setTitle("退出")
//            .setMessage("您执行了返回操作，是否要退出程序？")
//            .setPositiveButton("是") { _, _ -> finish() }
//            .setNegativeButton("否", null)
//            .show()
    }

    /**
     * 申请权限
     */
    private fun requestPermissions() {
        log("申请权限对话框")
        AlertDialog.Builder(this)
            .setTitle("申请权限")
            .setMessage("软件需要权限才能正常运行，请授予相应的权限")
            .setPositiveButton("授权") { _, _ ->
                requestPermissions(
                    Constant.NeedPermissions,
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
        for (i in Constant.NeedPermissions) {
            if (checkSelfPermission(i) == PackageManager.PERMISSION_DENIED) {
                log("权限授予失败")
                return false
            }
        }
        log("权限全部授予成功")
        return true
    }

    private fun log(any: Any) {
        debug(tag, any)
        var command = viewModel.commandText.value!!
        command += any.toString() + '\n'
        viewModel.commandText.postValue(prepareCommand(command))
    }
}