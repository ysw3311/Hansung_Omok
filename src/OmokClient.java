

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;

public class OmokClient extends JFrame {
	// 확대 비율 (1.0 = 원본, 2.0 = 2배 확대)
    private final double SCALE = 2.0;

    // 기본 보드 설정 (원본 400x400 이미지 기준)
    private final int SIZE = 15;
    private final int BASE_CELL = 25;
    private final int BASE_START_X = 25;
    private final int BASE_START_Y = 25;
    private final int BASE_STONE_SIZE = 25;

    // 확대 적용된 값 (자동 계산)
    private final int CELL = (int) Math.round(BASE_CELL * SCALE);
    private final int START_X = (int) Math.round(BASE_START_X * SCALE);
    private final int START_Y = (int) Math.round(BASE_START_Y * SCALE);
    private final int STONE_SIZE = (int) Math.round(BASE_STONE_SIZE * SCALE);

    private final Image boardImg = new ImageIcon("src/images/board_3.png").getImage();
    private final Image blackStone = new ImageIcon("src/images/mainBlack.png").getImage();
    private final Image whiteStone = new ImageIcon("src/images/mainWhite.png").getImage();
    private final Image banImg = new ImageIcon("src/images/ban.png").getImage();

    private String[][] board = new String[SIZE][SIZE]; // 각 칸의 상태 (B, W, null)
    private boolean[][] isBan = new boolean[SIZE][SIZE]; // 오목판의 교차점 좌표(화면 픽셀 좌표)
   // private boolean isBlackTurn = true; // 현재 차례

    private int lastRowB = -1; //가장 마지막에 놓은 돌 행
    private int lastColB = -1; //가장 마지막에 놓은 돌 열
    private int lastRowW = -1; //가장 마지막에 놓은 돌 행
    private int lastColW = -1; //가장 마지막에 놓은 돌 열

    private boolean showAll = false; // 전체 돌 보기 상태
    
    private boolean isMyBlack = false;  // 서버에서 할당 받음, 흑과 백
    private boolean isAssigned = false; //역할 할당 되었는지 확인하는 변수
    private boolean isMyTurn = false; // 현재 클라이언트 턴인지 확인하는 변수
    
    private int blackLives = 3;
    private int whiteLives = 3;
    private int blackChances = 2;
    private int whiteChances = 2;
    
    private LifePanel lifePanel;
    private ChancePanel chancePanel;
    private JPanel sidePanel;
    JButton reStartButton;
    JButton endButton;
    
    private Point[][] points = new Point[SIZE][SIZE];

    private Socket socket; // 연결소켓
    private BufferedReader in;
    private PrintWriter out;
    
    public OmokClient() {
        setTitle("오목 클라이언트");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 좌표 계산
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                points[i][j] = new Point(
                        START_X + j * CELL,
                        START_Y + i * CELL
                );
            }
        }
        // 금수 좌표 초기화 
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                isBan[r][c] = false;

        BoardPanel panel = new BoardPanel(); //오목판과 돌을 그림, 마우스 클릭 이벤트 처리
        int boardWidth = (int) Math.round(boardImg.getWidth(null) * SCALE);
        int boardHeight = (int) Math.round(boardImg.getHeight(null) * SCALE);
        panel.setPreferredSize(new Dimension(boardWidth, boardHeight));

        // 사이드 패널 생성 및 이벤트 연결
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(200, boardHeight));
        sidePanel.setBackground(new Color(240, 240, 240));

        lifePanel = new LifePanel(); // 목숨 보여주는 패널 생성
        chancePanel = new ChancePanel(); // 힌트 패널 생성
        
        sidePanel.add(lifePanel); // 사이드 패널에 붙이기
        sidePanel.add(Box.createVerticalStrut(30));
        sidePanel.add(chancePanel);
        sidePanel.add(Box.createVerticalStrut(30));
        
        // 힌트 패널 이벤트 넣기
        chancePanel.addShowButtonListener(e -> {
        	String color;
            if (chancePanel.newUseChance(isMyBlack, blackChances, whiteChances)) {
            	sendToServer("USECHANCE"); // 서버로 메시지 보내기
                showAll = true;
                //chancePanel.updateChances(isMyBlack, blackChances, whiteChances); // 상태 업데이트 & 버튼 활성 상태 지정
                panel.repaint(); // showAll = true인 상태로 BoardPanel의 paintComponent(g)호출 (다시 그리기)
            }
        });
        
        // 다시하기 버튼
        reStartButton = new JButton("다시하기");
        reStartButton.addActionListener(e -> {
        	sendToServer("RESET");
        });
        // 종료하기 버튼
        endButton = new JButton("종료하기");
        endButton.addActionListener(e -> {
        	sendToServer("END");
        });
        
        // 전체 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panel, BorderLayout.CENTER); // 메인 패널 중앙에 바둑판 추가
        mainPanel.add(sidePanel, BorderLayout.EAST); // 메인패널 오른쪽에 사이드패널 추가

        add(mainPanel);
        pack(); //pack()은 JFrame의 메서드로, 프레임 안의 모든 컴포넌트가 필요한 크기만큼만 딱 맞게 윈도우 크기를 조정
        setLocationRelativeTo(null); //화면(모니터)의 정중앙을 기준으로 배치
        setVisible(true); //창을 화면에 표시
        connectToServer("127.0.0.1", 9999);
    }
    
    // 메시지 전송
    private void sendToServer(String msg) {
    	out.println(msg);
    }
    
    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            ListenNetwork net = new ListenNetwork();
            net.start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }
    
    class ListenNetwork extends Thread {
        public void run() {
            try {
                while (true) {
                    String line = in.readLine(); //서버로 부터 메시지를 받기
                    System.out.println("recv: " + line); // 메시지 출력
                    handleServerMessage(line); // 메시지 처리
                }
            } catch (IOException e) {
                System.out.println("서버 연결 종료");
            }
        }
    }
    
    private void handleServerMessage(String msg) {
            String[] parts = msg.split(" ");
            String cmd = parts[0];

            switch (cmd) {
                case "START":
                    if (parts.length >= 2) {
                        if ("B".equals(parts[1])) {
                            isMyBlack = true;
                            isAssigned = true;
                            setTitle("오목 흑");
                        } else {
                            isMyBlack = false;
                            isAssigned = true;
                            setTitle("오목 백");
                        }
                    }
                    break;
                case "TURN":
                	// TURN B/W
                    if (parts.length >= 2) {
                        isMyTurn = (parts[1].equals("B") && isMyBlack) || (parts[1].equals("W") && !isMyBlack);
                        String turnStr = parts[1].equals("B") ? "흑 턴" : "백 턴";
                        chancePanel.updateTurn(turnStr);
                    }
                    break;
                case "MOVE":
                    // MOVE r c B/W
                    int r = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    String color = parts[3];
                    board[r][c] = color;
                    if(color.equals("B")) {
                    	lastRowB = r; lastColB = c;
                    } else {
                    	lastRowW = r; lastColW = c;
                    }
                    repaint();
                    break;
                case "BAN":
                    // BAN r c
                    int br = Integer.parseInt(parts[1]);
                    int bc = Integer.parseInt(parts[2]);
                    isBan[br][bc] = true;
                    repaint();
                    break;
                case "LIFE":
                    // LIFE b/w 횟수
                	if("B".equals(parts[1])) {
                		blackLives = Integer.parseInt(parts[2]);
                	} else {
                		whiteLives = Integer.parseInt(parts[2]);
                	}
                    lifePanel.updateLives(blackLives, whiteLives);
                    break;
                case "CHANCES":
                    // CHANCES b/w 횟수
                	if("B".equals(parts[1])) {
                		blackChances = Integer.parseInt(parts[2]);
                	} else {
                		whiteChances = Integer.parseInt(parts[2]);
                	}
                    chancePanel.updateChances(isMyBlack, blackChances, whiteChances);
                    break;
                case "WIN":
                    // WIN B/W
                    String winner = parts[1];
                    JOptionPane.showMessageDialog(this, (winner.equals("B") ? "흑 승리!" : "백 승리!"));
                    sidePanel.add(reStartButton);
                    sidePanel.add(endButton);
                    showAll = true;
                    sidePanel.revalidate();   // 레이아웃 재계산
                    repaint();
                    break;
                case "RESET":
                    resetLocalBoard();
                    break;
                case "END":
                    JOptionPane.showMessageDialog(this, "게임이 종료되었습니다.");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown server msg: " + msg);
            }
    }
    
    private void resetLocalBoard() {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = null;
                isBan[i][j] = false;
            }

        lastRowB = -1;
        lastColB = -1;
        lastRowW = -1;
        lastColW = -1;

        showAll = false;

        // 리셋 후에도 계속 게임할 수 있도록 설정
        isAssigned = true;     // 플레이어 역할 유지시키기
        isMyTurn = false;      // 서버에서 TURN 메시지 받을 때 true됨

        // UI 갱신
        lifePanel.updateLives(blackLives, whiteLives);
        chancePanel.updateChances(isMyBlack, blackChances, whiteChances);
        repaint();
    }


    // 내부 패널
    class BoardPanel extends JPanel implements MouseListener {
        public BoardPanel() {
            addMouseListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 보드 이미지 확대해서 그리기
            int w = (int) Math.round(boardImg.getWidth(null) * SCALE);
            int h = (int) Math.round(boardImg.getHeight(null) * SCALE);
            g.drawImage(boardImg, 0, 0, w, h, this);

            
            // 전체 돌 그리기
            if (showAll) { // 힌트 버튼을 눌렀다면
                // 전체 돌 표시 + 금수 표시
                for (int i = 0; i < SIZE; i++) {
                    for (int j = 0; j < SIZE; j++) {
                        if ("B".equals(board[i][j])) {
                            g.drawImage(blackStone,
                                    points[i][j].x - STONE_SIZE / 2,
                                    points[i][j].y - STONE_SIZE / 2,
                                    STONE_SIZE, STONE_SIZE, this);
                        } else if ("W".equals(board[i][j])) {
                            g.drawImage(whiteStone,
                                    points[i][j].x - STONE_SIZE / 2,
                                    points[i][j].y - STONE_SIZE / 2,
                                    STONE_SIZE, STONE_SIZE, this);
                        }
                        if (isBan[i][j] && board[i][j] == null) {
                            g.drawImage(banImg,
                                    points[i][j].x - STONE_SIZE / 2,
                                    points[i][j].y - STONE_SIZE / 2,
                                    STONE_SIZE, STONE_SIZE, this);
                        }
                    }
                }
            }
            else if (lastRowB != -1 && lastColB != -1 && board[lastRowB][lastColB] != null) { // 돌이 놓였다면
            	Image stoneb = blackStone;
                g.drawImage(stoneb,
                        points[lastRowB][lastColB].x - STONE_SIZE / 2,
                        points[lastRowB][lastColB].y - STONE_SIZE / 2,
                        STONE_SIZE, STONE_SIZE, this);
                if(lastRowW != -1 && lastColW != -1 && board[lastRowW][lastColW] != null) {
                	Image stonew = whiteStone;
                    g.drawImage(stonew,
                            points[lastRowW][lastColW].x - STONE_SIZE / 2,
                            points[lastRowW][lastColW].y - STONE_SIZE / 2,
                            STONE_SIZE, STONE_SIZE, this);
                }
                // 금수 표시
                for (int i = 0; i < SIZE; i++) {
                    for (int j = 0; j < SIZE; j++) {
                    	if (isBan[i][j] && board[i][j] == null) {
                            g.drawImage(banImg,
                                    points[i][j].x - STONE_SIZE / 2,
                                    points[i][j].y - STONE_SIZE / 2,
                                    STONE_SIZE, STONE_SIZE, this);
                        }
                    }
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        	if (!isAssigned) {
                JOptionPane.showMessageDialog(this, "아직 역할 할당 전입니다.");
                return;
            }
            if (!isMyTurn) {
                JOptionPane.showMessageDialog(this, "지금은 당신의 턴이 아닙니다.");
                return;
            }
            
            int mx = e.getX();
            int my = e.getY();

            // 클릭한 곳에서 가장 가까운 교차점 찾기
            int row = 0, col = 0;
            double minDist = Double.MAX_VALUE;

            // 클릭 좌표 → 가장 가까운 좌표 찾기
            for (int r = 0; r < SIZE; r++) { // 0행부터 비교하면서 가장 가까운곳 찾기
                for (int c = 0; c < SIZE; c++) {
                    double dx = mx - points[r][c].x;
                    double dy = my - points[r][c].y;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < minDist) {
                        minDist = dist;
                        row = r; // 가장 가까운 곳의 행, 열 저장
                        col = c;
                    }
                }
            }
         // 범위 밖 클릭 무시
            if (minDist > CELL * 0.5) return;
         // 클릭시 서버에 요청 전송 (서버가 유효성 검사 수행)
            sendToServer("PLACE " + row + " " + col);
            showAll = false; // 새 돌을 두면 전체보기 해제
            }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
    }

    public static void main(String[] args) {
        new OmokClient();
    }
}
