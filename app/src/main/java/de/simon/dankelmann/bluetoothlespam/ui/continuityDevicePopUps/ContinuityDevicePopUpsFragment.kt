package de.simon.dankelmann.bluetoothlespam.ui.continuityDevicePopUps

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.GoogleFastPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Constants.LogLevel
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.LogEntryModel
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.AdvertisementLoopService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentContinuityDevicePopupsBinding


class ContinuityDevicePopUpsFragment : Fragment(), IBleAdvertisementServiceCallback {

    private var _binding: FragmentContinuityDevicePopupsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _viewModel: ContinuityDevicePopUpsViewModel? = null
    private var _bluetoothLeAdvertisementService: BluetoothLeAdvertisementService? = null
    private var _advertisementLoopService: AdvertisementLoopService? = null
    private val _logTag = "continuityDevicePopUpsFragment"
    private lateinit var _toggleButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(ContinuityDevicePopUpsViewModel::class.java)
        _viewModel = viewModel
        _binding = FragmentContinuityDevicePopupsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // get bt adapter
        val bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
        if(bluetoothAdapter != null){
            _bluetoothLeAdvertisementService = BluetoothLeAdvertisementService(bluetoothAdapter)
            _advertisementLoopService = AdvertisementLoopService(_bluetoothLeAdvertisementService!!)

            // setup callbacks
            _bluetoothLeAdvertisementService?.addBleAdvertisementServiceCallback(this)
            _advertisementLoopService?.addBleAdvertisementServiceCallback(this)

            // Add advertisement sets to the Loop Service:
            val _continuityDevicePopUpsGenerator = ContinuityDevicePopUpAdvertisementSetGenerator()
            val _advertisementSets = _continuityDevicePopUpsGenerator.getAdvertisementSets()
            _advertisementLoopService?.addAdvertisementSetCollection(_advertisementSets)
        } else {
            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Bluetooth could not be initialized"
            _viewModel!!.addLogEntry(logEntry)
        }

        setupUi()

        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if(_advertisementLoopService != null && _advertisementLoopService!!.advertising){
            stopAdvertising()
        }
    }

    fun startAdvertising(){
        if(_advertisementLoopService != null){
            _advertisementLoopService!!.startAdvertising()

            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Started Advertising"
            _viewModel!!.addLogEntry(logEntry)

            _viewModel!!.isTransmitting.postValue(true)

            _toggleButton.text = "Stop Advertising"
        } else {
            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Could not start Advertising"
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    fun stopAdvertising(){
        if(_advertisementLoopService != null){
            _advertisementLoopService!!.stopAdvertising()

            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Stopped Advertising"
            _viewModel!!.addLogEntry(logEntry)

            _viewModel!!.isTransmitting.postValue(false)

            _toggleButton.text = "Start Advertising"
        } else {
            val logEntry = LogEntryModel()
            logEntry.level = LogLevel.Info
            logEntry.message = "Could not Stop Advertising"
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    fun setupUi(){
        if(_viewModel != null){

            // toggle button
            var toggleBtn: Button = binding.advertiseButton
            _toggleButton = toggleBtn
            //animation view
            val animationView: LottieAnimationView = binding.continuityDevicePopUpsAnimation

            val toggleOnClickListener = View.OnClickListener { view ->
                if (_advertisementLoopService != null) {
                    if (!_advertisementLoopService!!.advertising) {
                        startAdvertising()
                    } else {
                        stopAdvertising()
                    }
                }
            }

            toggleBtn.setOnClickListener(toggleOnClickListener)
            animationView.setOnClickListener(toggleOnClickListener)

            _viewModel!!.isTransmitting.observe(viewLifecycleOwner) {
                if(it == true){
                    animationView.repeatCount = LottieDrawable.INFINITE
                    animationView.playAnimation()
                } else {
                    animationView.cancelAnimation()
                }
            }

            // txPower
            val continuityDevicePopUpsTxPowerSeekbar = binding.continuityDevicePopUpsTxPowerSeekbar
            val continuityDevicePopUpsTxPowerSeekbarLabel: TextView = binding.continuityDevicePopUpsTxPowerSeekbarLabel
            continuityDevicePopUpsTxPowerSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                    var newTxPowerLevel = progress
                    var newTxPowerLabel = "High"

                    when (progress) {
                        0 -> {
                            newTxPowerLabel = "Ultra Low"
                        }
                        1 -> {
                            newTxPowerLabel = "Low"
                        }
                        2 -> {
                            newTxPowerLabel = "Medium"
                        }
                        3 -> {
                            newTxPowerLabel = "High"
                        } else -> {
                        newTxPowerLevel = 3
                        newTxPowerLabel = "High"
                    }
                    }

                    continuityDevicePopUpsTxPowerSeekbarLabel.text = "TX Power: ${newTxPowerLabel}"
                    if(_bluetoothLeAdvertisementService != null){
                        _bluetoothLeAdvertisementService!!.txPowerLevel = newTxPowerLevel
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // you can probably leave this empty
                }
            })

            // seekbar
            val continuityDevicePopUpsRepeatitionSeekbar: SeekBar = binding.continuityDevicePopUpsRepeatitionSeekbar
            val continuityDevicePopUpsRepeatitionLabel: TextView = binding.continuityDevicePopUpsRepeatitionSeekbarLabel
            continuityDevicePopUpsRepeatitionSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    continuityDevicePopUpsRepeatitionLabel.text = "Advertise every ${progress} Seconds"
                    if(_advertisementLoopService != null){
                        _advertisementLoopService!!.setIntervalSeconds(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // currently not in use
                }
            })

            // status label
            val statusLabelcontinuityDevicePopUps: TextView = binding.statusLabelcontinuityDevicePopUps
            _viewModel!!.statusText.observe(viewLifecycleOwner) {
                statusLabelcontinuityDevicePopUps.text = it
            }

            // log scroll view
            val logView: LinearLayout = binding.continuityDevicePopUpsLogLinearView
            _viewModel!!.logEntries.observe(viewLifecycleOwner) {
                logView.removeAllViews()
                it.reversed().map { logEntryModel ->
                    val logEntryTextView: TextView = TextView(logView.context)
                    logEntryTextView.text = logEntryModel.message

                    when (logEntryModel.level){
                        LogLevel.Info -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_info))
                        }
                        LogLevel.Warning -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_warning))
                        }
                        LogLevel.Error -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_error))
                        }
                        LogLevel.Success -> {
                            logEntryTextView.setTextColor(ContextCompat.getColor(logView.context, R.color.log_success))
                        }
                    }

                    logView.addView(logEntryTextView)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAdvertisementStarted() {
        _viewModel!!.setStatusText("Started Advertising")
    }
    override fun onAdvertisementStopped() {
        _viewModel!!.setStatusText("Stopped Advertising")
    }

    override fun onAdvertisementSetStarted(advertisementSet: AdvertisementSet) {
        var message = "Advertising: ${advertisementSet.deviceName}"
        _viewModel!!.setStatusText(message)

        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Info
        logEntry.message = message
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisementSetStopped(advertisementSet: AdvertisementSet) {
        // currently not in use
    }

    override fun onStartFailure(errorCode: Int) {
        var message = ""
        message = if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
            "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
            "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
            "ADVERTISE_FAILED_ALREADY_STARTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
            "ADVERTISE_FAILED_DATA_TOO_LARGE"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
            "ADVERTISE_FAILED_INTERNAL_ERROR"
        } else {
            "unknown"
        }

        if (errorCode != AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED){
            var logEntry = LogEntryModel()
            logEntry.level = LogLevel.Error
            logEntry.message = message
            _viewModel!!.addLogEntry(logEntry)
        }
    }

    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Success
        logEntry.message = "Started advertising successfully"
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisingSetStarted(
        advertisingSet: AdvertisingSet?,
        txPower: Int,
        status: Int
    ) {
        var logEntry = LogEntryModel()
        logEntry.level = LogLevel.Success
        logEntry.message = "Advertised successfully"
        _viewModel!!.addLogEntry(logEntry)
    }

    override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
        // currently not in use
    }

    override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
        // currently not in use
    }

    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
        // currently not in use
    }
}