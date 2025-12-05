package Client.ui;

import java.awt.*;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ChancePanel extends JPanel {

    private int blackChances = 2;
    private int whiteChances = 2;
    private boolean isBlack = true;

    private final Image hintImg = new ImageIcon("src/images/hint.png").getImage();

    private JButton showButton;
    private String turnText = "턴: -";

    public ChancePanel() {
        setBackground(new Color(240, 240, 240)); // 배경색 설정
        setPreferredSize(new java.awt.Dimension(250, 130)); // 패널 크기 설정(너비, 높이)
        setLayout(null);

        // 힌트 버튼 (이미지 버튼)
        Image scaled = hintImg.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        showButton = new JButton(new ImageIcon(scaled)); // 버튼 생성
        showButton.setBounds(170, 50, 50, 50); //버튼 위치 설정(x, y, width, height)
        showButton.setFocusable(false);
        add(showButton);
    }

    // 외부에서 리스너 등록
    public void addShowButtonListener(ActionListener listener) {
        showButton.addActionListener(listener);
    }

    @Override
    protected void paintComponent(Graphics g) { // 컴포넌트 그리기
        super.paintComponent(g);

        int hintSize = 40;
        g.setColor(Color.BLACK); // 그리는 색 설정(글자 색 포함)
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 18)); // 글자 폰트 설정

        // --- 턴 표시 ---
        g.drawString(turnText, 30, 40); // 턴 50, 40 위치에 표시
        
        if(isBlack) {
        // --- 흑 힌트 ---
        	for (int i = 0; i < blackChances; i++) {
        		g.drawImage(hintImg, 20 + i * 45, 60, hintSize, hintSize, this); // (그릴 이미지 객체, 이미지 그릴 왼쪽 위 좌표, 이미지 너비, 이미지 높이, 이 객체)
        	}
        }
        else {
        // --- 백 힌트 ---
        	for (int i = 0; i < whiteChances; i++) {
        		g.drawImage(hintImg, 20 + i * 45, 60, hintSize, hintSize, this); // hintSize가 40이므로 높이는 100이 된다.
        	}
        }
    }

    public void determineBlack (boolean isBlack) { // 흑돌인지 백돌인지 판별
    	this.isBlack = isBlack;
    }
    
    public void updateChances(boolean isBlack, int b, int w) {
        blackChances = b;
        whiteChances = w;
        showButton.setEnabled((isBlack && b > 0) || (!isBlack && w > 0));
        repaint();
    }

    public void updateTurn(String turn) {
        this.turnText = "턴: " + turn;
        repaint();
    }

    public boolean newUseChance(boolean isBlack, int blackC, int whiteC) {
        if (isBlack && blackC > 0) return true;
        if (!isBlack && whiteC > 0) return true;
        return false;
    }
    
    public void resetChances() {
        blackChances = 2;
        whiteChances = 2;
        turnText = "턴: -";
        showButton.setEnabled(true);
        repaint();
    }
}
