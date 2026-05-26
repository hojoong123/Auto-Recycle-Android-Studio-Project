package com.capstone.recyclehelper

import android.util.Log
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient

object StompManager {

    private const val WS_URL = "ws://192.168.0.116:8080/ws"
    private var client: StompClient? = null
    private val disposables = CompositeDisposable()

    fun connect(adminId: Long, onNotification: (NotificationDto) -> Unit) {
        disconnect()
        val c = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)
        client = c

        disposables.add(
            c.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event -> Log.d("STOMP", "lifecycle: ${event.type}") }
        )

        disposables.add(
            c.topic("/topic/notifications/$adminId")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { msg ->
                        val dto = Gson().fromJson(msg.payload, NotificationDto::class.java)
                        onNotification(dto)
                    },
                    { Log.e("STOMP", "err", it) }
                )
        )

        c.connect()
    }

    fun disconnect() {
        disposables.clear()
        client?.disconnect()
        client = null
    }
}

