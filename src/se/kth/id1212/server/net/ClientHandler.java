package se.kth.id1212.server.net;

import common.Command;
import se.kth.id1212.server.model.DTO;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
import static common.Constants.*;

public class ClientHandler implements Runnable {
    private Server server = null;
    private SocketChannel clientChannel;
    private final ByteBuffer msgFromClient = ByteBuffer.allocateDirect(1024);
    private Queue<String> msgToProcess = new ArrayDeque<>();
    private String player = "ANONYMOUS";
    private boolean connected;
    private int score = 0;

    ClientHandler(Server server, SocketChannel clientChannel) {
        this.server = server;
        this.clientChannel = clientChannel;
        connected = true;
    }

    public void run() {
        while (connected && !msgToProcess.isEmpty()) {
            String rawInput;
            while ((rawInput = msgToProcess.peek()) != null) {
                String[] parts = rawInput.split(LENGTH_DELIMITER);
                int lengthHeader = Integer.parseInt(parts[LENGTH_INDEX]);
                msgToProcess.remove(); //Removes the peeked message

                if (lengthHeader != parts[DATA_INDEX].length()) {
                    try {
                        throw new Exception("Length header didn't match");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String input = parts[DATA_INDEX].toUpperCase();
                Command cmd = Command.valueOf(parseCommand(input));
                try {
                    switch (cmd) {
                        case START:
                            if (server.isGameRunning()) {
                                transmitDirectlyToClient(Command.RUNNING.toString());
                                transmitDirectlyToClient(getStateOutput());
                                break;
                            }
                            server.startGame();
                            server.broadcast(Command.START.toString() + DATA_DELIMITER + player + END_OF_MSG_DELIMITER);
                            server.broadcast(getStateOutput());
                            break;
                        case DISCONNECT:
                            disconnect();
                            server.broadcast(Command.DISCONNECT.toString() + DATA_DELIMITER + player + END_OF_MSG_DELIMITER);
                            break;
                        case GUESS:
                            if (!server.isGameRunning()) {
                                transmitDirectlyToClient(Command.NOT_RUNNING.toString());
                            }
                           server.guess(parseLiteral(input));
                            DTO dto = server.getGameStateDTO();

                            if (dto.gameWon()) {
                                server.incrementAllClientsScore();

                            } else if (!dto.gameWon() & dto.getRemainingAttempts() == 0) {
                                server.decrementAllClientsScore();
                            }
                            server.broadcast(Command.GUESS.toString() +
                                    DATA_DELIMITER + player +
                                    DATA_DELIMITER +
                                    parseLiteral(input) +
                                    END_OF_MSG_DELIMITER);
                            server.broadcast(getStateOutput());
                            break;
                        case USER:
                            String playerOld = player;
                            player = parseLiteral(input);

                            server.broadcast(Command.USER.toString() +
                                    DATA_DELIMITER + playerOld +
                                    DATA_DELIMITER + player +
                                    END_OF_MSG_DELIMITER);
                            break;
                        case SCORE:
                            transmitDirectlyToClient(Command.SCORE.toString() + DATA_DELIMITER + score);
                            break;
                        case RULES:
                            transmitDirectlyToClient(Command.RULES.toString() + DATA_DELIMITER + server.getRules());
                            break;
                        default:
                            System.out.println("The input was not known to the ClientHandler");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String parseCommand(String input) {
        return input.split(" ")[0];
    }

    private String parseLiteral(String input) {
        return input.split(" ")[1];
    }

    private String getStateOutput() {
        DTO dto = server.getGameStateDTO();
        return Command.STATE.toString() +
                DATA_DELIMITER + dto.getGameState() +
                DATA_DELIMITER + dto.getRemainingAttempts() +
                DATA_DELIMITER + dto.gameWon() +
                DATA_DELIMITER + dto.getGuessedLetters() +
                DATA_DELIMITER + dto.getNoLetters() +
                END_OF_MSG_DELIMITER;
    }

    private void transmitDirectlyToClient(String update) throws IOException {
        transmitToClient(ByteBuffer.wrap(update.getBytes()));
    }

    void transmitToClient(ByteBuffer msg) throws IOException {
        clientChannel.write(msg);
        if (msg.hasRemaining()) {
            throw new IOException("Message still has remaining packets after sent");
        }
    }

    /**
     * Used to inspect buffers in debug mode.
     * */
    private void bufferSniffer(ByteBuffer msg) {
        ByteBuffer msgCopy = msg.duplicate();
        byte[] bytes = new byte[msgCopy.remaining()];
        msgCopy.get(bytes);
        String s = new String(bytes);
    }

    private void disconnect() {
        try {
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }

    void incrementScore() {
        score++;
    }

    void decrementScore() {
        score--;
    }

    public void receiveMessage() throws IOException {
        msgFromClient.clear();
        int numReadBytes = clientChannel.read(msgFromClient);
        //The number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream
        if (numReadBytes == -1) {
            throw new IOException("ClientInterface has closed connection.");
        }
        String receiveMsg = extractMessageFromBuffer();
        msgToProcess.add(receiveMsg);
        ForkJoinPool.commonPool().execute(this); //IO operation - starts run()
    }

    private String extractMessageFromBuffer() {
        msgFromClient.flip();
        byte[] bytes = new byte[msgFromClient.remaining()];
        msgFromClient.get(bytes);
        return new String(bytes);
    }
}

