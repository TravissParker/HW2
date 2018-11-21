package se.kth.id1212.client.view;

import java.io.IOException;
import java.util.Scanner;

import common.Command;
import se.kth.id1212.client.net.Broadcast;
import se.kth.id1212.client.net.ServerConnection;

import static common.Constants.LENGTH_DELIMITER;
import static common.Constants.WORD_DELIMITER;

public class Interpreter implements Runnable {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private SynchronizedStdOut printer = new SynchronizedStdOut();
    private boolean running = false;
    //Fixme: these should be parsed from user input, hardcoded here during development
    private final String host = "localhost";
    private final int port = 9091;
    private ServerConnection serverConnection;
    private final String WELCOME_MESSAGE = "Welcome to the Hangman Game!" +
            "\nAt any time you can type HELP to see a list of instructions, " +
            "if you feel you need it.";
    private final String COMMANDS_DESCRIPTION =
            "    CONNECT: connects you to the server.\n" +
                    "    DISCONNECT: disconnects you from the server.\n" +
                    "    USER [name]: change your screen name. \n" +
                    "    START: start a new game.\n" +
                    "    GUESS [letter|word]: make a guess, letter or whole word.\n" +
                    "    RULES: shows the rules of the game.\n" +
                    "    SCORE: shows your score.\n";

    /**
    * Starts up a new interpreter thread.
    * */
    public void start() {
        // Guards that nothing will happen if start is called on running interpreter.
        if (running) {
            return;
        }
        printer.println(WELCOME_MESSAGE);
        serverConnection = new ServerConnection();
        running = true;
        new Thread(this).start();
    }
    /**
     * The code that is run in the thread. The thread will spend its life here.
     * Listens to user input.
     * */
    @Override
    public void run() {
        while (running) {
            try {
                String input = readNextLine().toUpperCase().trim();;
                input = input.replace(LENGTH_DELIMITER, "");
                Command cmd = Command.valueOf(parseCommand(input));
                switch(cmd) {
                    case CONNECT:
                        serverConnection.connect(new Broadcaster());
                        break;
                    case DISCONNECT:
                        serverConnection.disconnect();
                        break;
                    case USER:
                        serverConnection.username(parseLiteral(input));
                        break;
                    case START:
                        serverConnection.startGame();
                        break;
                    case GUESS:
                    serverConnection.guess(parseLiteral(input));
                    break;
                    case RULES:
                        serverConnection.getRules();
                        break;
                    case SCORE:
                        serverConnection.getScore();
                        break;
                    case HELP:
                        System.out.println(COMMANDS_DESCRIPTION);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
            catch (IllegalArgumentException e) {
                printer.println("That is not a known command, type HELP to see a list of instructions.");
            }
            catch (ArrayIndexOutOfBoundsException e) {
                printer.println("Arguments where missing, type HELP to see a list of instructions.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readNextLine() {
        System.out.print(PROMPT);
        return console.nextLine();
    }

    private String parseCommand(String input) {
        return input.split(WORD_DELIMITER)[0];
    }

    private String parseLiteral(String input) {
        return input.split(WORD_DELIMITER)[1];
    }

    private class Broadcaster implements Broadcast {
        public void relayFromServer(String trans) {
            printer.println(trans);
            printer.print(PROMPT);
        }
    }
}
