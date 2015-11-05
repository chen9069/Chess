package org.chenji.ai;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;

import org.chenji.logic;
import org.shared.chess.Color;
import org.shared.chess.GameResult;
import org.shared.chess.Move;
import org.shared.chess.Piece;
import org.shared.chess.Position;
import org.shared.chess.State;

public class Heuristic {
	StateExplorerImpl  SEI= new StateExplorerImpl();
	StateChangerImpl SCI = new StateChangerImpl();

	
	int getStateValue(State s){
		int result = 0;
		if(s.getGameResult()!=null){
			GameResult gr= s.getGameResult();
			if(gr.isDraw()){
				return 0;
			}else if (gr.getWinner()==Color.WHITE){
				return 3200;
			}else {
				return -3200;
			}
		}else{
			for(int row=0;row<8;row++){
				for(int col=0;col<8;col++) result+= getValue(s.getPiece(new Position(row,col)));
			}
		}
		return result;
	}
	
	Iterable<Move> getOrderedMoves(final State s){
		ArrayList<Move> result = new ArrayList<Move>();
		result.addAll(SEI.getPossibleMoves(s));
		Collections.sort(result,new Comparator<Move>(){

			@Override
			public int compare(Move o1, Move o2) {
				Random rnd = new Random();
				int v1= rnd.nextInt();
				int v2= rnd.nextInt();
				return v1-v2;
			}
			
		} );
		Collections.sort(result,new Comparator<Move>(){

			@Override
			public int compare(Move o1, Move o2) {
				int v1= EatValue(s,o1);
				int v2= EatValue(s,o2);
				return v2-v1;
			}
			
		} );
		

		return result;
	}
	
	
	
	
	int getValue(Piece piece){
		int value=0;
		if(piece==null) return 0;
		
		switch(piece.getKind()){
			case PAWN:{
				value = 10;
				break;
			} 
			case KNIGHT:{
				value = 30;
				break;
			}
			case BISHOP:{
				value = 30;
				break;
			}
			case ROOK:{
				value = 50;
				break;
			}
			case QUEEN:{
				value = 100;
				break;
			}
			case KING:{
				value = 1200;
			}
		}
		
		if(piece.getColor()!=Color.WHITE){
			value = -value;
		}
		return value;
	}
	
	int EatValue(State s,Move m){
		Piece fp = s.getPiece(m.getFrom());
		Piece tp = s.getPiece(m.getTo());
		if(tp==null) return 0;
		int fromValue = Math.abs(getValue(fp));
		int toValue = Math.abs(getValue(tp));
		State result= s.copy();
		SCI.makeMove(result, m);
		Set<Move> moves = SEI.getPossibleMoves(result);
		for(Move mv:moves){
			if (mv.getTo().equals(m.getTo())){
				return toValue-fromValue;
			}
		}
		return toValue;
	}
	
	
}
