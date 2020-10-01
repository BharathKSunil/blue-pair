/*
 * MIT License
 * <p>
 * Copyright (c) 2017 Donato Rimenti
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package co.aurasphere.bluepair.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.Closeable

/**
 * Class for handling Bluetooth connection.
 *
 * @author Donato Rimenti
 */
class BluetoothController(
        /**
         * The activity which is using this controller.
         */
        private val context: Activity,
        /**
         * Interface for Bluetooth OS services.
         */
        private val bluetoothAdapter: BluetoothAdapter?, listener: BluetoothDiscoveryDeviceListener) : Closeable {
    /**
     * Class used to handle communication with OS about Bluetooth system events.
     */
    private val broadcastReceiverDelegator: BroadcastReceiverDelegator = BroadcastReceiverDelegator(context, listener, this)

    /**
     * Used as a simple way of synchronization between turning on the Bluetooth and starting a
     * device discovery.
     */
    private var bluetoothDiscoveryScheduled = false
    /**
     * Gets the currently bounding device.
     *
     * @return the [.boundingDevice].
     */

    /**
     * Used as a temporary field for the currently bounding device. This field makes this whole
     * class not Thread Safe.
     */
    var boundingDevice: BluetoothDevice? = null
        private set

    /**
     * Checks if the Bluetooth is already enabled on this device.
     *
     * @return true if the Bluetooth is on, false otherwise.
     */
    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter!!.isEnabled

    /**
     * Starts the discovery of new Bluetooth devices nearby.
     */
    fun startDiscovery() {
        broadcastReceiverDelegator.onDeviceDiscoveryStarted()

        // This line of code is very important. In Android >= 6.0 you have to ask for the runtime
        // permission as well in order for the discovery to get the devices ids. If you don't do
        // this, the discovery won't find any device.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Tries to start the discovery. If the discovery returns false, this means that the
        // bluetooth has not started yet.
        Log.d(TAG, "Bluetooth starting discovery.")
        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(context, "Error while starting device discovery!", Toast.LENGTH_SHORT)
                    .show()
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?")

            // Ends the discovery.
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }

    /**
     * Turns on the Bluetooth.
     */
    fun turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.")
        broadcastReceiverDelegator.onBluetoothTurningOn()
        bluetoothAdapter!!.enable()
    }

    /**
     * Performs the device pairing.
     *
     * @param device the device to pair with.
     * @return true if the pairing was successful, false otherwise.
     */
    fun pair(device: BluetoothDevice): Boolean {
        // Stops the discovery and then creates the pairing.
        if (bluetoothAdapter!!.isDiscovering) {
            Log.d(TAG, "Bluetooth cancelling discovery.")
            bluetoothAdapter.cancelDiscovery()
        }
        Log.d(TAG, "Bluetooth bonding with device: " + deviceToString(device))
        val outcome = device.createBond()
        Log.d(TAG, "Bounding outcome : $outcome")

        // If the outcome is true, we are bounding with this device.
        if (outcome == true) {
            boundingDevice = device
        }
        return outcome
    }

    /**
     * Checks if a device is already paired.
     *
     * @param device the device to check.
     * @return true if it is already paired, false otherwise.
     */
    fun isAlreadyPaired(device: BluetoothDevice?): Boolean {
        return bluetoothAdapter!!.bondedDevices.contains(device)
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        broadcastReceiverDelegator.close()
    }

    /**
     * Checks if a deviceDiscovery is currently running.
     *
     * @return true if a deviceDiscovery is currently running, false otherwise.
     */
    val isDiscovering: Boolean
        get() = bluetoothAdapter!!.isDiscovering

    /**
     * Cancels a device discovery.
     */
    fun cancelDiscovery() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery()
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }

    /**
     * Turns on the Bluetooth and executes a device discovery when the Bluetooth has turned on.
     */
    fun turnOnBluetoothAndScheduleDiscovery() {
        bluetoothDiscoveryScheduled = true
        turnOnBluetooth()
    }

    /**
     * Called when the Bluetooth status changed.
     */
    fun onBluetoothStatusChanged() {
        // Does anything only if a device discovery has been scheduled.
        if (bluetoothDiscoveryScheduled) {
            when (bluetoothAdapter!!.state) {
                BluetoothAdapter.STATE_ON -> {
                    // Bluetooth is ON.
                    Log.d(TAG, "Bluetooth succesfully enabled, starting discovery")
                    startDiscovery()
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false
                }
                BluetoothAdapter.STATE_OFF -> {
                    // Bluetooth is OFF.
                    Log.d(TAG, "Error while turning Bluetooth on.")
                    Toast.makeText(context, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT).show()
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false
                }
                else -> {
                }
            }
        }
    }// If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.

    /**
     * Returns the status of the current pairing and cleans up the state if the pairing is done.
     *
     * @return the current pairing status.
     * @see BluetoothDevice.getBondState
     */
    val pairingDeviceStatus: Int
        get() {
            checkNotNull(boundingDevice) { "No device currently bounding" }
            val bondState = boundingDevice!!.bondState
            // If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.
            if (bondState != BluetoothDevice.BOND_BONDING) {
                boundingDevice = null
            }
            return bondState
        }

    /**
     * Gets the name of the currently pairing device.
     *
     * @return the name of the currently pairing device.
     */
    val pairingDeviceName: String?
        get() = getDeviceName(boundingDevice)

    /**
     * Returns if there's a pairing currently being done through this app.
     *
     * @return true if a pairing is in progress through this app, false otherwise.
     */
    val isPairingInProgress: Boolean
        get() = boundingDevice != null

    companion object {
        /**
         * Tag string used for logging.
         */
        private const val TAG = "BluetoothManager"

        /**
         * Converts a BluetoothDevice to its String representation.
         *
         * @param device the device to convert to String.
         * @return a String representation of the device.
         */
        @JvmStatic
        fun deviceToString(device: BluetoothDevice?): String {
            return "[Address: " + device!!.address + ", Name: " + device.name + "]"
        }

        /**
         * Gets the name of a device. If the device name is not available, returns the device address.
         *
         * @param device the device whose name to return.
         * @return the name of the device or its address if the name is not available.
         */
        @JvmStatic
        fun getDeviceName(device: BluetoothDevice?): String? {
            var deviceName = device!!.name
            if (deviceName == null) {
                deviceName = device.address
            }
            return deviceName
        }
    }

}