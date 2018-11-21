package se.kth.id1212.server.net;

import se.kth.id1212.server.model.DTO;
import se.kth.id1212.server.model.Game;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class Server {
    private int portNo = 9091;
    private Selector selector;
    private ServerSocketChannel listeningSocketChannel;
    private final Queue<ByteBuffer> packetsToBroadcast = new ArrayDeque<>();
    private boolean broadcastTime = false;
    private GameInterface gameInterface = new GameInterface();

    private Server() {}

    private void serve() {
        try {
            initSelector();
            initListeningSocketChannel();

            while (true) {
                if (broadcastTime) {
                    setWriteInterestForAllClients();
                    appendBroadcastMsgToAllClientQueues();
                    broadcastTime = false;
                }
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        startHandler(key);
                    } else if (key.isReadable()) {
                        receiveFromClient(key);
                    } else if (key.isWritable()) {
                        transmitToClient(key);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Server-exception");
            e.printStackTrace();
        }
    }

    private void initSelector() throws IOException {
        selector = Selector.open();
    }

    private void initListeningSocketChannel() throws IOException {
        listeningSocketChannel = ServerSocketChannel.open();
        listeningSocketChannel.configureBlocking(false);
        listeningSocketChannel.bind(new InetSocketAddress(portNo));
        listeningSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void startHandler(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        ClientHandler handler = new ClientHandler(this, clientChannel);
        clientChannel.register(selector, SelectionKey.OP_WRITE, new ClientInterface(handler));
    }

    private void setWriteInterestForAllClients() {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void appendBroadcastMsgToAllClientQueues() {
        synchronized (packetsToBroadcast) {
            ByteBuffer msg;
            while ((msg = packetsToBroadcast.poll()) != null) {
                for (SelectionKey key : selector.keys()) {
                    ClientInterface client = (ClientInterface) key.attachment();
                    if (client == null) {
                        continue;
                    }
                    synchronized (client.messagesToTransmit) {
                        client.queueOutgoingMsg(msg);
                    }
                }
            }
        }
    }

    private void transmitToClient(SelectionKey key) {
        ClientInterface client = (ClientInterface) key.attachment();
        try {
            client.transmitAll();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFromClient(SelectionKey key) {
        ClientInterface client = (ClientInterface) key.attachment();
        try {
            client.handler.receiveMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void broadcast(String msg) {
        broadcastTime = true;
        ByteBuffer message = ByteBuffer.wrap(msg.getBytes());
        synchronized (packetsToBroadcast) {
            packetsToBroadcast.add(message);
        }
        selector.wakeup();
    }

    void decrementAllClientsScore() {
        gameInterface.stopGame();
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                ClientInterface client = (ClientInterface) key.attachment();
                client.decrementScore();
            }
        }
    }

    void incrementAllClientsScore() {
        gameInterface.stopGame();
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                ClientInterface client = (ClientInterface) key.attachment();
                client.incrementScore();
            }
        }
    }

    private class ClientInterface {
        private final ClientHandler handler;
        private final Queue<ByteBuffer> messagesToTransmit = new ArrayDeque<>();

        ClientInterface(ClientHandler handler) {
            this.handler = handler;
        }

        private void queueOutgoingMsg(ByteBuffer msg) {
            synchronized (messagesToTransmit) {
                messagesToTransmit.add(msg.duplicate());
            }
        }

        private void transmitAll() throws IOException {
            ByteBuffer msg = null;
            synchronized (messagesToTransmit) {
                while ((msg = messagesToTransmit.peek()) != null) {
                    handler.transmitToClient(msg);
                    // If an exception is thrown then we retreat without removing message.
                    messagesToTransmit.remove();
                }
            }
        }

        private void incrementScore() {
            handler.incrementScore();
        }

        private void decrementScore() {
            handler.decrementScore();
        }
    }

    /*
     * Game related operations
     * */

    void startGame() throws Exception {
        if (!gameInterface.isGameRunning()) {
            gameInterface.start();
        } else {
            throw new Exception("A game is already running");
        }
    }

    void guess(String lit) {
        gameInterface.guess(lit);
    }

    private void stopGame() {
        gameInterface.stopGame();
    }

    boolean isGameRunning() {
        return gameInterface.isGameRunning();
    }

    DTO getGameStateDTO() {
        return gameInterface.getGameStateDTO();
    }

    String getRules() {
        return gameInterface.getRules();
    }

    private class GameInterface {
        private Game game = new Game();
        private boolean gameRunning = false;

        private void start() throws Exception {
            game.start();
            gameRunning = true;
        }

        private void guess(String lit) {
            game.makeGuess(lit);
        }

        private void stopGame() {
            gameRunning = false;
        }

        boolean isGameRunning() {
            return gameRunning;
        }

        private DTO getGameStateDTO() {
            return game.getDTO();
        }

        private String getRules() {
            return game.getRules();
        }
    }

    public static void main(String[] args) {
        new Server().serve();
    }
}
