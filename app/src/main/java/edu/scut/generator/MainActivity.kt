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
            log("??????????????????????????????")
        }
    }

    private fun processDataFromBLT(byteArray: ByteArray) {
        byteArray.forEach {
            readBytes.offer(it) //??????????????????????????????
        }
        while (readBytes.isNotEmpty() && readBytes.peek() != Constant.MessageStartCode) {
            readBytes.poll() //?????????????????????????????????
        }
        if (readBytes.contains(Constant.MessageStopCode)) {
            var input = ""
            while (true) {
                val poll = readBytes.poll()!! //?????????????????????????????????????????????
                input += poll.toChar()
                if (poll == Constant.MessageStopCode) break
            }
            CoroutineScope(Dispatchers.Default).launch {
                //????????????????????????
                val generators = GeneratorItem.decodeGeneratorArray(input) //????????????????????????
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
                log("????????????????????? = ${generators.contentDeepToString()}")
            }
        }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
        super.onConnectionStateChanged(device, state)
        val stateText = when (state) {
            Connection.STATE_CONNECTED -> "?????????"
            Connection.STATE_CONNECTING -> "????????????"
            Connection.STATE_DISCONNECTED -> "????????????"
            Connection.STATE_PAIRED -> "?????????"
            Connection.STATE_PAIRING -> "????????????"
            Connection.STATE_RELEASED -> "?????????"
            else -> "????????????"
        }
        log("${device.name}?????????????????? ?????? = $stateText")
        viewModel.bluetoothState.postValue("???${device.name} $stateText")
    }

    private fun initBluetooth() {
        if (viewModel.discoveryListener.value == null) {
            log("???????????????")
            btManager = BTManager.getInstance().apply { initialize(application) }
            BTManager.isDebugMode = false
            viewModel.discoveryListener.value = object : DiscoveryListener {
                private val deviceList = arrayListOf<BluetoothDevice>()

                override fun onDiscoveryStart() {
                    log("????????????")
                    deviceList.clear()
                    viewModel.bluetoothDiscovering.postValue(View.VISIBLE)
                }

                override fun onDiscoveryStop() {
                    log("???????????? ?????????${deviceList.size}?????????")
                    if (deviceList.isNotEmpty()) {
                        viewModel.bluetoothDiscovering.postValue(View.INVISIBLE)
                        val findTargetDevice =
                            deviceList.filter { it.name == Constant.DefaultDevice }

                        fun connectDevice(device: BluetoothDevice) {
                            val connection = btManager.createConnection(device, this@MainActivity)
                            viewModel.bluetoothConnection.value = connection!!
                            connection.connect(null, object : ConnectCallback {
                                override fun onSuccess() {
                                    log("??? ${connection.device.name} ????????????")
                                }

                                override fun onFail(errMsg: String, e: Throwable?) {
                                    log("??? ${connection.device.name} ???????????? ???????????? = $errMsg")
                                }
                            })
                        }
                        if (findTargetDevice.isNotEmpty()) { //?????????????????? ????????????
                            connectDevice(findTargetDevice[0])
                        } else {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("????????????????????????")
                                .setItems(deviceList.map { it.name }
                                    .toTypedArray()) { dialog, which ->
                                    connectDevice(deviceList[which])
                                    dialog.dismiss()
                                }
                                .setNegativeButton("??????") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }

                override fun onDeviceFound(device: BluetoothDevice, rssi: Int) {
                    log("???????????? name = ${device.name}, rssi = $rssi")
                    if (device.name != null && deviceList.contains(device).not()) {
                        deviceList.add(device)
                        if (device.name == Constant.DefaultDevice)
                            btManager.stopDiscovery()
                    }
                }

                override fun onDiscoveryError(errorCode: Int, errorMsg: String) {
                    log("???????????? ???????????? = $errorCode, ???????????? = $errorMsg")
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
            log("???????????? startDiscovery()")
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
                            editText.hint = "??????????????????????????????"
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("?????????")
                                .setView(editText)
                                .setPositiveButton("??????") { _, _ ->
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
                                .setNegativeButton("??????", null)
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
                    .setTitle("??????")
                    .setMessage(
                        "???????????? ${BuildConfig.VERSION_CODE}\n" +
                                "???????????? ${BuildConfig.VERSION_NAME}"
                    )
                    .setPositiveButton("??????", null)
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
     * ????????????
     */
    private fun requestPermissions() {
        log("?????????????????????")
        AlertDialog.Builder(this)
            .setTitle("????????????")
            .setMessage("???????????????????????????????????????????????????????????????")
            .setPositiveButton("??????") { _, _ ->
                requestPermissions(
                    Constant.NeedPermissions,
                    Constant.requestPermissionCode
                )
            }
            .setNegativeButton("??????") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * ??????????????????
     * @return ????????????
     */
    private fun permissionsAllGranted(): Boolean {
        for (i in Constant.NeedPermissions) {
            if (checkSelfPermission(i) == PackageManager.PERMISSION_DENIED) {
                log("??????????????????")
                return false
            }
        }
        log("????????????????????????")
        return true
    }

    private fun log(any: Any) {
        debug(tag, any)
        var command = viewModel.commandText.value!!
        command += any.toString() + '\n'
        viewModel.commandText.postValue(prepareCommand(command))
    }
}