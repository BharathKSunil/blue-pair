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
package co.aurasphere.bluepair

import android.app.ProgressDialog
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import co.aurasphere.bluepair.bluetooth.BluetoothController
import co.aurasphere.bluepair.view.DeviceRecyclerViewAdapter
import co.aurasphere.bluepair.view.ListInteractionListener
import co.aurasphere.bluepair.view.RecyclerViewProgressEmptySupport
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Main Activity of this application.
 *
 * @author Donato Rimenti
 */
class MainActivity : AppCompatActivity(), ListInteractionListener<BluetoothDevice?> {
    /**
     * The controller for Bluetooth functionalities.
     */
    private var bluetooth: BluetoothController? = null

    /**
     * The Bluetooth discovery button.
     */
    private var fab: FloatingActionButton? = null

    /**
     * Progress dialog shown during the pairing process.
     */
    private var bondingProgressDialog: ProgressDialog? = null

    /**
     * Adapter for the recycler view.
     */
    private var recyclerViewAdapter: DeviceRecyclerViewAdapter? = null
    private var recyclerView: RecyclerViewProgressEmptySupport? = null
    private val adContainer: LinearLayout? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Changes the theme back from the splashscreen. It's very important that this is called
        // BEFORE onCreate.
        SystemClock.sleep(resources.getInteger(R.integer.splashscreen_duration).toLong())
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)


        // Sets up the RecyclerView.
        recyclerViewAdapter = DeviceRecyclerViewAdapter(this)
        recyclerView = findViewById<View>(R.id.list) as RecyclerViewProgressEmptySupport
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        // Sets the view to show when the dataset is empty. IMPORTANT : this method must be called
        // before recyclerView.setAdapter().
        val emptyView = findViewById<View>(R.id.empty_list)
        recyclerView!!.setEmptyView(emptyView)

        // Sets the view to show during progress.
        val progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        recyclerView!!.setProgressView(progressBar)
        recyclerView!!.adapter = recyclerViewAdapter

        // [#11] Ensures that the Bluetooth is available on this device before proceeding.
        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            val dialog = AlertDialog.Builder(this@MainActivity).create()
            dialog.setTitle(getString(R.string.bluetooth_not_available_title))
            dialog.setMessage(getString(R.string.bluetooth_not_available_message))
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, which -> // Closes the dialog and terminates the activity.
                dialog.dismiss()
                finish()
            }
            dialog.setCancelable(false)
            dialog.show()
        }

        // Sets up the bluetooth controller.
        recyclerViewAdapter?.let { bluetooth = BluetoothController(this, BluetoothAdapter.getDefaultAdapter(), it) }
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab!!.setOnClickListener { view ->
            // If the bluetooth is not enabled, turns it on.
            if (!bluetooth!!.isBluetoothEnabled) {
                Snackbar.make(view, R.string.enabling_bluetooth, Snackbar.LENGTH_SHORT).show()
                bluetooth!!.turnOnBluetoothAndScheduleDiscovery()
            } else {
                //Prevents the user from spamming the button and thus glitching the UI.
                if (!bluetooth!!.isDiscovering) {
                    // Starts the discovery.
                    Snackbar.make(view, R.string.device_discovery_started, Snackbar.LENGTH_SHORT).show()
                    bluetooth!!.startDiscovery()
                } else {
                    Snackbar.make(view, R.string.device_discovery_stopped, Snackbar.LENGTH_SHORT).show()
                    bluetooth!!.cancelDiscovery()
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_about) {
            showAbout()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Creates the about popup.
     */
    private fun showAbout() {
        // Inflate the about message contents
        val messageView = layoutInflater.inflate(R.layout.about, null, false)
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setTitle(R.string.app_name)
        builder.setView(messageView)
        builder.create()
        builder.show()
    }
    /**
     * {@inheritDoc}
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onItemClick(item: BluetoothDevice?) {
        Log.d(TAG, "Item clicked : " + BluetoothController.deviceToString(item))
        item?.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onPhyUpdate: status = $status; txPhy = $txPhy; rxPhy = $rxPhy")
            }

            override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyRead(gatt, txPhy, rxPhy, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onPhyRead: status = $status; txPhy = $txPhy; rxPhy = $rxPhy")
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onConnectionStateChange: status = $status; newState = $newState")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "onConnectionStateChange: Discovering Services")
                        gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "onConnectionStateChange: closing Gatt")
                        gatt?.close()
                    }
                } else {
                    status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED
                    gatt?.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.w(TAG, "onServicesDiscovered: status = $status; services = ${gatt?.services?.size}")
                for (service in gatt!!.services) {
                    Log.i(TAG, "onServicesDiscovered: service = ${service.uuid}")
                    for (char in service.characteristics) {
                        Log.d(TAG, "onServicesDiscovered: Char = ${char.uuid}")
                    }
                }
                Log.w(TAG, "onServicesDiscovered: Closing GATT")
                gatt.close()
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onCharacteristicRead: status = $status; characteristic = $characteristic")
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onCharacteristicWrite: status = $status; characteristic = $characteristic")
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicChanged(gatt, characteristic)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onCharacteristicChanged: characteristic = $characteristic")
            }

            override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorRead(gatt, descriptor, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onDescriptorRead: status = $status; descriptor = $descriptor")
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onDescriptorWrite: status = $status; descriptor = $descriptor")
            }

            override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                super.onReliableWriteCompleted(gatt, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onReliableWriteCompleted: status = $status")
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onReadRemoteRssi: status = $status; rssi = $rssi")
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                val TAG = "MainActivity[${gatt?.device?.name}: ${gatt?.device?.address}]"
                Log.i(TAG, "onMtuChanged: status = $status; mtu = $mtu")
            }
        })
        /*if (item == null || bluetooth!!.isAlreadyPaired(item)) {
            Log.d(TAG, "Device already paired!")
            Toast.makeText(this, R.string.device_already_paired, Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Device not paired. Pairing.")
            val outcome = bluetooth!!.pair(item)

            // Prints a message to the user.
            val deviceName = BluetoothController.getDeviceName(item)
            if (outcome) {
                // The pairing has started, shows a progress dialog.
                Log.d(TAG, "Showing pairing dialog")
                bondingProgressDialog = ProgressDialog.show(this, "", "Pairing with device $deviceName...", true, false)
            } else {
                Log.d(TAG, "Error while pairing with device $deviceName!")
                Toast.makeText(this, "Error while pairing with device $deviceName!", Toast.LENGTH_SHORT).show()
            }
        }*/
    }

    /**
     * {@inheritDoc}
     */
    override fun startLoading() {
        recyclerView!!.startLoading()

        // Changes the button icon.
        fab!!.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp)
    }

    /**
     * {@inheritDoc}
     */
    override fun endLoading(partialResults: Boolean) {
        recyclerView!!.endLoading()

        // If discovery has ended, changes the button icon.
        if (!partialResults) {
            fab!!.setImageResource(R.drawable.ic_bluetooth_white_24dp)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun endLoadingWithDialog(error: Boolean, element: BluetoothDevice?) {
        if (bondingProgressDialog != null) {
            val view = findViewById<View>(R.id.main_content)
            val message: String
            val deviceName = BluetoothController.getDeviceName(element)

            // Gets the message to print.
            message = if (error) {
                "Failed pairing with device $deviceName!"
            } else {
                "Succesfully paired with device $deviceName!"
            }

            // Dismisses the progress dialog and prints a message to the user.
            bondingProgressDialog!!.dismiss()
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()

            // Cleans up state.
            bondingProgressDialog = null
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        bluetooth!!.close()
        super.onDestroy()
    }

    /**
     * {@inheritDoc}
     */
    override fun onRestart() {
        super.onRestart()
        // Stops the discovery.
        if (bluetooth != null) {
            bluetooth!!.cancelDiscovery()
        }
        // Cleans the view.
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.cleanView()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        super.onStop()
        // Stoops the discovery.
        if (bluetooth != null) {
            bluetooth!!.cancelDiscovery()
        }
    }

    companion object {
        /**
         * Tag string used for logging.
         */
        private const val TAG = "MainActivity"
    }
}