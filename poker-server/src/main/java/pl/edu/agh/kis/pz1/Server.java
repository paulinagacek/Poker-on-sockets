package pl.edu.agh.kis.pz1;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private final ServerSocket serverSocket; // responsible for handling communication
    private static int nrOfPlayers = ClientHandler.clientHandlers.size();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    public static int requiredPlayers = 0;

    /**
     * Keeps server running while server socket is not closed
     */
    private void startServer(int requiredNrOfPlayers){
        try{
            Game game = new Game();
            game.tie.getDeck().displayDeck();
            System.out.println("Poker server is Running...");
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept(); // returns socket to specific Client
                if(nrOfPlayers < requiredNrOfPlayers){
                    ClientHandler clientHandler = new ClientHandler(socket, game);
                    System.out.println("A new player has joined the game!");
                    updateNrOfPlayers();
                    System.out.println("Now there are "+ nrOfPlayers + " players in game");
                    waitForMorePlayers(clientHandler);
                    Thread thread = new Thread(clientHandler);
                    thread.start();
                    if(nrOfPlayers==requiredNrOfPlayers){
                        game.play();
                    }
                }
                else{
                    System.out.println("Nr of players exceeded!");
                }
            }
        }
        catch(IOException e){
            closeServerSocket();
        }
    }

    private void closeServerSocket(){
        try{
            if(serverSocket != null){
                serverSocket.close();
            }
        }
        catch(IOException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter nr of players: ");
        requiredPlayers = scanner.nextInt();
        server.startServer(requiredPlayers);
    }

    //game
    public int getNrOfPlayers(){return nrOfPlayers;}
    private void updateNrOfPlayers(){
        nrOfPlayers = ClientHandler.clientHandlers.size();
    }
    private void waitForMorePlayers(ClientHandler clientHandler) {
        if(nrOfPlayers<requiredPlayers)
            clientHandler.broadcastMessageToAll("We are still waiting for " + (requiredPlayers-nrOfPlayers)+ " players...");
        else if(nrOfPlayers==requiredPlayers)
            clientHandler.broadcastMessageToAll("All players joined, lets start the game!");
    }
}