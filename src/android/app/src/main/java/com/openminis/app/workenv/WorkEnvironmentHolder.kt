package com.openminis.app.workenv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object WorkEnvironmentHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var manager: DesktopConnectionManager? = null

    fun store(context: Context): WorkEnvironmentStore = WorkEnvironmentStore.get(context)

    fun connection(context: Context): DesktopConnectionManager {
        manager?.let { return it }
        synchronized(this) {
            manager?.let { return it }
            val created = DesktopConnectionManager(context, store(context), scope)
            manager = created
            created.start()
            return created
        }
    }
}
