package Client.ui;

import java.awt.*;
import javax.swing.*;

public class LifePanel extends JPanel {
    private JLabel blackLivesLabel;
    private JLabel whiteLivesLabel;

    private int blackLives = 3;
    private int whiteLives = 3;
    
    private final Image blackUser = new ImageIcon("src/images/blackUser.png").getImage();
    private final Image whiteUser = new ImageIcon("src/images/whiteUser.png").getImage();
    private final Image heartImg = new ImageIcon("src/images/heart.png").getImage();
    
    private char myColor; // 내 색깔
    private String myNickname; // 내 닉네임
    private String opponentNickname = ""; // 상대방 닉네임

    public LifePanel(char myColor, String myNickname) {
        this.myColor = myColor;
        this.myNickname = myNickname;
        setBackground(new Color(240, 240, 240)); //배경색 설정
        setPreferredSize(new java.awt.Dimension(250, 150)); // 패널 크기 설정
    }
    
    public void setOpponentNickname(String nickname) {
        this.opponentNickname = nickname;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int iconSize = 40;
        int heartSize = 35;

        g.setColor(Color.BLACK); // 그리는 색 설정(글자 색 포함)
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 18)); // 글 폰트 설정

        int heartGap = 10; // 하트 두줄의 정렬 간격
        int blackIconX = 20;
        int blackIconY = 10;

        // 흑 아이콘
        g.drawImage(blackUser, blackIconX, blackIconY, iconSize, iconSize, this);

        // 하트는 아이콘 오른쪽에 배치
        int blackHeartsX = blackIconX + iconSize + 20; //유저 아이콘의 x좌표 + 아이콘 사이즈 +20
        int blackHeartsY = blackIconY + 2;

        for (int i = 0; i < blackLives; i++) {
            g.drawImage(heartImg, blackHeartsX + i * (heartSize + heartGap), 
                        blackHeartsY, heartSize, heartSize, this);
        }

        // 흑 닉네임 표시
        int blackTextX = 21; 
        int blackTextY = blackIconY + iconSize + 20;
        String blackNick = (myColor == 'B') ? myNickname : opponentNickname;
        if (!blackNick.isEmpty()) {
            g.drawString(blackNick, blackTextX, blackTextY);
        } else {
            g.drawString("흑", blackTextX, blackTextY);
        }

        // 2. 백 영역
        int whiteIconX = 20;
        int whiteIconY = 90;

        // 백 아이콘
        g.drawImage(whiteUser, whiteIconX, whiteIconY, iconSize, iconSize, this);

        // 백 하트 아이콘 오른쪽
        int whiteHeartsX = whiteIconX + iconSize + 20;
        int whiteHeartsY = whiteIconY + 2;

        for (int i = 0; i < whiteLives; i++) {
            g.drawImage(heartImg, whiteHeartsX + i * (heartSize + heartGap), 
                        whiteHeartsY, heartSize, heartSize, this);
        }

        // 백 닉네임 표시
        int whiteTextX = 21;
        int whiteTextY = whiteIconY + iconSize + 20;
        String whiteNick = (myColor == 'W') ? myNickname : opponentNickname;
        if (!whiteNick.isEmpty()) {
            g.drawString(whiteNick, whiteTextX, whiteTextY);
        } else {
            g.drawString("백", whiteTextX, whiteTextY);
        }
    }

    // 서버에서 받은 값으로 UI 갱신
    public void updateLives(int b, int w) {
        blackLives = b;
        whiteLives = w;
        repaint();
    }

    // 패널 내부에서 직접 감소시킬 때
    public int decreaseLife(boolean isBlackTurn) {
        if (isBlackTurn) {
            if (blackLives > 0) blackLives--;
        } else {
            if (whiteLives > 0) whiteLives--;
        }

        repaint();

        if (blackLives == 0) return 0; // 흑 패배
        if (whiteLives == 0) return 1; // 백 패배
        return 2; // 계속
    }

    public void resetLife() {
        blackLives = 3;
        whiteLives = 3;
        repaint();
    }
}