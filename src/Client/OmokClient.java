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

    // 네트워크 및 플레이어 정보
    private Socket socket;
    private BufferedReader in;    // 서버 → 메시지 수신
    private PrintWriter out;      // 서버 ← 메시지 전송
    private char myColor;         // 'B' or 'W'
    private String myNickname;    // 내 닉네임
    private String opponentNickname; // 상대방 닉네임

    // 보드 관련 기본 설정
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
    private JPanel endPanel; //종료시 다시하기, 종료하기 버튼 나오는 패널
    private JPanel lastPanel; // 무르기, 보내기 버튼 추가하는 패널
    private JButton reStartButton;
    private JButton endButton;
    private JButton cancelButton; // 무르기 버튼
    
    // 채팅 추가
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;

    // 생명, 찬스
    private int blackLives = 3, whiteLives = 3;
    private int blackChances = 2, whiteChances = 2;

    // 생성자
    public OmokClient(Socket socket, BufferedReader in, PrintWriter out, char myColor, String nickname) {

        this.socket = socket;
        this.in = in;
        this.out = out;
        this.myColor = myColor;
        this.myNickname = nickname;

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

    // UI 구성
    private void initUI() {
    	BoardPanel panel = new BoardPanel(); //오목판과 돌을 그림, 마우스 클릭 이벤트 처리
        int boardWidth = (int) Math.round(boardImg.getWidth(null) * SCALE);
        int boardHeight = (int) Math.round(boardImg.getHeight(null) * SCALE);
        panel.setPreferredSize(new Dimension(boardWidth, boardHeight));

        // 사이드 패널 생성 및 이벤트 연결
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(250, boardHeight));
        sidePanel.setBackground(new Color(240, 240, 240));

        lifePanel = new LifePanel(myColor, myNickname); // 목숨 보여주는 패널 생성
        chancePanel = new ChancePanel(); // 힌트 패널 생성
        chancePanel.determineBlack(myColor == 'B'); // chancePanel에서 흑돌인지 백돌인지 판별
        endPanel = new JPanel(); // 종료 시 다시하기, 종료하기 버튼 나오도록 하는 패널 생성
        endPanel.setPreferredSize(new Dimension(250, 40));  // 250, 40 크기 설정
        endPanel.setBackground(new Color(240, 240, 240));
        
        // 채팅 영역
        // 채팅 패널 (여백을 주기 위해 감싸는 패널)
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBackground(new Color(240, 240, 240));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); 
        // ↑ 좌우 여백 10px

        // 채팅 영역
        chatArea = new JTextArea(); 
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setPreferredSize(new Dimension(200, 250)); // ← 높이 약간 조정

        // 채팅 입력창
        chatInput = new JTextField();
        chatInput.setPreferredSize(new Dimension(150, 25)); // ← 입력창 높이 얇게(20→25 또는 반대로 줄여도 됨)

        // 보내기 버튼
        sendButton = new JButton("보내기");

        sendButton.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());

        // 하단 입력창 묶는 패널
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // 패널에 배치
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        //채팅 영역 끝
        
        // 힌트 패널 이벤트 넣기
        chancePanel.addShowButtonListener(e -> {
        	String color;
            if (chancePanel.newUseChance(myColor == 'B', blackChances, whiteChances)) {
            	sendToServer("USECHANCE"); // 서버로 메시지 보내기
                showAll = true;
                panel.repaint(); // showAll = true인 상태로 BoardPanel의 paintComponent(g)호출 (다시 그리기)
            }
        });
        
        // 무르기 버튼
        cancelButton = new JButton("무르기 요청");
        cancelButton.setEnabled(false); // 처음에 버튼 비활성화
        cancelButton.addActionListener(e -> {
            sendToServer("CANCEL"); 
            cancelButton.setEnabled(false); // 요청 보낸 후 버튼 비활성화
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
        	dispose(); // 창 닫기
        });
        
        lastPanel = new JPanel(); // 무르기, 보내기 버튼 나오도록 하는 패널 생성
        lastPanel.setPreferredSize(new Dimension(250, 40));  // 250, 40 크기 설정
        lastPanel.setBackground(new Color(240, 240, 240));
        lastPanel.add(cancelButton);
        lastPanel.add(sendButton);
        
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(lifePanel); // 사이드 패널에 생명 패널 붙이기
        sidePanel.add(Box.createVerticalStrut(30));
        sidePanel.add(chancePanel); // 사이드 패널에 힌트 패널 붙이기
        sidePanel.add(endPanel);
        sidePanel.add(chatPanel); // 사이드 패널에 채팅창 붙이기
        sidePanel.add(lastPanel); // 무르기, 보내기 버튼
        
        // 전체 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panel, BorderLayout.CENTER); // 메인 패널 중앙에 바둑판 추가
        mainPanel.add(sidePanel, BorderLayout.EAST); // 메인패널 오른쪽에 사이드패널 추가

        add(mainPanel);
        pack(); //pack()은 JFrame의 메서드로, 프레임 안의 모든 컴포넌트가 필요한 크기만큼만 딱 맞게 윈도우 크기를 조정
        setLocationRelativeTo(null); //화면(모니터)의 정중앙을 기준으로 배치
        setVisible(true); //창을 화면에 표시
    }

    // 서버로 메시지 전송
    private void sendToServer(String msg) {
    	out.println(msg);
    }

    private void sendChat() {
        String msg = chatInput.getText().trim(); //공백 제거하여 msg에 저장 
        if (msg.isEmpty()) return;

        out.println("CHAT " + myNickname + " " + msg); // 닉네임과 함께 서버로 전송
        chatInput.setText(""); // 서버로 전송한 후 채팅 작성창 초기화
    }

    // 서버 메시지 처리
    public void receiveMessage(String msg) {
    	String[] parts = msg.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case "TURN":
            	// TURN B/W 상대방닉네임
                if (parts.length >= 2) {
                    isMyTurn = (parts[1].equals("B") && myColor == 'B') || (parts[1].equals("W") && myColor == 'W');
                    String turnStr = parts[1].equals("B") ? "흑 턴" : "백 턴";
                    chancePanel.updateTurn(turnStr);
                    
                    // 상대방 닉네임 저장 (서버에서 항상 상대방 닉네임을 보내줌)
                    if (parts.length >= 3) {
                        opponentNickname = parts[2];
                        lifePanel.setOpponentNickname(opponentNickname);
                    }
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
                // 방금 놓인 돌(color)이 내 색깔(isMyBlack)과 같으면 -> 내가 둔 것 -> 활성화
                // 다르면 -> 상대방이 둔 것 -> 비활성화 (이미 턴이 넘어가서 내 이전 수를 무를 수 없음)
                if (color.equals(String.valueOf(myColor))) {
                    cancelButton.setEnabled(true);  // 내가 뒀으니 무르기 가능
                } else {
                    cancelButton.setEnabled(false); // 상대가 뒀으니 무르기 불가
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
            case "BAN_CLEAR": // ★ [추가] 금수 초기화 명령 처리
                for (int i = 0; i < SIZE; i++) {
                    for (int j = 0; j < SIZE; j++) {
                        isBan[i][j] = false; // 모든 금수 해제
                    }
                }
                repaint(); // 화면 갱신
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
                chancePanel.updateChances(myColor == 'B', blackChances, whiteChances);
                break;
            case "WIN":
                // WIN B/W
                String winner = parts[1];
                JOptionPane.showMessageDialog(this, (winner.equals("B") ? "흑 승리!" : "백 승리!"));
                endPanel.add(reStartButton); // 리셋 버튼 추가
                endPanel.add(endButton); // 종료 버튼 추가
                showAll = true;
                sidePanel.revalidate();   // 레이아웃 재계산
                repaint();
                break;
            case "RESET":
                resetLocalBoard();
                break;
            case "CHAT":
                // CHAT 닉네임 메시지
                if (parts.length >= 3) {
                    String nickname = parts[1];
                    String chatMsg = msg.substring(5 + nickname.length() + 1); // "CHAT " + 닉네임 + " " 이후
                    chatArea.append("[" + nickname + "] " + chatMsg + "\n"); // 채팅창에 메시지 추가
                    chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 채팅창 스크롤을 가장 아래로 스크롤
                }
                break;
            case "END":
                // END 이후 메시지 전체 가져오기
                String endMsg = msg.substring(4); //문자열의 5번 인덱스부터 끝까지 반환
                chatArea.append(endMsg + "\n"); // 채팅창에 메시지 추가
                chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 채팅창 스크롤을 가장 아래로 스크롤
                break;
             // 상대방이 무르기를 요청했을 때
            case "CANCEL_ASK":
                int choice = JOptionPane.showConfirmDialog(this, 
                        "상대방이 무르기를 요청했습니다.\n수락하시겠습니까?", 
                        "무르기 요청", 
                        JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    sendToServer("CANCEL_YES");
                } else {
                    sendToServer("CANCEL_NO");
                }
                break;

            // 상대방이 내 요청을 거절했을 때
            case "CANCEL_DENIED":
                JOptionPane.showMessageDialog(this, "상대방이 무르기를 거절했습니다.");
                break;

            // 무르기 확정
            case "UNDO":
            	// 프로토콜: UNDO [지울R] [지울C] [이전R] [이전C] [색상]
            	int ur = Integer.parseInt(parts[1]);
                int uc = Integer.parseInt(parts[2]);
                int prevR = Integer.parseInt(parts[3]);
                int prevC = Integer.parseInt(parts[4]);
                String ucolor = parts[5];

                // 보드에서 돌 삭제
                board[ur][uc] = null;

                // 마지막 돌 위치 정보를 "직전 돌" 위치로 갱신
                if (ucolor.equals("B")) {
                    lastRowB = prevR;
                    lastColB = prevC;
                } else {
                    lastRowW = prevR;
                    lastColW = prevC;
                }
                // 무르기가 완료되었으므로 버튼 비활성화
                cancelButton.setEnabled(false);
                // 화면 다시 그리기
                repaint();
            default:
                System.out.println("Unknown server msg: " + msg);
        }
    }


    // 게임 리셋 함수
    private void resetLocalBoard() { 
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = null;
                isBan[i][j] = false;
            }
        lastRowB = -1; lastColB = -1;
        lastRowW = -1; lastColW = -1;
        showAll = false;
        lifePanel.updateLives(blackLives, whiteLives);
        chancePanel.updateChances(myColor == 'B', blackChances, whiteChances);
        cancelButton.setEnabled(false); // 무르기 버튼 비활성화
        // 다시하기, 종료하기 버튼 제거
        endPanel.remove(reStartButton); // 다시하기 버튼 제거
        endPanel.remove(endButton); // 종료하기 버튼 제거
        sidePanel.revalidate(); //레이아웃 재계산
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
        	//if (!isAssigned) {
                //JOptionPane.showMessageDialog(this, "아직 역할 할당 전입니다.");
                //return;
            //}
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
}