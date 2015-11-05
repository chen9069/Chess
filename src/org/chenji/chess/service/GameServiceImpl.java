package org.chenji.chess.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chenji.chess.client.*;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class GameServiceImpl extends RemoteServiceServlet implements GameService {

	/**
   * 
   */
  private static final long serialVersionUID = -1138122470165028454L;
  private ChannelService channelService = ChannelServiceFactory.getChannelService();
	private Map<String, Player> players = new HashMap<String, Player>();
	private Map<String, List<Match>> matchList = new HashMap<String, List<Match>>();
  private List<Match> matches = new ArrayList<Match>();
  private Match autoMatch = null;
  public static int TotalNumberOfMatches = 0;
	@Override
	public Player connect(LoginInfo loginInfo) {
	  //State state = new State();
		Player player = new Player();
		player.setPlayerId(loginInfo.getEmailAddress());
		player.setNickname(loginInfo.getNickname());
		String token = channelService.createChannel(player.getPlayerId());
		player.setToken(token);
		players.put(player.getPlayerId(), player);
		return player;
	}
	@Override 
	public List<Match> getMatches(String clientId) {
	  return matchList.get(clientId);
	}
  @Override
  public void autoMatch(String clientId) {
    if (autoMatch == null) {
      autoMatch = new Match(TotalNumberOfMatches++);
      autoMatch.setPlayer(0, clientId);
    } else {
      if (autoMatch.getPlayer(0).equals(clientId))
        return;
      autoMatch.setPlayer(1, clientId);
      updateMatchLists(autoMatch);
      channelService.sendMessage(new ChannelMessage(autoMatch.getPlayer(0), "newMatch:" + autoMatch.toString()));
      channelService.sendMessage(new ChannelMessage(autoMatch.getPlayer(1), "newMatch:" + autoMatch.toString()));
      autoMatch = null;
    }
  }
  private void updateMatchLists(Match match) {
    String clientId = match.getPlayer(0);
    String opponentId = match.getPlayer(1);
    matches.add(match);
    if (!matchList.containsKey(clientId)) {
      matchList.put(clientId, new ArrayList<Match>());
    }
    if (!matchList.containsKey(opponentId)) {
      matchList.put(opponentId, new ArrayList<Match>());
    }
    matchList.get(clientId).add(match);
    matchList.get(opponentId).add(match);
  }
	@Override
	public void invite(String clientId, String opponentId) {
	  Match match = new Match(TotalNumberOfMatches++, clientId, opponentId);
	  updateMatchLists(match);
	  if (players.containsKey(opponentId)) {
	    channelService.sendMessage(new ChannelMessage(opponentId, "newMatch:" + match.toString()));
	  }
	  channelService.sendMessage(new ChannelMessage(clientId, "newMatch:" + match.toString()));
	}
	@Override
	public void delete(int matchId) {
	  for (Match match : matches) {
	    if (match.getMatchId() == matchId) {
	      match.setDeleted(true);
	      channelService.sendMessage(new ChannelMessage(match.getPlayer(0), "deleteMatch:" + match.getMatchId()));
        channelService.sendMessage(new ChannelMessage(match.getPlayer(1), "deleteMatch:" + match.getMatchId()));
	    }
	  }
	}
	@Override
  public Match load(int matchId) {
    for (Match match : matches) {
      if (match.getMatchId() == matchId) {
        return match;
      }
    }
    return null;
  }
	@Override
	public void updateState(int matchId, int turn, String state) {
	  for (Match match : matches) {
      if (match.getMatchId() == matchId) {
        match.setState(state);
        match.setTurn(turn);
        channelService.sendMessage(new ChannelMessage(match.getPlayer(0), "updateMatch:" + match.toString()));
        channelService.sendMessage(new ChannelMessage(match.getPlayer(1), "updateMatch:" + match.toString()));
        return;
      }
    }
	}
}
