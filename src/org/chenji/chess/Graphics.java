package org.chenji.chess;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.shared.chess.Color;
import org.shared.chess.GameResult;
import org.shared.chess.Move;
import org.shared.chess.Piece;
import org.shared.chess.Position;
import org.shared.chess.State;
import org.chenji.chess.client.*;
import com.google.gwt.animation.client.Animation;
import com.google.gwt.appengine.channel.client.Channel;
import com.google.gwt.appengine.channel.client.ChannelError;
import com.google.gwt.appengine.channel.client.ChannelFactoryImpl;
import com.google.gwt.appengine.channel.client.SocketListener;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class Graphics extends Composite implements View {
  private static GameImages gameImages = GWT.create(GameImages.class);
  private static GraphicsUiBinder uiBinder = GWT.create(GraphicsUiBinder.class);

  interface GraphicsUiBinder extends UiBinder<Widget, Graphics> {
  }

  private GameServiceAsync gameService = GWT.create(GameService.class);
  private Channel channel;

  @UiField
  GameCss css;
  @UiField
  Grid boardGrid;
  @UiField
  Label playerStatus;
  @UiField
  Label gameStatus;
  @UiField
  AbsolutePanel gamePanel;
  @UiField
  Button restart;
  @UiField
  Button save;
  @UiField
  Button load;
  @UiField
  Button delete;
  @UiField
  ListBox stateList;
  @UiField
  Button invite;
  @UiField
  Button autoMatch;
  @UiField
  TextBox opponentEmail;
  @UiField
  Grid promotion;
  private Image[][] background = new Image[8][8];
  private final Image[][] board = new Image[8][8];
  private Image[] promotionImgs = new Image[4];
  private Grid gameGrid;
  private Presenter presenter;
  private boolean isMouseDown = false;
  private Image draggingImage = new Image();
  private int reletiveX = 0;
  private int reletiveY = 0;
  private Image animationImage = new Image();
  private final int DURATION = 1000;
  private boolean animationOn = false;
  private Player player;
  private Storage stateStore = Storage.getLocalStorageIfSupported();
  // private List<Match> matchList = new ArrayList<Match>();
  public int currentMatchId = 0;
  private Match currentMatch = null;
  private boolean isWaiting = true;

  public class ChangePositionAnimation extends Animation {
    private Position start;
    private Position end;
    private Position moveTo;

    public ChangePositionAnimation(Position start, Position end) {
      this.start = new Position(start.getRow() * 50, start.getCol() * 50);
      this.end = new Position(end.getRow() * 50, end.getCol() * 50);
      this.moveTo = end;
    }

    @Override
    protected void onUpdate(double progress) {
      Position position = extractProportionalLength(progress);
      animationImage.removeFromParent();
      System.out.println(position.getCol() + " " + position.getRow());
      gamePanel.add(animationImage, position.getCol(), position.getRow());
    }

    @Override
    protected void onComplete() {
      presenter.onClick(moveTo.getRow(), moveTo.getCol());
      animationImage.removeFromParent();
      animationOn = false;
    }

    private Position extractProportionalLength(double progress) {
      int x = (int) (start.getCol() - (start.getCol() - end.getCol())
          * progress);
      int y = (int) (start.getRow() - (start.getRow() - end.getRow())
          * progress);
      return new Position(y, x);
    }
  }

  public Graphics(Presenter p) {
    initWidget(uiBinder.createAndBindUi(this));
    presenter = p;
    presenter.setView(this);
    boardGrid.resize(8, 8);
    boardGrid.setCellPadding(0);
    boardGrid.setCellSpacing(0);
    boardGrid.setBorderWidth(0);
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        final Image image = new Image();
        background[row][col] = image;
        image.setWidth("100%");
        if (row % 2 == 0 && col % 2 == 1 || row % 2 == 1 && col % 2 == 0) {
          image.setResource(gameImages.blackTile());
        } else {
          image.setResource(gameImages.whiteTile());
        }
        boardGrid.setWidget(row, col, image);
      }
    }
    draggingImage.addMouseUpHandler(new MouseUpHandler() {

      @Override
      public void onMouseUp(MouseUpEvent event) {
        int x = event.getRelativeX(gamePanel.getElement());
        int y = event.getRelativeY(gamePanel.getElement());
        System.out.println("mouseup:" + x + " " + y);
        draggingImage.removeFromParent();
        presenter.onClick(y / 50, x / 50);
        History.newItem(presenter.getState().toString());
        isMouseDown = false;
      }

    });
    draggingImage.addMouseMoveHandler(new MouseMoveHandler() {

      @Override
      public void onMouseMove(MouseMoveEvent event) {
        if (isMouseDown) {
          int x = event.getRelativeX(gamePanel.getElement());
          int y = event.getRelativeY(gamePanel.getElement());
          System.out.println("mousemove:" + (x - reletiveX) + " "
              + (y - reletiveX));
          presenter.mouseMove(x, y);
        }

      }

    });

    gameGrid = new Grid();
    gameGrid.resize(8, 8);
    gameGrid.setCellPadding(0);
    gameGrid.setCellSpacing(0);
    gameGrid.setBorderWidth(0);
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        final Image image = new Image();
        board[row][col] = image;
        image.setWidth("100%");
        image.setResource(gameImages.empty());
        final int r = row;
        final int c = col;

        image.addMouseDownHandler(new MouseDownHandler() {
          @Override
          public void onMouseDown(MouseDownEvent event) {
            if (animationOn || !isTurn())
              return;
            event.preventDefault();
            reletiveX = event.getX();
            reletiveY = event.getY();
            System.out.println("mousedown:" + reletiveX + " " + reletiveX);
            isMouseDown = true;
            presenter.mouseDown(r, c);
          }
        });
        image.addMouseUpHandler(new MouseUpHandler() {

          @Override
          public void onMouseUp(MouseUpEvent event) {
            if (animationOn || !isTurn())
              return;
            int x = event.getX();
            int y = event.getY();
            System.out.println("mouseup:" + x + " " + y);
            presenter.onClick(r, c);
            History.newItem(presenter.getState().toString());
            isMouseDown = false;
          }

        });
        image.addMouseMoveHandler(new MouseMoveHandler() {

          @Override
          public void onMouseMove(MouseMoveEvent event) {
            if (isMouseDown && isTurn()) {
              int x = event.getX();
              int y = event.getY();
              System.out.println("mousemove:" + (x - reletiveX) + " "
                  + (y - reletiveX));
              presenter.mouseMove(c * 50 + x, r * 50 + y);
            }

          }

        });
        image.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!isTurn())
              return;
            animationOn = true;
            presenter.animationClick(r, c);
            System.out.println("mouseclick");
            History.newItem(presenter.getState().toString());
          }
        });

        gameGrid.setWidget(row, col, image);
      }
    }
    gamePanel.add(gameGrid, 0, 0);

    promotion.resize(1, 4);
    promotion.setCellPadding(0);
    promotion.setCellSpacing(0);
    promotion.setBorderWidth(0);

    for (int i = 0; i < 4; i++) {
      final int c = i;
      promotionImgs[i] = new Image();
      promotionImgs[i].setWidth("100%");
      promotionImgs[i].addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          presenter.promote(c);
          promotion.setVisible(false);
        }
      });
      promotion.setWidget(0, i, promotionImgs[i]);
    }

    restart.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        presenter.restart();
        History.newItem(presenter.getState().toString());
      }
    });
    // botton handlers
    restart.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        presenter.restart();
        History.newItem(presenter.getState().toString());
      }
    });

    save.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        String time = new Date().toString();
        stateStore.setItem(time, presenter.getState().toString());
        stateList.addItem(time);
      }
    });

    delete.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int index = stateList.getSelectedIndex();
        String key = stateList.getItemText(index);
        // stateStore.removeItem(key);
        // stateList.removeItem(index);
        // TODO Delete from server
        final int matchId = Integer.parseInt(key.substring(key.indexOf(':') + 2,
            key.indexOf(';')));
        gameService.delete(matchId, new AsyncCallback<Void>() {

          @Override
          public void onFailure(Throwable caught) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onSuccess(Void result) {
            // TODO Auto-generated method stub
            Window.alert("Match " + matchId + " Deleted");
            for (int i = 0; i < stateList.getItemCount(); i++) {
              String key = stateList.getItemText(i);
              int id = Integer.parseInt(key.substring(key.indexOf(':') + 2,
                  key.indexOf(';')));
              if (id == matchId) {
                stateList.removeItem(i);
                break;
              }
            }
          }
        });
      }
    });

    load.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        isWaiting = false;
        int index = stateList.getSelectedIndex();
        String key = stateList.getItemText(index);
        // String loadState = stateStore.getItem(key);
        // State state = StateParser.parse(loadState);
        // presenter.setState(state);
        // TODO Load from server
        int matchId = Integer.parseInt(key.substring(key.indexOf(':') + 2,
            key.indexOf(';')));
        gameService.load(matchId, new AsyncCallback<Match>() {

          @Override
          public void onFailure(Throwable caught) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onSuccess(Match result) {
            // TODO Auto-generated method stub
            player.setColor(result.getColorOf(player.getPlayerId()));
            currentMatchId = result.getMatchId();
            currentMatch = result;
            presenter.setState(presenter.getStateFromString(result.getState()));
            playerStatus.setText("Welcome, " + player.getPlayerId() + "("
                + player.getColor() + "); your Opponent: "
                + currentMatch.getOpponentOf(player.getPlayerId()) + "("
                + player.getColor().getOpposite() + ")");
          }
        });
      }
    });

    invite.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.alert(opponentEmail.getText() + " Invited!");
        String key = opponentEmail.getText();
        // TODO Invite
        gameService.invite(player.getPlayerId(), key,
            new AsyncCallback<Void>() {

              @Override
              public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

              }

              @Override
              public void onSuccess(Void result) {
                // TODO Auto-generated method stub

              }
            });
      }
    });

    autoMatch.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // TODO AutoMatch
        isWaiting = false;
        playerStatus.setText("waiting for other players");
        presenter.setState(new State());
        gameService.autoMatch(player.getPlayerId(), new AsyncCallback<Void>() {

          @Override
          public void onFailure(Throwable caught) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onSuccess(Void result) {
            // TODO Auto-generated method stub
          }
        });
      }
    });

    History.addValueChangeHandler(new ValueChangeHandler<String>() {

      public void onValueChange(ValueChangeEvent<String> event) {
        String token = event.getValue();
        if (token.isEmpty()) {
          presenter.setState(new State());
        } else {
          presenter.setState(presenter.getStateFromString(token));
        }
      }
    });
    promotion.setVisible(false);
  }

  public void connect(LoginInfo loginInfo) {
    gameService.connect(loginInfo, new AsyncCallback<Player>() {

      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Player result) {
        player = result;
        playerStatus.setText("Welcome, " + player.getNickname());
        channel = new ChannelFactoryImpl().createChannel(player.getToken());
        channel.open(new SocketListener() {
          @Override
          public void onOpen() {
            gameService.getMatches(player.getPlayerId(),
                new AsyncCallback<List<Match>>() {

                  @Override
                  public void onFailure(Throwable caught) {
                    // TODO Auto-generated method stub

                  }

                  @Override
                  public void onSuccess(List<Match> result) {
                    // TODO Auto-generated method stub
                    // matchList = result;
                    if (result == null)
                      return;
                    for (Match match : result) {
                      if (!match.isDeleted())
                        stateList.addItem("Match: " + match.getMatchId()
                            + "; Opponent: "
                            + match.getOpponentOf(player.getPlayerId())
                            + "; Turn: " + match.getPlayer(match.getTurn()));
                    }
                  }
                });
          }

          @Override
          public void onMessage(String message) {
            //Window.alert(message);
            if (message.startsWith("newMatch:")) {
              //Window.alert("new match");
              String value = message.substring(message.indexOf(':') + 1);
              Match match = Match.deserializeFrom(value);
              // Window.alert(match.toString());
              // matchList.add(match);
              stateList.addItem("Match: " + match.getMatchId() + "; Opponent: "
                  + match.getOpponentOf(player.getPlayerId()) + "; Turn: "
                  + match.getPlayer(match.getTurn()));
            }
            if (message.startsWith("updateMatch:")) {
              //Window.alert("update match");
              String value = message.substring(message.indexOf(':') + 1);
              Match update = Match.deserializeFrom(value);
              for (int i = 0; i < stateList.getItemCount(); i++) {
                stateList.getItemText(i);
                String key = stateList.getItemText(i);
                int matchId = Integer.parseInt(key.substring(
                    key.indexOf(':') + 2, key.indexOf(';')));
                if (matchId == update.getMatchId()) {
                  //Window.alert("match: " + matchId);
                  stateList.setItemText(
                      i,
                      "Match: " + update.getMatchId() + "; Opponent: "
                          + update.getOpponentOf(player.getPlayerId())
                          + "; Turn: " + update.getPlayer(update.getTurn()));
                }
              }
              if (currentMatchId == update.getMatchId()) {
                //Window.alert("current: " + currentMatchId);
                presenter.setState(presenter.getStateFromString(update
                    .getState()));
              }
            }
            if (message.startsWith("deleteMatch:")) {
              //Window.alert("delete match");
              String value = message.substring(message.indexOf(':') + 1);
              int matchId = Integer.parseInt(value);
              Window.alert("Match " + matchId + " Deleted");
              for (int i = 0; i < stateList.getItemCount(); i++) {
                String key = stateList.getItemText(i);
                int id = Integer.parseInt(key.substring(key.indexOf(':') + 2,
                    key.indexOf(';')));
                if (id == matchId) {
                  stateList.removeItem(i);
                  break;
                }
              }
            }
          }

          @Override
          public void onError(ChannelError error) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onClose() {
            // TODO Auto-generated method stub

          }
        });
      }
    });
  }

  @Override
  public void updateState(State state) {
    gameService.updateState(currentMatchId,
        (state.getTurn().isWhite() ? 0 : 1), state.toString(),
        new AsyncCallback<Void>() {

          @Override
          public void onFailure(Throwable caught) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onSuccess(Void result) {
            // TODO Auto-generated method stub

          }
        });
  }

  private boolean isTurn() {
    return isWaiting
        || (player != null && player.getColor() == presenter.getTurn());
  }

  public void setResource(Image image, Piece piece) {
    if (piece != null) {
      switch (piece.getKind()) {
      case BISHOP:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whiteBishop() : gameImages.blackBishop());
        break;
      case KING:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whiteKing() : gameImages.blackKing());
        break;
      case KNIGHT:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whiteKnight() : gameImages.blackKnight());
        break;
      case PAWN:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whitePawn() : gameImages.blackPawn());
        break;
      case QUEEN:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whiteQueen() : gameImages.blackQueen());
        break;
      case ROOK:
        image.setResource(piece.getColor() == Color.WHITE ? gameImages
            .whiteRook() : gameImages.blackRook());
        break;
      default:
        break;
      }
    }
    image.setWidth("100%");
  }

  public void dragTo(int x, int y, Piece piece) {
    setResource(draggingImage, piece);
    draggingImage.removeFromParent();
    gamePanel.add(draggingImage, x - reletiveX, y - reletiveY);
  }

  public void setPromotion(Color color) {
    if (color == null) {
      promotion.setVisible(false);
      return;
    }
    if (color.isWhite()) {
      promotionImgs[0].setResource(gameImages.whiteQueen());
      promotionImgs[1].setResource(gameImages.whiteKnight());
      promotionImgs[2].setResource(gameImages.whiteRook());
      promotionImgs[3].setResource(gameImages.whiteBishop());
    } else {
      promotionImgs[0].setResource(gameImages.blackQueen());
      promotionImgs[1].setResource(gameImages.blackKnight());
      promotionImgs[2].setResource(gameImages.blackRook());
      promotionImgs[3].setResource(gameImages.blackBishop());
    }
    promotion.setVisible(true);
  }

  @Override
  public void setPiece(int row, int col, Piece piece) {
    if (piece == null) {
      board[row][col].setResource(gameImages.empty());
      return;
    }
    switch (piece.getKind()) {
    case PAWN:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whitePawn() : gameImages.blackPawn());
      break;
    case ROOK:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whiteRook() : gameImages.blackRook());
      break;
    case KNIGHT:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whiteKnight() : gameImages.blackKnight());
      break;
    case BISHOP:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whiteBishop() : gameImages.blackBishop());
      break;
    case QUEEN:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whiteQueen() : gameImages.blackQueen());
      break;
    case KING:
      board[row][col].setResource(piece.getColor().isWhite() ? gameImages
          .whiteKing() : gameImages.blackKing());
      break;
    default:
      break;
    }
  }

  @Override
  public void setHighlighted(int row, int col, boolean highlighted) {
    Element element = board[row][col].getElement();
    if (highlighted) {
      element.setClassName(css.highlighted());
    } else {
      element.removeClassName(css.highlighted());
    }
  }

  @Override
  public void setSelected(int row, int col, boolean selected) {
    Element element = board[row][col].getElement();
    if (selected) {
      element.setClassName(css.selected());
    } else {
      element.removeClassName(css.selected());
    }
  }

  @Override
  public void setWhoseTurn(Color color) {
    if (currentMatch != null) {
      if (player.getColor() == color) {
        gameStatus.setText("Your(" + color + ") Turn");
      } else {
        gameStatus.setText(currentMatch.getOpponentOf(player.getPlayerId())
            + "(" + color + ")" + "'s Turn");
      }
    } else {
      if (color == Color.BLACK) {
        gameStatus.setText("Black's Turn");
      } else if (color == Color.WHITE) {
        gameStatus.setText("White's Turn");
      } else {
        gameStatus.setText("Draw");
      }
    }
  }

  @Override
  public void setGameResult(GameResult gameResult) {
    if (gameResult != null) {
      if (gameResult.isDraw()) {
        gameStatus.setText("Draw: " + gameResult.getGameResultReason());
      } else if (gameResult.getWinner() == Color.BLACK) {
        gameStatus.setText("Black Win");
      } else if (gameResult.getWinner() == Color.WHITE) {
        gameStatus.setText("White Win");
      } else {
      }
    }
  }

  @Override
  public void animation(Move move, Piece piece) {
    // TODO Auto-generated method stub
    if (piece == null) {
      animationOn = false;
      return;
    }
    setResource(animationImage, piece);
    ChangePositionAnimation cpa = new ChangePositionAnimation(move.getFrom(),
        move.getTo());
    cpa.run(DURATION);
  }

}
