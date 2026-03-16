package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.network.pitch.PitchFrame
import com.couchraoke.tv.domain.network.pitch.PitchFrameCodec
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class UdpPitchReceiver(
    private val onFrame: (PitchFrame) -> Unit,
) {
    private val activeSongInstanceSeq = AtomicLong(-1L)
    private val playerConnections = ConcurrentHashMap<Int, Int>()
    private val lastAcceptedTvTimeMs = ConcurrentHashMap<Int, Int>()
    
    @Volatile
    private var socket: DatagramSocket? = null
    
    @Volatile
    private var running = false
    private var receiveThread: Thread? = null

    fun bind(port: Int) {
        val s = DatagramSocket(port)
        socket = s
        running = true
        receiveThread = Thread {
            val buffer = ByteArray(16)
            val packet = DatagramPacket(buffer, buffer.size)
            while (running) {
                try {
                    s.receive(packet)
                    // We must use copyOf because the buffer is reused
                    processPacket(packet.data.copyOf(packet.length))
                } catch (e: SocketException) {
                    if (running) {
                        e.printStackTrace()
                    }
                    break
                } catch (e: Exception) {
                    if (running) {
                        e.printStackTrace()
                    }
                }
            }
        }.apply {
            name = "UdpPitchReceiver-Thread"
            isDaemon = true
            start()
        }
    }

    fun setActiveSong(instanceSeq: Long) {
        activeSongInstanceSeq.set(instanceSeq)
    }

    fun setPlayerConnection(playerId: Int, connectionId: Int) {
        playerConnections[playerId] = connectionId
    }

    fun stop() {
        running = false
        socket?.close()
        socket = null
        receiveThread?.interrupt()
        receiveThread = null
    }

    internal fun processPacket(data: ByteArray) {
        val frame = PitchFrameCodec.decode(data) ?: return
        
        val expectedConnectionId = playerConnections[frame.playerId] ?: return
        if (expectedConnectionId != frame.connectionId) return
        
        if (frame.songInstanceSeq != activeSongInstanceSeq.get()) return
        
        val lastTvTime = lastAcceptedTvTimeMs[frame.playerId]
        if (lastTvTime != null && (lastTvTime - frame.tvTimeMs) > 200) {
            return
        }
        
        lastAcceptedTvTimeMs[frame.playerId] = frame.tvTimeMs
        onFrame(frame)
    }
}
