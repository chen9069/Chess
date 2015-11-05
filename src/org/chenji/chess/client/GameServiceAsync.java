package org.chenji.chess.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface GameServiceAsync {
	void connect(LoginInfo loginInfo, AsyncCallback<Player> callback);
	void updateState(int matchId, int turn, String state, AsyncCallback<Void> callback);
  void getMatches(String clientId, AsyncCallback<List<Match>> callback);
  void invite(String clientId, String opponentId, AsyncCallback<Void> callback);
  void delete(int matchId, AsyncCallback<Void> callback);
  void autoMatch(String clientId, AsyncCallback<Void> callback);
  void load(int matchId, AsyncCallback<Match> callback);
}
