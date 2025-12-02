package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import Client.ui.ChancePanel;
import Client.ui.LifePanel;

public class OmokClient extends JFrame implements Network.MessageListener {

    // -----------------------------
    // 네트워크 및 플레이어 정보
    // -----------------------------
    private Socket socket;
    private BufferedReader in;    // 서버 → 메시지 수신
    private PrintWriter out;      // 서버 ← 메시지 전송
    private char myColor;         // 'B' or 'W'

    // -----------------------------
    // 보드 관련 기본 설정
    // -----------------------------
    private final double SCALE = 2.0;
    private final int SIZE = 15;
    private final int CELL = (int) (25 * SCALE);
    private final int START_X = (int) (25 * SCALE);
    private final int START_Y = (int) (25 * SCALE);
    private final int STONE_SIZE = (int) (25 * SCALE);

    private final Image boardImg = new ImageIcon("src/images/board_3.png").getImage();
    private final Image blackStone = new ImageIcon("src/images/mainBlack.png").getImage();
    private final Image whiteStone = new ImageIcon("src/images/mainWhite.png").getImage();
    private final Image banImg = new ImageIcon("src/images/ban.png").getImage();

    private String[][] board = new String[SIZE][SIZE]; // 'B' 'W' 저장
    private boolean[][] isBan = new boolean[SIZE][SIZE]; // 금수 표시

    private boolean isMyTurn = false; // 현재 턴 여부
    private boolean showAll = false;  // 힌트 사용 여부

    private int lastRowB = -1, lastColB = -1; // 최근 놓인 흑돌
    private int lastRowW = -1, lastColW = -1; // 최근 놓인 백돌

    private Point[][] points = new Point[SIZE][SIZE]; // 화면 좌표

    // UI 패널
    private LifePanel lifePanel;
    private ChancePanel chancePanel;
    private JPanel sidePanel;

    // 생명, 찬스
    private int blackLives = 3, whiteLives = 3;
    private int blackChances = 2, whiteChances = 2;

    // 다시하기/나가기 버튼
    private JButton restartBtn, endBtn;


    // -----------------------------
    // 생성자
    // -----------------------------
    public OmokClient(Socket socket, BufferedReader in, PrintWriter out, char myColor) {

        this.socket = socket;
        this.in = in;
        this.out = out;
        this.myColor = myColor;

        setTitle("오목 - " + (myColor == 'B' ? "흑" : "백"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initPoints(); // 돌 좌표 계산
        initUI();     // UI 구성

        // 서버 메시지 수신 스레드
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    receiveMessage(msg.trim());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void onMessage(String msg) {
        receiveMessage(msg);
    }


    // -----------------------------
    // 보드 좌표 계산
    // -----------------------------
    private void initPoints() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                points[r][c] = new Point(
                        START_X + c * CELL,
                        START_Y + r * CELL
                );
    }


    // -----------------------------
    // UI 구성
    // -----------------------------
    private void initUI() {

        BoardPanel boardPanel = new BoardPanel(); // 바둑판 패널 구성
        int w = (int) (boardImg.getWidth(null) * SCALE);
        int h = (int) (boardImg.getHeight(null) * SCALE);
        boardPanel.setPreferredSize(new Dimension(w, h));

        sidePanel = new JPanel(); // 오른쪽 패널
        sidePanel.setPreferredSize(new Dimension(200, h));
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(240, 240, 240));

        lifePanel = new LifePanel();     // 생명 패널
        chancePanel = new ChancePanel(); // 찬스 패널

        sidePanel.add(lifePanel);
        sidePanel.add(Box.createVerticalStrut(25));
        sidePanel.add(chancePanel);

        // 찬스 버튼 클릭 처리
        chancePanel.addShowButtonListener(e -> {
            boolean canUse = chancePanel.newUseChance(
                    myColor == 'B', blackChances, whiteChances
            );
            if (canUse) {
                send("USECHANCE");
                showAll = true;
                repaint();
            }
        });

        restartBtn = new JButton("다시하기");
        restartBtn.addActionListener(e -> send("RESET"));

        endBtn = new JButton("나가기");
        endBtn.addActionListener(e -> send("END"));

        JPanel root = new JPanel(new BorderLayout());
        root.add(boardPanel, BorderLayout.CENTER);
        root.add(sidePanel, BorderLayout.EAST);

        add(root);
    }


    // -----------------------------
    // 서버로 메시지 전송
    // -----------------------------
    private void send(String msg) {
        out.println(msg);
        out.flush();
    }


    // -----------------------------
    // 서버 메시지 처리
    // -----------------------------
    public void receiveMessage(String msg) {

        String[] parts = msg.split(" ");
        String cmd = parts[0];

        switch (cmd) {

            case "TURN": // 턴 변경
                isMyTurn = (parts[1].equals("B") && myColor == 'B')
                        || (parts[1].equals("W") && myColor == 'W');

                chancePanel.updateTurn(parts[1].equals("B") ? "흑 턴" : "백 턴");
                break;

            case "MOVE": // 돌 놓기
                int r = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                String color = parts[3];

                board[r][c] = color;

                if (color.equals("B")) { lastRowB = r; lastColB = c; }
                else { lastRowW = r; lastColW = c; }

                repaint();
                break;

            case "BAN": // 금수 업데이트
                int br = Integer.parseInt(parts[1]);
                int bc = Integer.parseInt(parts[2]);
                isBan[br][bc] = true;
                repaint();
                break;

            case "LIFE": // 생명 업데이트
                if (parts[1].equals("B"))
                    blackLives = Integer.parseInt(parts[2]);
                else
                    whiteLives = Integer.parseInt(parts[2]);

                lifePanel.updateLives(blackLives, whiteLives);
                break;

            case "CHANCES": // 찬스 갱신
                if (parts[1].equals("B"))
                    blackChances = Integer.parseInt(parts[2]);
                else
                    whiteChances = Integer.parseInt(parts[2]);

                chancePanel.updateChances(myColor == 'B', blackChances, whiteChances);
                break;

            case "WIN": // 승리 처리
                JOptionPane.showMessageDialog(this,
                        parts[1].equals("B") ? "흑 승리!" : "백 승리!");

                sidePanel.add(restartBtn);
                sidePanel.add(endBtn);
                sidePanel.revalidate();
                repaint();
                break;

            case "RESET": // 게임 리셋
                resetBoard();
                repaint();
                break;

            case "END": // 게임 종료
                JOptionPane.showMessageDialog(this, "게임 종료");
                dispose();
                break;
        }
    }


    // -----------------------------
    // 내부 보드 리셋
    // -----------------------------
    private void resetBoard() {

        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = null;
                isBan[i][j] = false;
            }

        lastRowB = lastColB = -1;
        lastRowW = lastColW = -1;

        showAll = false;
    }


    // -----------------------------
    // 내부 클래스 : 바둑판 패널
    // -----------------------------
    private class BoardPanel extends JPanel implements MouseListener {

        BoardPanel() { addMouseListener(this); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = (int) (boardImg.getWidth(null) * SCALE);
            int h = (int) (boardImg.getHeight(null) * SCALE);
            g.drawImage(boardImg, 0, 0, w, h, this);

            if (showAll) drawAllStones(g); // 모든 돌 보기
            else drawLastStones(g);       // 최근 돌만 보기

            drawBans(g);                  // 금수 표시
        }

        // 전체 돌 렌더링
        private void drawAllStones(Graphics g) {

            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++) {

                    if ("B".equals(board[r][c])) {
                        g.drawImage(blackStone,
                                points[r][c].x - STONE_SIZE / 2,
                                points[r][c].y - STONE_SIZE / 2,
                                STONE_SIZE, STONE_SIZE, this);
                    }
                    else if ("W".equals(board[r][c])) {
                        g.drawImage(whiteStone,
                                points[r][c].x - STONE_SIZE / 2,
                                points[r][c].y - STONE_SIZE / 2,
                                STONE_SIZE, STONE_SIZE, this);
                    }
                }
        }

        // 마지막 돌만 표시
        private void drawLastStones(Graphics g) {

            if (lastRowB != -1)
                g.drawImage(blackStone,
                        points[lastRowB][lastColB].x - STONE_SIZE / 2,
                        points[lastRowB][lastColB].y - STONE_SIZE / 2,
                        STONE_SIZE, STONE_SIZE, this);

            if (lastRowW != -1)
                g.drawImage(whiteStone,
                        points[lastRowW][lastColW].x - STONE_SIZE / 2,
                        points[lastRowW][lastColW].y - STONE_SIZE / 2,
                        STONE_SIZE, STONE_SIZE, this);
        }

        // 금수 표시
        private void drawBans(Graphics g) {

            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    if (isBan[r][c] && board[r][c] == null)
                        g.drawImage(banImg,
                                points[r][c].x - STONE_SIZE / 2,
                                points[r][c].y - STONE_SIZE / 2,
                                STONE_SIZE, STONE_SIZE, this);
        }

        // 마우스 클릭 → 서버로 PLACE 전송
        @Override
        public void mouseClicked(MouseEvent e) {

            if (!isMyTurn) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!");
                return;
            }

            int mx = e.getX();
            int my = e.getY();

            int row = 0, col = 0;
            double minDist = Double.MAX_VALUE; // 가장 가까운 교차점 찾기

            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++) {

                    double dx = mx - points[r][c].x;
                    double dy = my - points[r][c].y;
                    double d = dx * dx + dy * dy;

                    if (d < minDist) {
                        minDist = d;
                        row = r;
                        col = c;
                    }
                }

            if (minDist > CELL * CELL / 4.0) return; // 너무 멀면 무시

            send("PLACE " + row + " " + col); // 서버에 착수 요청
            showAll = false;
        }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
    }
}
