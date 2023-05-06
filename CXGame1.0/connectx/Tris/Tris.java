/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

 package connectx.Tris;

 import connectx.CXPlayer;
 import connectx.CXBoard;
 import connectx.CXGameState;
 import connectx.CXCell;
 import java.util.TreeSet;
 import java.util.Random;
 import java.util.Arrays;
 import java.util.concurrent.TimeoutException;
 
 /**
  * Software player only a bit smarter than random.
  * <p>
  * It can detect a single-move win or loss. In all the other cases behaves
  * randomly.
  * </p>
  */
 public class Tris implements CXPlayer {
     @Override
    public String playerName() {
        // TODO Auto-generated method stub
        return null;
    }
    private Random rand;
     private CXGameState myWin;
     private CXGameState yourWin;
     private int  TIMEOUT;
     private long START;
 
     /* Default empty constructor */
     public Tris() {
     }
 
     public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
         // New random seed for each game
         rand    = new Random(System.currentTimeMillis());
         myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
         yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
         TIMEOUT = timeout_in_secs;
     }
     
     public int selectColumn(CXBoard B) { {
        start   = System.currentTimeMillis();
        CXCell ret_value = null;
        //System.out.println("FC.len = " + FC.length);

        if(MC.length > 0) {
            CXCell c = MC[MC.length-1]; // Recover the last move from MC
            B.markCell(c.i,c.j);         // Save the last move in the local MNKBoard
        }

        //System.out.println("B.FC.len = " + B.getFreeCells().length);

        double bestScore = Double.NEGATIVE_INFINITY;
        for(CXCell d : FC) {
            B.markCell(d.i, d.j);
            double score = alphaBeta(false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 30);
            B.unmarkCell();
            if(score > bestScore){
                bestScore = score;
                ret_value = d;
            }
        }

        B.markCell(ret_value.i, ret_value.j);
        return ret_value;
    }

    public double alphaBeta(boolean isMaximising, double a, double b, int depth){
        // if it runs out of time or depth is 0 return 0
        if((System.currentTimeMillis()-start)/1000.0 > TIMEOUT*(99.0/100.0) || depth == 0) 
            return 0;
            
        // check if there's a win ----------------------------------
        CXCell[] MC = B.getMarkedCells();
        CXCell c = MC[MC.length-1];
        B.unmarkCell();
        if(B.markCell(c.i, c.j) == myWin)
            return 10;
        else{
            B.unmarkCell();
            if(B.markCell(c.i, c.j) == yourWin)
                return -10;
        } // -------------------------------------------------------


        // check if draw -------------------------------------------
        if(B.getFreeCells().length == 0)
            return 0;
        // ---------------------------------------------------------



        // search for the win --------------------------------------
        if(isMaximising) { 
            double bestScore = Double.NEGATIVE_INFINITY;
            for(CXCell d : B.getFreeCells()) {
                B.markCell(d.i, d.j);
                double score = alphaBeta(false, a, b, depth-1);
                B.unmarkCell();
                bestScore = max(score, bestScore);
                a = max(a, bestScore);
                if(b <= a) break;
            }
            return bestScore;
        }
        else {
            double bestScore = Double.POSITIVE_INFINITY;
            for(CXCell d : B.getFreeCells()) {
                B.markCell(d.i, d.j);
                double score = alphaBeta(true, a, b, depth-1);
                B.unmarkCell();
                bestScore = min(score, bestScore);
                b = min(b, bestScore);
                if(b <= a) break;
            }
            return bestScore;
        } // --------------------------------------------------------
    }

    public double max(double a, double b) { if(a > b) return a; else return b; }
    public double min(double a, double b) { if(a < b) return a; else return b; }
 }
 