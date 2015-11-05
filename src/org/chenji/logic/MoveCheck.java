package org.chenji.logic;

import org.shared.chess.Move;
import org.shared.chess.State;

public interface MoveCheck {
  /**
   * check whether the move is legal
   * @param state
   * @param move
   * @return
   */
  public boolean isCanMakeMove(State state, Move move);
}
