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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import cn.wandersnail.bluetooth.*
import cn.wandersnail.commons.observer.Observe
import com.github.mikephil.charting.data.Entry
import edu.scut.generator.database.Generator
import edu.scut.generator.database.GeneratorDataBase
import edu.scut.generator.global.Constant
import edu.scut.generator.global.GeneratorState
import edu.scut.generator.global.debug
import edu.scut.generator.global.prepareCommand
import edu.scut.generator.ui.main.GeneratorItem
import edu.scut.generator.ui.main.MainFragment
import edu.scut.generator.ui.main.MainViewModel
import kotlinx.coroutines.*
import java.nio.charset.Charset
import java.util.*
import kotlin.math.pow
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), EventObserver {

    private lateinit var viewModel: MainViewModel
    private var btManager: BTManager = BTManager.getInstance()
    private val tag = "MainActivity"
    private val generatorDataBase: GeneratorDataBase
        get() = GeneratorDataBase.getInstance()

    private val readBytes: Queue<Byte> = LinkedList()

    private lateinit var refreshGenerators: Job
    private lateinit var refreshLineChat: Job
    private var simulationData: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(MainViewModel::class.java)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(), "MainFragment")
                .commitNow()
        }
        GeneratorDataBase.init(context = applicationContext)

        updateGenerators()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        refreshGenerators = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10L)
                if (SystemClock.elapsedRealtime() - viewModel.lastReadBluetoothTime.value!! >= 250L) {
                    val list = viewModel.generatorItemList.value!!
                    list.forEach {
                        it.power = 0.0
                        it.rev = 0.0
                        it.temperatureDifference = 0.0
                        it.state = GeneratorState.Disconnected
                    }
                    viewModel.generatorItemList.postValue(list)
                } else {
                    if (SystemClock.elapsedRealtime() - viewModel.editGeneratorTime.value!! >= 500)
                        withContext(Dispatchers.Main) {
                            val list = viewModel.generatorItemList.value!!
                            viewModel.generatorItemList.value = list
                            val thisGeneratorItem =
                                list.find { it.id == viewModel.thisGeneratorItem.value?.id }
                            viewModel.thisGeneratorItem.value = thisGeneratorItem
                        }
                }
            }
        }

        refreshLineChat = CoroutineScope(Dispatchers.Default).launch {
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

                withContext(Dispatchers.Main) {
                    viewModel.powerLineChatData.value = powerData
                    viewModel.differTLineChatData.value = differTData
                    viewModel.revLineChatData.value = revData
                }
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
        simulationData?.cancel()
        btManager.destroy()
        generatorDataBase.close()

        GlobalScope.launch {
            debug(500)
            exitProcess(0)
        }
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
            CoroutineScope(Dispatchers.Default).launch {
                //解码成发电机信息
                val generators = GeneratorItem.decodeGeneratorArray(input) //解码成发电机信息
                generators.forEach {
                    withContext(Dispatchers.IO) {
                        val queryResult = generatorDataBase.getGenerator(it.id.toString())
                        if (queryResult == null) {
                            generatorDataBase.addGenerator(
                                Generator(
                                    uuid = it.id.toString(),
                                    name = it.name,
                                    createTime = System.currentTimeMillis()
                                )
                            )
                        } else {
                            it.name = queryResult.name
                        }
                    }
                }
                val tempList = generators.toMutableList()
                withContext(Dispatchers.Main) {
                    viewModel.generatorItemList.value = tempList
                    viewModel.lastReadBluetoothTime.value = SystemClock.elapsedRealtime()
                }
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
                        val findTargetDevice =
                            deviceList.filter { it.name == Constant.DefaultDevice }

                        fun connectDevice(device: BluetoothDevice) {
                            val connection = btManager.createConnection(device, this@MainActivity)
                            viewModel.bluetoothConnection.value = connection!!
                            connection.connect(null, object : ConnectCallback {
                                override fun onSuccess() {
                                    log("与 ${connection.device.name} 连接成功")
                                }

                                override fun onFail(errMsg: String, e: Throwable?) {
                                    log("与 ${connection.device.name} 连接失败 错误信息 = $errMsg")
                                }
                            })
                        }
                        if (findTargetDevice.isNotEmpty()) { //找到目标设备 直接连接
                            connectDevice(findTargetDevice[0])
                        } else {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("选择要连接的设备")
                                .setItems(deviceList.map { it.name }
                                    .toTypedArray()) { dialog, which ->
                                    connectDevice(deviceList[which])
                                    dialog.dismiss()
                                }
                                .setNegativeButton("取消") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setCancelable(false)
                                .show()
                        }
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

    private fun updateGenerators() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = generatorDataBase.getAllGenerator().map {
                GeneratorItem(
                    id = UUID.fromString(it.uuid),
                    name = it.name,
                    state = GeneratorState.Disconnected
                )
            }.toMutableList()
            viewModel.mustNotifyDataSetChanged.postValue(true)
            viewModel.generatorItemList.postValue(list)
            val thisGenerator = viewModel.thisGeneratorItem
            thisGenerator.postValue(viewModel.generatorItemList.value!!.find { it.id == thisGenerator.value!!.id })
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tempTest -> {
//                generatorDataBase.deleteGeneratorByUuid("123456")
//                generatorDataBase.addGenerator(Generator(uuid = "123456"))
//                generatorDataBase.deleteAll()
            }
            R.id.rename -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val from =
                        generatorDataBase.getGenerator(viewModel.thisGeneratorItem.value?.id.toString())
                    if (from != null) {
                        withContext(Dispatchers.Main) {
                            val editText = EditText(this@MainActivity).apply { setText(from.name) }
                            editText.hint = "请输入新的发电机名称"
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("重命名")
                                .setView(editText)
                                .setPositiveButton("确定") { _, _ ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        generatorDataBase.updateGenerator(from.apply {
                                            name = editText.text.toString()
                                        })
                                        viewModel.generatorItemList.value!!.find { it.id.toString() == from.uuid }?.name =
                                            from.name
                                        viewModel.thisGeneratorItem.postValue(
                                            viewModel.thisGeneratorItem.value!!.apply {
                                                name = from.name
                                            })
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                    }
                }
            }
            R.id.delete -> {
                if (viewModel.thisGeneratorItem.value != null) {
                    val uuid = viewModel.thisGeneratorItem.value!!.id.toString()
                    onBackPressed()
                    CoroutineScope(Dispatchers.IO).launch {
                        generatorDataBase.deleteGeneratorByUuid(uuid)
                        viewModel.generatorItemList.value!!.remove(viewModel.generatorItemList.value!!.find { it.id.toString() == uuid })
                    }
                }
            }
            R.id.debugMode -> {
                viewModel.commandTextVisibility.value =
                    when (viewModel.commandTextVisibility.value) {
                        View.VISIBLE -> View.INVISIBLE
                        View.INVISIBLE -> View.VISIBLE
                        else -> View.INVISIBLE
                    }
            }
            R.id.simulationData -> {
                if (simulationData == null) {
                    simulationData = CoroutineScope(Dispatchers.IO).launch {
                        while (isActive) {
                            val random = Math.random() - 0.5
                            val generatorItem =
                                GeneratorItem(
                                    id = Constant.DefaultUUID,
                                    power = 20.0 + 8 * random,
                                    temperatureDifference = 5.0 + 3 * random,
                                    rev = 450.0 + 350 * random
                                )
                            val message = GeneratorItem.encodeGeneratorArray(arrayOf(generatorItem))
                            processDataFromBLT(message.toByteArray(Charset.defaultCharset()))
                            delay(100L)
                        }
                    }
                } else {
                    simulationData!!.cancel()
                    simulationData = null
                }
            }
            R.id.manualDiscover -> startDiscovery()
            R.id.stopDiscover -> btManager.stopDiscovery()
            R.id.about -> {
                AlertDialog.Builder(this)
                    .setTitle("关于")
                    .setMessage(
                        "版本代号 ${BuildConfig.VERSION_CODE}\n" +
                                "版本名称 ${BuildConfig.VERSION_NAME}"
                    )
                    .setPositiveButton("关闭", null)
                    .show()
            }
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