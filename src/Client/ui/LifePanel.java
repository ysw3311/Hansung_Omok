package Client.ui;


import java.awt.Color;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LifePanel extends JPanel {
    private JLabel blackLivesLabel;
    private JLabel whiteLivesLabel;

    private int blackLives = 3;
    private int whiteLives = 3;

    public LifePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // 세로 방향으로 쌓기
        setBackground(new Color(240, 240, 240));

        blackLivesLabel = new JLabel("흑 ❤️❤️❤️"); // 흑 목숨 보여주는 레이블
        whiteLivesLabel = new JLabel("백 ❤️❤️❤️"); // 백 목숨 보여주는 레이블

        blackLivesLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        whiteLivesLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬

        add(Box.createVerticalStrut(20)); //여백 20
        add(blackLivesLabel);
        add(Box.createVerticalStrut(10));
        add(whiteLivesLabel);
    }

    // 이미 돌이 있는 곳 클릭 시 목숨 감소
    public int decreaseLife(boolean isBlackTurn) {
        if (isBlackTurn) { //흑돌 턴일 때 흑돌 목숨 -1
            if (blackLives > 0) blackLives--;
        } else { // 백돌 턴일 때 백돌 목숨 -1
            if (whiteLives > 0) whiteLives--;
        }
        updateLabels();
        
        if(blackLives == 0) {
        	return 0; // 흑 패배
        }
        else if(whiteLives == 0) {
        	return 1; // 백 패배
        }
        else { // 목숨이 남아있을 시
        	return 2;
        }
    }

    public void updateLabels() {
        blackLivesLabel.setText("흑 " + "❤️".repeat(blackLives)); // repeat(blackLives): blackLives 횟수만큼 반복해서 하트 붙이기
        whiteLivesLabel.setText("백 " + "❤️".repeat(whiteLives));
    }
    
    // 새로 만든거
    public void updateLives(int b, int w) {
    	blackLivesLabel.setText("흑 " + "❤️".repeat(b));
    	whiteLivesLabel.setText("백 " + "❤️".repeat(w));
    }

    public void resetLife() { // 리셋 실행 시 목숨 초기화
        blackLives = 3;
        whiteLives = 3;
        updateLabels();
    }
}