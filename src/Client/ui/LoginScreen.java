package Client.ui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginScreen extends JFrame {

    // 서버 통신을 위한 소켓 / 입력스트림 / 출력스트림
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // 닉네임 입력 필드
    private JTextField nickField;

    // ---------------------------------------
    // 로그인 화면 생성 (닉네임 입력 UI)
    // ---------------------------------------
    public LoginScreen(Socket socket, BufferedReader in, PrintWriter out) {

        this.socket = socket;
        this.in = in;
        this.out = out;

        setTitle("오목 - 로그인");
        setSize(350, 200);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);

        // 메인 패널
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 제목 텍스트
        JLabel title = new JLabel("닉네임을 입력하세요", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        // 닉네임 입력창
        nickField = new JTextField();
        nickField.setHorizontalAlignment(JTextField.CENTER);

        // 입장 버튼 → 서버에 NICK 명령 전송
        JButton btn = new JButton("입장");
        btn.addActionListener(e -> {
            String nick = nickField.getText().trim();
            if (!nick.isEmpty()) {
                out.println("NICK " + nick); // 서버에 닉네임 전달
                out.flush();
            }
        });

        panel.add(title);
        panel.add(nickField);
        panel.add(btn);

        add(panel);
    }
}
