package pl.edu.agh.kis.pz1;

import pl.edu.agh.kis.pz1.util.Card;
import pl.edu.agh.kis.pz1.util.Combination;
import pl.edu.agh.kis.pz1.util.Rank;
import pl.edu.agh.kis.pz1.util.Suit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class Hand {
    private static Map<Combination, Integer> combinationRanking = new TreeMap<>();
    private static Map<Rank, Integer> rankRanking = new TreeMap<Rank, Integer>();
    private static Combination[] combinations = {
            Combination.RoyalFlush,     // A,K,Q,J,10 in the same suit
            Combination.StraightFlush,  // sequence in the same suit
            Combination.FourOfTheKind,
            Combination.FullHouse,      // 3 *(Rank1) + 2*(Rank2)
            Combination.Flush,          // 5 cards of the same suit
            Combination.Straight,       // sequence of cards in random suits
            Combination.ThreeOfTheKind,
            Combination.TwoPairs,
            Combination.OnePair,
            Combination.NoPair
    };
    private static ArrayList<Rank> rankArray = new ArrayList<>(
            Arrays.asList(Rank._2,Rank._3,Rank._4,Rank._5,Rank._6,Rank._7,
                    Rank._8,Rank._9,Rank._10, Rank.J, Rank.Q, Rank.K, Rank.A)
    );

    private static ArrayList<Suit> suitArray = new ArrayList<>(
            Arrays.asList(Suit.club, Suit.diamond, Suit.heart, Suit.spade)
    );

    public Hand(){
        initialiseCombinationRanking();
        initialiseRankRanking();
    }

    private static void initialiseRankRanking() {
        rankRanking.put(Rank._2,2);
        rankRanking.put(Rank._3,3);
        rankRanking.put(Rank._4,4);
        rankRanking.put(Rank._5,5);
        rankRanking.put(Rank._6,6);
        rankRanking.put(Rank._7,7);
        rankRanking.put(Rank._8,8);
        rankRanking.put(Rank._9,9);
        rankRanking.put(Rank._10,10);
        rankRanking.put(Rank.J,11);
        rankRanking.put(Rank.Q,12);
        rankRanking.put(Rank.K,13);
        rankRanking.put(Rank.A,14);
    }

    private static void initialiseCombinationRanking(){
        combinationRanking.put(Combination.RoyalFlush,10);
        combinationRanking.put(Combination.StraightFlush,9);
        combinationRanking.put(Combination.FourOfTheKind,8);
        combinationRanking.put(Combination.FullHouse,7);
        combinationRanking.put(Combination.Flush,6);
        combinationRanking.put(Combination.Straight,5);
        combinationRanking.put(Combination.ThreeOfTheKind,4);
        combinationRanking.put(Combination.TwoPairs,3);
        combinationRanking.put(Combination.OnePair,2);
        combinationRanking.put(Combination.NoPair,1);
    }

    /**
     * The highest card is important only when there is a tie, it means that
     * higher card cannot be more important than the value of combination,
     * that's why combinations points are multiplied by 15
     * @param combination scored combination of cards on hand
     * @param highestCard cards with the highest rank
     * @return point for cards on hand
     */
    public int mapHandToPoints(Combination combination, Card highestCard){
        int combinationPoints = combinationRanking.get(combination) * 15;
        int rankPoints = rankRanking.get(highestCard.getRank());
        return rankPoints + combinationPoints;
    }

    public Combination findCombinationInCards(ArrayList<Card> cards){ // not finished
        if(cards.size()!=5){
            throw new RuntimeException();
        }
        int[] playersRanks = {0,0,0,0,0,0,0,0,0,0,0,0,0};
        int[] playersSuits = {0,0,0,0};
        for(Card card: cards){
            playersSuits[suitArray.indexOf(card.getSuit())]++;
            playersRanks[rankArray.indexOf(card.getRank())]++;
        }
        /*System.out.println("Ranks:");
        for(int i = 0; i < 13; ++i){
            System.out.println(rankArray.get(i) + " "+playersRanks[i]);
        }
        System.out.println("\nSuits:");
        for(int i = 0; i < 4; ++i){
            System.out.println(suitArray.get(i) + " "+playersSuits[i]);
        }*/
        if(checkRoyalFlush(playersRanks,playersSuits))
            return Combination.RoyalFlush;
        if(checkStraightFlush(playersRanks,playersSuits))
            return Combination.StraightFlush;
        if(getMaxOfTheKind(playersRanks)==4)
            return Combination.FourOfTheKind;
        if(checkSFull(playersRanks,playersSuits))
            return Combination.FullHouse;
        if(checkFlush(playersSuits))
            return Combination.Flush;
        if(getMaxOfTheKind(playersRanks)==3)
            return Combination.ThreeOfTheKind;
        // Two Pairs
        if(checkTwoPairs(playersRanks))
            return Combination.TwoPairs;
        if(getMaxOfTheKind(playersRanks)==2)
            return Combination.OnePair;
        return Combination.NoPair;
    }

    private boolean checkTwoPairs(int[] playersRanks) {
        boolean isFirstPair = false;
        for(int i = 0; i < 13; ++i) {
            if (playersRanks[i] == 2 && !isFirstPair) {
                isFirstPair = true;
            }
            else if(playersRanks[i] == 2){
                return true;
            }
        }
        return false;
    }

    private boolean checkSFull(int[] playersRanks, int[] playersSuits) {
        for(int i = 0; i < 13; ++i) {
            if (playersRanks[i] > 0 && playersRanks[i] != 2 && playersRanks[i] !=3) {
                return false;
            }
        }
        return true;
    }

    private int getMaxOfTheKind(int[] playersRanks){
        int maxOfTheKind = 0;
        for(int i = 0; i < 13; ++i) {
            if (playersRanks[i] > maxOfTheKind) {
                maxOfTheKind = playersRanks[i];
            }
        }
        return maxOfTheKind;
    }

    private boolean checkFlush(int[] playersSuits){
        for(int i = 0; i < 4; ++i){
            if(playersSuits[i]==5){
                return true;
            }
        }
        return false;
    }

    private boolean checkStraight(int[] playersRanks){
        int startingIndex = -1;
        for(int i = 0; i < 13; ++i){
            if(playersRanks[i]>1){
                return false;
            }
            if(startingIndex==-1 && playersRanks[i]==1){
                startingIndex = i;
            }
        }
        for(int i = 1; i < 5; ++i){
            if(playersRanks[startingIndex+i]!=1){
                return false;
            }
        }
        return true;
    }

    private boolean checkStraightFlush(int[] playersRanks, int[] playersSuits) {
        if(!checkFlush(playersSuits)){
            return false;
        }
        if(!checkStraight(playersRanks)){
            return false;
        }
        return true;
    }

    private boolean checkRoyalFlush(int[] playersRanks, int[] playersSuits) {
        if(!checkFlush(playersSuits)){
            return false;
        }
        for(int i = 8; i < 13; ++i){
            if(playersRanks[i]!=1)
                return false;
        }
        return true;
    }
}