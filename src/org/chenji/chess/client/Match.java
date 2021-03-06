package org.chenji.chess.client;

import java.io.Serializable;

import org.shared.chess.Color;
import org.shared.chess.State;

public class Match implements Serializable{
  /**
   * 
   */
  private static final long serialVersionUID = 2811557183395350224L;
  private int matchId;
  private boolean deleted = false;
  private String state;
  private int turn;
  private String[] players = new String[] {"", ""};
  public Match() {}
  public Match(int id) {
    matchId = id;
    turn = 0;
    state = new State().toString();
  }
  public Match(int id, String player1, String player2) {
    matchId = id;
    players[0] = player1;
    players[1] = player2;
    turn = 0;
    state = new State().toString();
  }
  public Match(int id, boolean deleted, String player1, String player2, int turn, String state) {
    matchId = id;
    this.deleted = deleted;
    players[0] = player1;
    players[1] = player2;
    this.turn = turn;
    this.state = state;
  }
  public String getState() {
    return state;
  }
  public void setState(String state) {
    this.state = state;
  }
  public String getPlayer(int i) {
    return players[i];
  }
  public void setPlayer(int i, String player) {
    this.players[i] = player;
  }
  public int getMatchId() {
    return matchId;
  }
  public void setMatchId(int matchId) {
    this.matchId = matchId;
  }
  public boolean isDeleted() {
    return deleted;
  }
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
  public boolean isMatch(String player1, String player2) {
    return (players[0].equals(player1) && players[1].equals(player2)) || (players[0].equals(player2) && players[1].equals(player1));
  }
  public String getOpponentOf(String player) {
    assert (player.equals(players[0]) || player.equals(players[1]));
    return players[0].equals(player) ? players[1] : players[0];
  }
  public Color getColorOf(String player) {
    assert (player.equals(players[0]) || player.equals(players[1]));
    return players[0].equals(player) ? Color.WHITE : Color.BLACK;
  }
  @Override
  public String toString() {
    return matchId + "&" + deleted + "&" + players[0] + "&" + players[1] + "&" + turn + "&" + state;
  }
  public static Match deserializeFrom(String value) {
    String keys[] = value.split("&");
    return new Match(Integer.parseInt(keys[0]), Boolean.parseBoolean(keys[1]), keys[2], keys[3], Integer.parseInt(keys[4]), keys[5]);
  }
  public int getTurn() {
    return turn;
  }
  public void setTurn(int turn) {
    this.turn = turn;
  }
}
