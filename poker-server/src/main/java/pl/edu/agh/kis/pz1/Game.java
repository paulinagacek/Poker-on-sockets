package pl.edu.agh.kis.pz1;

import pl.edu.agh.kis.pz1.util.Card;

import java.io.IOException;
import java.util.ArrayList;
public class Game {
    private ClientHandler currentPlayer;
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); // clients connected to socket
    public static ArrayList<ClientHandler> playersInGame = new ArrayList<>(); // clients in game
    public Tie tie = new Tie();
    private int WAIT = 1, PASS = 2, RAISE = 3;

    public ClientHandler getCurrentPlayer(){return currentPlayer;}

    public void play() throws IOException {
        prepareGame();
        // 1st betting

        if(!tie.isGameOver()){
            handleBetting();
            while (!checkIfRoundIsComplete() && !tie.isGameOver()) {
                handleBetting();
            }
            tie.prepareForNextBetting();
        }
        currentPlayer.broadcastMessageToAll("Betting round finished");

        // swapping
        if(!tie.isGameOver()){
            handleSwapping();
        }

        //second betting
        if(!tie.isGameOver()){
            handleBetting();
            while (!checkIfRoundIsComplete() && !tie.isGameOver()) {
                handleBetting();
            }
            tie.prepareForNextBetting();
        }
        currentPlayer.broadcastMessageToAll("Betting round finished");

        //judging
        if(!tie.isGameOver()){
            handleRevealingHands();
        }
        System.out.println("Finish");
    }


    private void handleBetting() throws IOException {
        for(int i = 0; i < playersInGame.size(); ++i){
            checkIfGameOver();
            showWhoseTurn();
            currentPlayer.broadcastMessageToItself("\nCommon pool " + tie.getCommonPool() +
                    "\nEntrance pool: "+tie.getPoolInCurrentBetting()+ "\n"
                    + "You have already raised: "+ currentPlayer.player.getPoolInCurrentBetting()+"\n"+
                    "What do you want to do?\n(1) Wait\n(2) Pass\n(3) Raise the stakes");

            int move = currentPlayer.decideWhichMoveToDo();
            handleMove(move);
            while(!isMovePossible(move)){
                currentPlayer.broadcastMessageToItself("Your move is invalid, do sth else");
                move = currentPlayer.decideWhichMoveToDo();
                handleMove(move);
            }
            if (move==PASS){
                i--;
            }
            thankUNext();
            removePassedPlayers();
            currentPlayer.broadcastMessageToAll(displayPlayersInGame());
            if(checkIfGameOver()){
                break;
            }
        }
    }

    public void prepareGame(){
        initPlayersInGameArray();
        currentPlayer = clientHandlers.get(0);
        collectAnte();
        dealOutInitialCards();
        tie.getDeck().displayDeck();
        displayCards();
    }

    private void handleRaisingStakes(){
        currentPlayer.broadcastMessageToItself("Enter stake you want to raise: ");
        int raise = currentPlayer.decideHowMuchRaiseStakes();
        while(raise < tie.getPoolInCurrentBetting()- currentPlayer.player.getPoolInCurrentBetting()
            || raise > currentPlayer.player.getPool()){
            if(raise < tie.getPoolInCurrentBetting()- currentPlayer.player.getPoolInCurrentBetting()){
                currentPlayer.broadcastMessageToItself("You have to raise at least " +
                        (tie.getPoolInCurrentBetting() - currentPlayer.player.getPoolInCurrentBetting()));
            }else{
                currentPlayer.broadcastMessageToItself("You do not have enough money to do so");
            }
            currentPlayer.broadcastMessageToItself("Enter stake you want to raise: ");
            raise = currentPlayer.decideHowMuchRaiseStakes();
        }
        currentPlayer.player.setPoolInCurrentBetting(
                currentPlayer.player.getPoolInCurrentBetting()+raise);
        currentPlayer.player.pay(raise);
        tie.setPoolInCurrentBetting(currentPlayer.player.getPoolInCurrentBetting());
        currentPlayer.broadcastMessageToItself("Your current pool: " + currentPlayer.player.getPool());
        tie.addToCommonPool(raise);
        currentPlayer.broadcastMessageToAll("Common pool: "+ tie.getCommonPool());
    }

    public void handleMove(int move) {
        if(move==PASS){
            currentPlayer.player.setHasPassed();
            currentPlayer.broadcastMessageToOthers(currentPlayer.getClientUsername()+" passed");
        }else if(move==RAISE){
            handleRaisingStakes();
        }else if(move==WAIT){
            currentPlayer.broadcastMessageToOthers("\n" + currentPlayer.getClientUsername()+" waits");
        }
    }

    /**
     * Checks if player can do such move
     * @param move move selected by player (wait, pass, raise)
     * @return whether move is possible
     */
    public boolean isMovePossible(int move){
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
    public boolean checkIfRoundIsComplete(){
        for(int i = 0; i < playersInGame.size(); ++i){
            if(playersInGame.get(i).player.getHasPassed()){
                playersInGame.remove(playersInGame.get(i));
            }
        }
        if (playersInGame.isEmpty() || playersInGame.size()==1){
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

    public void collectAnte() {
        currentPlayer.broadcastMessageToAll("\nAnte ("+tie.getAnte()+") was taken from your pool in order to join the game");
        for(ClientHandler player: clientHandlers){
            player.player.pay(tie.getAnte());
            player.broadcastMessageToItself("Your current pool: " + player.player.getPool());
        }
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

    /**
     * initialises clientHandlers and playersInGame lists
     */
    public void initPlayersInGameArray() {
        clientHandlers.addAll(ClientHandler.clientHandlers);
        playersInGame.addAll(ClientHandler.clientHandlers);
        currentPlayer = clientHandlers.get(0);
    }

    /**
     * updates clientHandlers and playersInGame lists
     */
    public void updatePlayersInGame(){
        if(ClientHandler.clientHandlers.size()!=clientHandlers.size()){
            clientHandlers.clear();
            clientHandlers.addAll(ClientHandler.clientHandlers);
            currentPlayer = clientHandlers.get(0);
        }
        if(ClientHandler.clientHandlers.size()!=playersInGame.size()){
            playersInGame.clear();
            playersInGame.addAll(ClientHandler.clientHandlers);
        }
    }

    public void removePassedPlayers() {
        int indexToRemove = -1;
        for(int i = 0; i < playersInGame.size();++i){
            if(playersInGame.get(i).player.getHasPassed()){
                indexToRemove = i;
                break;
            }
        }
        if(indexToRemove!=-1){ // deletion wihch may cause changes in queue
            playersInGame.remove(indexToRemove);
        }
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

    public String displayPlayersInGame(){
        StringBuilder message = new StringBuilder("Players in game: ");
        for(ClientHandler player: playersInGame){
            message.append(player.getClientUsername()).append(", ");
        }
        return message.toString();
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
            player.swapCads(swappedCards, tie);
            player.broadcastMessageToItself("\nYour cards: "+ player.player.displayCards());
            tie.getDeck().displayDeck();
            thankUNext();
        }
    }

    // judge hand
    public void handleRevealingHands() {
        ArrayList<ArrayList<Card>> playersCards = new ArrayList<>();
        for(ClientHandler player: playersInGame){
            player.broadcastMessageToOthers(player.getClientUsername()+"'s cards: "+
                    player.player.displayCards());
            playersCards.add(player.player.getCards());
        }
        Hand hand = new Hand();
        int winnerIndex = hand.indexWhoWins(playersCards);
        if(hand.isOnlyOneWinner(playersCards)){
            playersInGame.get(0).broadcastMessageToAll(playersInGame.get(winnerIndex).getClientUsername() +
                    " wins - "+ playersInGame.get(winnerIndex).player.getCombination() + ", highest card: "+
                    playersInGame.get(winnerIndex).player.getHighestRankInCombination());
        }
        else{
            playersInGame.get(0).broadcastMessageToAll("There is a tie!");
            int indexSecondWinner = hand.indexSecondWinner(playersCards);
            playersInGame.get(0).broadcastMessageToAll(playersInGame.get(winnerIndex).getClientUsername() +
                    " and "+ playersInGame.get(indexSecondWinner).getClientUsername()+" win - "+ playersInGame.get(winnerIndex).player.getCombination()
                    + ", highest card: "+ playersInGame.get(winnerIndex).player.getHighestRankInCombination());
        }
    }

    public void restartGame() {
        clientHandlers.clear(); // order changed
        playersInGame.clear();
        tie.restart();
    }
}
