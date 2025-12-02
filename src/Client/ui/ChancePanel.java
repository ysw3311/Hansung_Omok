package Client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ChancePanel extends JPanel {
    private JLabel blackChanceLabel; //흑 보기 기회 레이블
    private JLabel whiteChanceLabel; //백 보기 기회 레이블
    private JLabel turnLabel; // 현재 턴 표시
    private JButton showButton; // 보기 버튼

    private int blackChances = 2;
    private int whiteChances = 2;

    private final Image hintImg = new ImageIcon("src/images/hint.png").getImage();
    Image scaled = hintImg.getScaledInstance(40, 40, Image.SCALE_SMOOTH); // 이미지 버튼 크기 조절

    public ChancePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(240, 240, 240)); // 밝은 회색 배경 예시

        // 라벨과 버튼 생성
        blackChanceLabel = new JLabel("흑 보기 기회: 2");
        whiteChanceLabel = new JLabel("백 보기 기회: 2");
        turnLabel = new JLabel("턴: -");
        showButton = new JButton(new ImageIcon(scaled));
        showButton.setPreferredSize(new Dimension(50, 50)); // 버튼 크기 조절

        // 가운데 정렬
        blackChanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        whiteChanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        showButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 간격 + 구성요소 추가
        add(Box.createVerticalStrut(20)); // 위쪽 여백
        add(blackChanceLabel);
        add(Box.createVerticalStrut(6)); // 흑/백 라벨 사이 간격
        add(whiteChanceLabel);
        add(Box.createVerticalStrut(10));
        add(turnLabel);
        add(Box.createVerticalStrut(12)); // 아래쪽 여백
        add(showButton);
    }

    // 버튼 리스너를 외부에서 지정할 수 있도록
    public void addShowButtonListener(ActionListener listener) {
        showButton.addActionListener(listener);
    }

    // 상태 업데이트용 메서드
    public void updateLabels(boolean isBlackTurn) {
        blackChanceLabel.setText("흑 보기 기회: " + blackChances);
        whiteChanceLabel.setText("백 보기 기회: " + whiteChances);
        showButton.setEnabled((isBlackTurn && blackChances > 0) || (!isBlackTurn && whiteChances > 0)); // 기회를 다 쓰면 버튼 비활성화
    }
    
    //new 상태 업데이트용 메서드
    public void updateChances(boolean isMyBlack, int b, int w) {
    	blackChanceLabel.setText("흑 보기 기회: " + b);
    	whiteChanceLabel.setText("백 보기 기회: " + w);
    	showButton.setEnabled((isMyBlack && b > 0) || (!isMyBlack && w > 0)); // 기회를 다 쓰면 버튼 비활성화
    }
    
  //new 턴 업데이트용 메서드
    public void updateTurn(String str) {
    	turnLabel.setText("턴: " + str);
    }
    
    // new 버튼 클릭 시 기회 차감
    public boolean newUseChance(boolean isMyBlack, int blackChances, int whiteChances) {
        if (isMyBlack && blackChances > 0) {
            return true;
        } else if (!isMyBlack && whiteChances > 0) {
            return true;
        }
        return false;
    }

    // 버튼 클릭 시 기회 차감
    public boolean useChance(boolean isBlackTurn) {
        if (isBlackTurn && blackChances > 0) {
            blackChances--;
            return true;
        } else if (!isBlackTurn && whiteChances > 0) {
            whiteChances--;
            return true;
        }
        return false;
    }

    // 게임 리셋 시 기회 복구
    public void resetChances() {
        blackChances = 2;
        whiteChances = 2;
        showButton.setEnabled(true); // 버튼 활성화
        updateLabels(true); // 새로고침
        setTurnLabel("턴: -");
    }

    // 새로 추가: 턴 표시 갱신
    public void setTurnLabel(String text) {
        turnLabel.setText(text);
    }
}
