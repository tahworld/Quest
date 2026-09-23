package com.vika.quest

import android.app.Application
import com.vika.quest.di.AppContainer

class QuestApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
