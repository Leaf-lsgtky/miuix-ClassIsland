package moe.lsgtky.leafisland.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import moe.lsgtky.leafisland.INetworkBlockerService
import rikka.shizuku.Shizuku

object ShizukuHelper {

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

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    /**
     * Bind the NetworkBlockerService, execute [block], then unbind.
     * Must be called from a background thread.
     */
    fun withBlockerService(block: (INetworkBlockerService) -> Unit) {
        val latch = java.util.concurrent.CountDownLatch(1)
        var service: INetworkBlockerService? = null

        val args = Shizuku.UserServiceArgs(
            ComponentName(
                "moe.lsgtky.leafisland",
                NetworkBlockerService::class.java.name,
            )
        ).processNameSuffix("network_blocker")

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = INetworkBlockerService.Stub.asInterface(binder)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }

        Shizuku.bindUserService(args, connection)

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            val svc = service ?: throw IllegalStateException("Failed to bind NetworkBlockerService")
            block(svc)
        } finally {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Exception) { }
        }
    }
}
