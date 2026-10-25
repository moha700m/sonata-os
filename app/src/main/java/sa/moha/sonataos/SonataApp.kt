package sa.moha.sonataos

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import sa.moha.sonataos.data.SettingsRepo
import sa.moha.sonataos.obd.ObdManager
import sa.moha.sonataos.sys.MaintenanceWorker
import sa.moha.sonataos.sys.Notify

class SonataApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var settings: SettingsRepo; private set
    lateinit var obd: ObdManager; private set
    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepo(this)
        obd = ObdManager(this, scope)
        Notify.channels(this)
        runCatching { MaintenanceWorker.enqueue(this) }
    }
}
