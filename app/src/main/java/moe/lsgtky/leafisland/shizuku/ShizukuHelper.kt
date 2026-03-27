package moe.lsgtky.leafisland.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import moe.lsgtky.leafisland.IPrivilegedService
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    private var privilegedService: IPrivilegedService? = null
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("moe.lsgtky.leafisland", PrivilegedServiceImpl::class.java.name)
    ).daemon(false).processNameSuffix("privileged")

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            privilegedService = IPrivilegedService.Stub.asInterface(service)
            Log.d(TAG, "Privileged service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            privilegedService = null
            Log.d(TAG, "Privileged service disconnected")
        }
    }

    fun ensureServiceBound() {
        if (privilegedService == null && isAvailable() && hasPermission()) {
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind user service: ${e.message}")
            }
        }
    }

    fun blockNetwork(uid: Int): Boolean {
        ensureServiceBound()
        val service = privilegedService ?: return false
        return service.setPackageNetworkingEnabled(uid, false)
    }

    fun unblockNetwork(uid: Int) {
        privilegedService?.setPackageNetworkingEnabled(uid, true)
    }
}
