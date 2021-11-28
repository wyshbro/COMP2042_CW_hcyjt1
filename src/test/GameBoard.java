/*
 *  Brick Destroy - A simple Arcade video game
 *   Copyright (C) 2017  Filippo Ranza
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

public class GameBoard extends JComponent implements KeyListener, MouseListener, MouseMotionListener {

    private static final String CONTINUE = "Continue";
    private static final String RESTART = "Restart";
    private static final String EXIT = "Exit";
    private static final String PAUSE = "Pause Menu";
    private static final int TEXT_SIZE = 30;
    private static final Color MENU_COLOR = new Color(0, 255, 0);

    private static final Color BUTTON_COLOR = new Color(102, 102, 102);

    private static final int DEF_WIDTH = 600;
    private static final int DEF_HEIGHT = 450;

    private static final Color BG_COLOR = Color.WHITE;

    private Timer gameTimer;
    private Timer timer;

    int second,min;
    String ddSecond = "00",ddMin = "00";

    DecimalFormat format = new DecimalFormat("00");
    private Wall wall;


    private String message;

    private boolean showPauseMenu;
    private boolean showScore;

    private Font menuFont;
    private Font textFont;
    private Font scoreFont;

    private Rectangle continueButtonRect;
    private Rectangle exitButtonRect;
    private Rectangle restartButtonRect;
    private Rectangle theScore = new Rectangle(new Point(175, 200), new Dimension(250, 150));
    private Rectangle continueButton;

    private Score scores;

    private int strLen;

    private DebugConsole debugConsole;

    private GameFrame owner;

    public GameBoard(GameFrame owner) {

        super();

        this.owner = owner;

        Dimension btnDim = new Dimension(170, 35);
        continueButton = new Rectangle(btnDim);

        second = 0;
        min = 0;

        strLen = 0;

        showPauseMenu = false;
        showScore = false;

        menuFont = new Font("Monospaced", Font.PLAIN, TEXT_SIZE);

        textFont = new Font("Arial", Font.BOLD, 18);

        scoreFont = new Font("Arial", Font.ITALIC, 40);

        scores = new Score(owner);

        this.initialize();

        message = "";

        wall = new Wall(new Rectangle(0, 0, DEF_WIDTH, DEF_HEIGHT), 1, 1, 6 / 2, new Point(300, 430));

        debugConsole = new DebugConsole(owner, wall, this);

        //initialize the first level
        wall.nextLevel();

        timer = new Timer(1000, e -> {
            second++;
            ddSecond = format.format(second);
            ddMin = format.format(min);

            if(second == 60) {
                min++;
                second = 0;
            }
        });

        gameTimer = new Timer(10, e -> {
            wall.move();
            wall.findImpacts();
            message = String.format("Bricks: %d Balls %d", wall.getBrickCount(), wall.getBallCount());
            if (wall.isBallLost()) {
                if (wall.ballEnd()) {
                    wall.wallReset();
                    message = "Game over";
                    timerReset();
                }
                wall.ballReset();
                gameTimer.stop();
                timer.stop();
            } else if (wall.isDone()) {
                if (wall.hasLevel()) {
                    gameTimer.stop();
                    timer.stop();
                    showScore = true;
                } else {
                    message = "ALL WALLS DESTROYED";
                    gameTimer.stop();
                    timer.stop();
                    //this.owner.dispose();
                    this.owner.remove(this);
                    scores.showScore();
                }
            }
            repaint();
        });

    }

    private void timerReset() {
        ddSecond = "00";
        ddMin = "00";
        min = 0;
        second = 0;
    }

    private void initialize() {
        this.setPreferredSize(new Dimension(DEF_WIDTH, DEF_HEIGHT));
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }


    public void paint(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        clear(g2d);

        g2d.setColor(Color.BLUE);
        g2d.drawString(message, 250, 225);

        drawBall(wall.ball, g2d);

        for (Brick b : wall.bricks)
            if (!b.isBroken())
                drawBrick(b, g2d);

        drawPlayer(wall.player, g2d);

        if (showPauseMenu)
            drawMenu(g2d);
        if (showScore)
            drawScore(g2d);
        else {
            g2d.setFont(textFont);
            g2d.drawString(ddMin + ":" + ddSecond, 520, 45);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawScore(Graphics2D g2d) {
        obscureGameBoard(g2d);
        g2d.setColor(BG_COLOR);
        g2d.fill(theScore);
        Text(g2d);
        score(g2d);
        Button(g2d);
    }

    private void score(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(scoreFont);
        scores.setScore(min,second);
        double playerScore = scores.getScore();
        g2d.drawString(String.valueOf(playerScore),260,270);
        timerReset();
    }

    private void Text(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(textFont);
        int textX = 200;
        int textY = 220;
        g2d.drawString("Your score is :", textX, textY);
    }

    private void Button(Graphics2D g2d) {

        g2d.setColor(MENU_COLOR);
        g2d.setFont(menuFont);

        int buttonX = 220;
        int buttonY = 305;//identifyButton_Y();
        continueButton.setLocation(buttonX, buttonY);

        int textX = 230;
        int textY = 335;

        g2d.setColor(BUTTON_COLOR);
        g2d.fill(continueButton);
        g2d.setColor(Color.BLACK);
        g2d.draw(continueButton);
        g2d.drawString("CONTINUE", textX, textY);
    }

    private void clear(Graphics2D g2d) {
        Color tmp = g2d.getColor();
        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(tmp);
    }

    private void drawBrick(Brick brick, Graphics2D g2d) {
        Color tmp = g2d.getColor();

        g2d.setColor(brick.getInnerColor());
        g2d.fill(brick.getBrick());

        g2d.setColor(brick.getBorderColor());
        g2d.draw(brick.getBrick());

        g2d.setColor(tmp);
    }

    private void drawBall(Ball ball, Graphics2D g2d) {
        Color tmp = g2d.getColor();

        Shape s = ball.getBallFace();

        g2d.setColor(ball.getInnerColor());
        g2d.fill(s);

        g2d.setColor(ball.getBorderColor());
        g2d.draw(s);

        g2d.setColor(tmp);
    }

    private void drawPlayer(Player p, Graphics2D g2d) {
        Color tmp = g2d.getColor();

        Shape s = p.getPlayerFace();
        g2d.setColor(Player.INNER_COLOR);
        g2d.fill(s);

        g2d.setColor(Player.BORDER_COLOR);
        g2d.draw(s);

        g2d.setColor(tmp);
    }

    private void drawMenu(Graphics2D g2d) {
        obscureGameBoard(g2d);
        drawPauseMenu(g2d);
    }

    private void obscureGameBoard(Graphics2D g2d) {

        Composite tmp = g2d.getComposite();
        Color tmpColor = g2d.getColor();

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f);
        g2d.setComposite(ac);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, DEF_WIDTH, DEF_HEIGHT);

        g2d.setComposite(tmp);
        g2d.setColor(tmpColor);
    }

    private void drawPauseMenu(Graphics2D g2d) {
        Font tmpFont = g2d.getFont();
        Color tmpColor = g2d.getColor();


        g2d.setFont(menuFont);
        g2d.setColor(MENU_COLOR);

        if (strLen == 0) {
            FontRenderContext frc = g2d.getFontRenderContext();
            strLen = menuFont.getStringBounds(PAUSE, frc).getBounds().width;
        }

        int x = (this.getWidth() - strLen) / 2;
        int y = this.getHeight() / 10;

        g2d.drawString(PAUSE, x, y);

        x = this.getWidth() / 8;
        y = this.getHeight() / 4;


        if (continueButtonRect == null) {
            FontRenderContext frc = g2d.getFontRenderContext();
            continueButtonRect = menuFont.getStringBounds(CONTINUE, frc).getBounds();
            continueButtonRect.setLocation(x, y - continueButtonRect.height);
        }

        g2d.drawString(CONTINUE, x, y);

        y *= 2;

        if (restartButtonRect == null) {
            restartButtonRect = (Rectangle) continueButtonRect.clone();
            restartButtonRect.setLocation(x, y - restartButtonRect.height);
        }

        g2d.drawString(RESTART, x, y);

        y *= 3.0 / 2;

        if (exitButtonRect == null) {
            exitButtonRect = (Rectangle) continueButtonRect.clone();
            exitButtonRect.setLocation(x, y - exitButtonRect.height);
        }

        g2d.drawString(EXIT, x, y);


        g2d.setFont(tmpFont);
        g2d.setColor(tmpColor);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_A:
                wall.player.moveLeft();
                break;
            case KeyEvent.VK_D:
                wall.player.movRight();
                break;
            case KeyEvent.VK_ESCAPE:
                showPauseMenu = !showPauseMenu;
                repaint();
                gameTimer.stop();
                timer.stop();
                break;
            case KeyEvent.VK_SPACE:
                if (!showPauseMenu)
                    if (gameTimer.isRunning()) {
                        timer.stop();
                        gameTimer.stop();
                    } else {
                        gameTimer.start();
                        timer.start();
                    }
                break;
            case KeyEvent.VK_F1:
                if (keyEvent.isAltDown() && keyEvent.isShiftDown())
                    debugConsole.setVisible(true);
            default:
                wall.player.stop();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        wall.player.stop();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        Point p = mouseEvent.getPoint();
        if (showPauseMenu) {
            if (continueButtonRect.contains(p)) {
                showPauseMenu = false;
                repaint();
            } else if (restartButtonRect.contains(p)) {
                message = "Game restarts";
                wall.ballReset();
                wall.wallReset();
                showPauseMenu = false;
                repaint();
            } else if (exitButtonRect.contains(p)) {
                System.exit(0);
            }
        }
        else {
            if (continueButton.contains(p)) {
                message = "Next level";
                wall.ballReset();
                wall.wallReset();
                wall.nextLevel();
                showScore = false;
                repaint();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        Point p = mouseEvent.getPoint();
        if (exitButtonRect != null && showPauseMenu) {
            if (exitButtonRect.contains(p) || continueButtonRect.contains(p) || restartButtonRect.contains(p))
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else
                this.setCursor(Cursor.getDefaultCursor());
        } else if (showScore) {
            if (continueButton.contains(p))
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else
                this.setCursor(Cursor.getDefaultCursor());
        } else {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    public void onLostFocus() {
        gameTimer.stop();
        message = "Focus lost";
        repaint();
    }
}