package com.khpark.pool.thread.processor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.khpark.event.Job;
import com.khpark.event.NIOEvent;
import com.khpark.pool.buffer.ByteBufferPool;
import com.khpark.pool.manager.PoolManager;
import com.khpark.queue.ChattingRoom;
import com.khpark.queue.Queue;

public class ReadWriteProcessor extends Thread {
	private Queue queue = null;

	public ReadWriteProcessor(Queue queue) {
		this.queue = queue;
	}

	public void run() {
		try {

			while (!Thread.currentThread().isInterrupted()) {
				Job job = queue.pop(NIOEvent.READ_EVENT);
				SelectionKey key = (SelectionKey) job.getSession().get("SelectionKey");
				SocketChannel sc = (SocketChannel) key.channel();

				try {
					broadcast(sc);
				} catch (IOException e) {
					closeChannel(sc);
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	private void broadcast(SocketChannel sc) throws IOException {
		ByteBufferPool bufferPool = PoolManager.getByteBufferPool();
		ByteBuffer buffer = null;

		try {
			buffer = bufferPool.getMemoryBuffer();

			for (int i = 0; i < 2; i++) {
				sc.read(buffer);
			}

			buffer.flip();
			Iterator<SocketChannel> iter = ChattingRoom.getInstance().iterator();

			while (iter.hasNext()) {
				SocketChannel member = (SocketChannel) iter.next();

				if (member != null && member.isConnected()) {

					while (buffer.hasRemaining()) {
						member.write(buffer);
					}
					buffer.rewind();
				}
			}
		} finally {
			bufferPool.putBuffer(buffer);
		}
	}

	private void closeChannel(SocketChannel sc) {
		try {
			sc.close();
			ChattingRoom.getInstance().remove(sc);
		} catch (IOException e) {
		}
	}

}
