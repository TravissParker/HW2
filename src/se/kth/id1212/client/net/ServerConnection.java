package se.kth.id1212.client.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static common.Constants.*;

public class ServerConnection implements Runnable {
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private final ByteBuffer packetsToReceive = ByteBuffer.allocateDirect(1024);
    private final Queue<ByteBuffer> packetsToTransmit = new ArrayDeque<>();
    private final List<Broadcast> clientBroadcasters = new ArrayList<>();
    private String host = "localhost";
    private int port = 9091;
    private boolean connected;
    private volatile boolean transmit = false;
    private final String DISCONNECT = "DISCONNECT";
    private MessageProcessor messageProcessor = new MessageProcessor();

    public void connect(Broadcast broadcaster) {
        // Parameters for serverAddress are hardcoded during development
        serverAddress = new InetSocketAddress(host, port);
        clientBroadcasters.add(broadcaster);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            initConnection();
            initSelector();

            while (connected || !packetsToTransmit.isEmpty()) {
                if (transmit) {
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    transmit = false;
                }
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        completeConnection(key);
                    } else if (key.isReadable()) {
                        receiveFromServer(key);
                    } else if (key.isWritable()) {
                        transmitToServer(key);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Server Connection: exception, server failure");
            e.printStackTrace();
        }
        try {
            clientSideDisconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConnection() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
        connected = true;
    }

    private void initSelector() throws IOException {
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void completeConnection(SelectionKey key) throws IOException {
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);
        Executor pool = ForkJoinPool.commonPool();
        for (Broadcast broadcaster : clientBroadcasters) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    broadcaster.relayFromServer("Someone just connected to the server!");
                }
            });
        }
    }

    private void transmitToServer(SelectionKey key) throws IOException {
        ByteBuffer msg;
        synchronized (packetsToTransmit) {
            while ((msg = packetsToTransmit.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return; //Fixme: remaining?
                }
                packetsToTransmit.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void receiveFromServer(SelectionKey key) throws Exception {    //throws IOException
        packetsToReceive.clear(); //Clears positioning and markers, doesn't clear content
        int numReadBytes = socketChannel.read(packetsToReceive);   //Read from the channel to the buffer
        if (numReadBytes == -1) {
            throw new Exception("Notice: numReadBytes == -1");
        }

        String[] unprocessedMsg = extractMessageFromBuffer();
        List<String> processedMsg = new ArrayList<>();
        for (String s : unprocessedMsg) {
            processedMsg.add(messageProcessor.processMsg(s));
        }
        //Question: are we creating a new thread with this as an argument?
        Executor pool = ForkJoinPool.commonPool();
        for (Broadcast broadcaster : clientBroadcasters) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    for (String s : processedMsg) {
                        broadcaster.relayFromServer(s);
                    }
                }
            });
        }
    }

    private String[] extractMessageFromBuffer() {
        packetsToReceive.flip();
        byte[] bytes = new byte[packetsToReceive.remaining()];
        packetsToReceive.get(bytes);
        return new String(bytes).split(END_OF_MSG_DELIMITER);
    }

    public void username(String name) {
        transmitToServer(common.Command.USER.toString(), name);
    }
    public void startGame() {
        transmitToServer(common.Command.START.toString());
    }
    public void guess(String lit) {
        transmitToServer(common.Command.GUESS.toString(), lit);
    }
    public void getScore() {
        transmitToServer(common.Command.SCORE.toString());
    }
    public void getRules() {
        transmitToServer(common.Command.RULES.toString());
    }

    public void disconnect() throws IOException {
        connected = false;
        transmitToServer(DISCONNECT);
    }

    private void clientSideDisconnect() throws IOException {
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
    }

    private void transmitToServer(String... data) {
        StringJoiner sj = new StringJoiner(WORD_DELIMITER);
        for (String d: data)
            sj.add(d);
        String packet = Integer.toString(sj.length()) + LENGTH_DELIMITER + sj.toString();
        packetsToTransmit.add(ByteBuffer.wrap(packet.getBytes()));

        transmit = true;
        selector.wakeup();
    }
}
