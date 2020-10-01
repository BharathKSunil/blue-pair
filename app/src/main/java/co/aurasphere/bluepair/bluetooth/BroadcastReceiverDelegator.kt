/**
 * MIT License
 *
 *
 * Copyright (c) 2017 Donato Rimenti
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package co.aurasphere.bluepair.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.Closeable

/**
 * Class used to handle communication with the OS about Bluetooth system events.
 *
 * @author Donato Rimenti
 */
class BroadcastReceiverDelegator(
        /**
         * The context of this object.
         */
        private val context: Context,
        /**
         * Callback for Bluetooth events.
         */
        private val listener: BluetoothDiscoveryDeviceListener, bluetooth: BluetoothController?) : BroadcastReceiver(), Closeable {
    /**
     * Tag string used for logging.
     */
    private val TAG = "BroadcastReceiver"

    /**
     * {@inheritDoc}
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Incoming intent : $action")
        when (action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Device discovered! " + BluetoothController.deviceToString(device))
                listener.onDeviceDiscovered(device)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                // Discovery has ended.
                Log.d(TAG, "Discovery ended.")
                listener.onDeviceDiscoveryEnd()
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                // Discovery state changed.
                Log.d(TAG, "Bluetooth state changed.")
                listener.onBluetoothStatusChanged()
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                // Pairing state has changed.
                Log.d(TAG, "Bluetooth bonding state changed.")
                listener.onDevicePairingEnded()
            }
            else -> {
            }
        }
    }

    /**
     * Called when device discovery starts.
     */
    fun onDeviceDiscoveryStarted() {
        listener.onDeviceDiscoveryStarted()
    }

    /**
     * Called when device discovery ends.
     */
    fun onDeviceDiscoveryEnd() {
        listener.onDeviceDiscoveryEnd()
    }

    /**
     * Called when the Bluetooth has been enabled.
     */
    fun onBluetoothTurningOn() {
        listener.onBluetoothTurningOn()
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        context.unregisterReceiver(this)
    }

    /**
     * Instantiates a new BroadcastReceiverDelegator.
     *
     * @param context   the context of this object.
     * @param listener  a callback for handling Bluetooth events.
     * @param bluetooth a controller for the Bluetooth.
     */
    init {
        listener.setBluetoothController(bluetooth)

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(this, filter)
    }
}