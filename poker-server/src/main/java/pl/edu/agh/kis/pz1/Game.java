package pl.edu.agh.kis.pz1;

import java.io.IOException;
import java.util.ArrayList;
public class Game {
    private ClientHandler currentPlayer;
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); // clients connected to socket
    public static ArrayList<ClientHandler> playersInGame = new ArrayList<>(); // clients in game
    public Tie tie = new Tie();
    int WAIT = 1, PASS = 2, RAISE = 3;

    public void play() throws IOException {
        initPlayersInGameArray();
        currentPlayer = clientHandlers.get(0);
        collectAnte();
        dealOutInitialCards();
        tie.getDeck().displayDeck();
        displayCards();

        // 1st betting
        /*
        if(!tie.isGameOver()){
            handleBetting();
            System.out.println("first round");
            while (!checkIfRoundIsComplete() && !tie.isGameOver()) {
                handleBetting();
            }
        }*/
        handleSwapping();
        System.out.println("Finish");
    }

    private void handleBetting() throws IOException {
        for(int i = 0; i < playersInGame.size(); ++i){
            checkIfGameOver();
            showWhoseTurn();
            currentPlayer.bufferedWriter.flush();
            currentPlayer.broadcastMessageToItself("\nEntrance pool: "+tie.getPoolInCurrentBetting()+ "\n"+
                    "What do you want to do?\n(1) Wait\n(2) Pass\n(3) Raise the stakes");

            int move = currentPlayer.decideWhatToDo();
            handleMove(move);
            while(!isMovePossible(move)){
                currentPlayer.broadcastMessageToItself("Your move is invalid, do sth else");
                move = currentPlayer.decideWhatToDo();
                handleMove(move);
            }
            if (move==PASS){
                i--;
            }
            thankUNext();
            updatePlayersArray();
            currentPlayer.broadcastMessageToAll(displayPlayersInGame());
            if(checkIfGameOver()){
                break;
            }
        }
        currentPlayer.broadcastMessageToAll("------- betting round finished --------");
    }

    private void handleMove(int move) {
        if(move==PASS){
            currentPlayer.player.setHasPassed();
            currentPlayer.broadcastMessageToOthers(currentPlayer.getClientUsername()+" passed");
        }else if(move==RAISE){
            currentPlayer.broadcastMessageToItself("Entrance stake you want to raise: ");
            int raise = currentPlayer.raiseStakes();
            currentPlayer.player.setPoolInCurrentBetting(
                    currentPlayer.player.getPoolInCurrentBetting()+raise);
            currentPlayer.player.pay(raise);
            tie.setPoolInCurrentBetting(currentPlayer.player.getPoolInCurrentBetting());
        }else if(move==WAIT){
            currentPlayer.broadcastMessageToOthers("\n" + currentPlayer.getClientUsername()+" waits");
        }
    }

    private boolean isMovePossible(int move){
        if(move==WAIT){
            return tie.getPoolInCurrentBetting() == currentPlayer.player.getPoolInCurrentBetting();
        }else if(move==RAISE) {
            return currentPlayer.player.getPool() > 0;
        }
        return true; // PASS is always possible
    }

    /**
     * betting round is complete when:
     * (A) there is only 1 player
     * (B) all players pay pool in current betting
     * @return whether betting round is complete
     */
    private boolean checkIfRoundIsComplete(){
        for(int i = 0; i < playersInGame.size(); ++i){
            if(playersInGame.get(i).player.getHasPassed()){
                playersInGame.remove(playersInGame.get(i));
            }
        }
        if (playersInGame.size()==0 || playersInGame.size()==1){
            tie.setGameOver();
            return true;
        }
        for(ClientHandler player: playersInGame){
            if(player.player.getPoolInCurrentBetting() != tie.getPoolInCurrentBetting()){
                return false;
            }
        }
        return true;
    }

    private void collectAnte() {
        currentPlayer.broadcastMessageToAll("\nAnte ("+tie.getAnte()+") was taken from your pool in order to join the game");
        for(ClientHandler player: clientHandlers){
            player.payAnte(tie.getAnte());
            player.broadcastMessageToItself("Your current pool: " + player.player.getPool());
        }
        //updateNrOfPlayersInGameAndGameOver();
        tie.addToCommonPool(tie.getAnte() * playersInGame.size());
        currentPlayer.broadcastMessageToAll("Common pool: "+ tie.getCommonPool());
    }

    private void displayCards() {
        currentPlayer.broadcastMessageToAll("\nYour cards:");
        for(ClientHandler player: clientHandlers){
            player.displayCards();
        }
    }

    private void dealOutInitialCards() {
        for(int i = 0; i < 5; ++i){
            for(ClientHandler player: clientHandlers){
                player.player.addCard(tie.getDeck().dealOutCard());
            }
        }
    }

    private void initPlayersInGameArray() {
        clientHandlers.addAll(ClientHandler.clientHandlers);
        playersInGame.addAll(ClientHandler.clientHandlers);
    }

    private void updatePlayersArray() {
        int indexToRemove = -1;
        for(int i = 0; i < playersInGame.size();++i){
            if(playersInGame.get(i).player.getHasPassed()){
                indexToRemove = i;
                break;
            }
        }
        if(indexToRemove!=-1){
            int previousPlayerIndex = indexToRemove - 1;
            if(previousPlayerIndex==-1){ // player who passed was first
                previousPlayerIndex = playersInGame.size()-2;
            }
            playersInGame.remove(indexToRemove);
        }
    }

    // TODO
    // Here should people be deleted from list not in more higher methods
    // Problem with loop in next rounds - it finished in bad moment
    public void setNextPlayersTurn(){
        int currentIndex = playersInGame.indexOf(currentPlayer);
        if(currentIndex == playersInGame.size()-1){
            currentIndex = 0;
        }
        else{
            currentIndex++;
        }
        for(int i = 0; i < playersInGame.size(); ++i){
            if(i != currentIndex) {
                playersInGame.get(i).player.setNotTheirTurn();
            }
            else{
                playersInGame.get(i).player.setTheirTurn(); // why true is never set??? - now its set
            }
        }
        currentPlayer = playersInGame.get(currentIndex);
        updatePlayersArray();
    }

    public void thankUNext(){
        currentPlayer.player.setNotTheirTurn();
        int currentPlayersIndex = playersInGame.indexOf(currentPlayer);
        if(currentPlayersIndex == playersInGame.size()-1){
            playersInGame.get(0).player.setTheirTurn();
            currentPlayer = playersInGame.get(0);
        }
        else{
            playersInGame.get(currentPlayersIndex+1).player.setTheirTurn();
            currentPlayer = playersInGame.get(currentPlayersIndex+1);
        }


    }

    public void showWhoseTurn(){
        currentPlayer.broadcastMessageToItself("It is your turn");
        currentPlayer.broadcastMessageToOthers("It is " + currentPlayer.getClientUsername() + "'s turn");
    }

    private String displayPlayersInGame(){
        String message = "Players in game: ";
        for(ClientHandler player: playersInGame){
            message += player.getClientUsername() + ", ";
        }
        return message;
    }

    public boolean checkIfGameOver(){
        if(playersInGame.size()==1){
            tie.setGameOver();
        }
        if(tie.isGameOver()){
            currentPlayer.broadcastMessageToAll("GAME OVER");
            currentPlayer.broadcastMessageToAll(currentPlayer.getClientUsername()+" WON "+tie.getCommonPool());
            return true;
        }
        return false;
    }

    // swapping
    public void handleSwapping(){
        for(ClientHandler player: playersInGame){
            showWhoseTurn();
            player.broadcastMessageToItself("\nYour cards: "+ player.player.displayCards());
            player.broadcastMessageToItself("How many of them would you like to swap? (Input number from 0 to 5)");
            int swappedCards = player.decideWhatToSwap();
            player.broadcastMessageToOthers(player.getClientUsername() + " swaps " + swappedCards + " cards");
            thankUNext();
        }
    }
}
