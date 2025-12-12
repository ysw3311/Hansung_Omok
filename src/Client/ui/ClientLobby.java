package Client.ui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import Client.Network;

public class ClientLobby extends JFrame implements Network.MessageListener {

    // 네트워크 연결에 사용되는 소켓/입력/출력 스트림
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // 방 목록 표시용 모델 및 리스트
    private DefaultListModel<String> roomModel;
    private JList<String> roomList;

    // 로비 화면 UI 생성
    public ClientLobby(Socket socket, BufferedReader in, PrintWriter out) {

        this.socket = socket;
        this.in = in;
        this.out = out;

        setTitle("오목 - 로비");
        setSize(330, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        Color bg = new Color(245, 245, 245);
        Color buttonColor = new Color(220, 220, 220);

        // 메인 패널
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(bg);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 상단 버튼 (방 만들기 / 종료)
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        topPanel.setBackground(bg);

        JButton createRoom = new JButton("방 만들기");
        createRoom.setBackground(buttonColor);
        createRoom.addActionListener(e -> createRoom()); // 방 만들기 실행

        JButton exitBtn = new JButton("종료");
        exitBtn.setBackground(buttonColor);
        exitBtn.addActionListener(e -> System.exit(0));

        topPanel.add(createRoom);
        topPanel.add(exitBtn);

        // 방 목록 리스트
        roomModel = new DefaultListModel<>();
        roomList = new JList<>(roomModel);
        roomList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(roomList);

        // 하단 버튼 (입장하기 / 새로고침)
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        bottomPanel.setBackground(bg);

        JButton joinBtn = new JButton("입장하기");
        joinBtn.setBackground(buttonColor);
        joinBtn.addActionListener(e -> joinRoom()); // 방 입장

        JButton refreshBtn = new JButton("갱신");
        refreshBtn.setBackground(buttonColor);
        refreshBtn.addActionListener(e -> {
            out.println("ROOMLIST"); // 방 목록 요청
            out.flush();
        });

        bottomPanel.add(joinBtn);
        bottomPanel.add(refreshBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // 현재 UI가 메시지를 받을 수 있도록 Listener 구현
    @Override
    public void onMessage(String msg) {
        receiveMessage(msg);
    }

    // 방 만들기 요청 전송
    private void createRoom() {
        String name = JOptionPane.showInputDialog(this, "방 이름 입력", "새방");
        if (name == null) return;

        name = name.trim();

        if (name.contains(" ")) { // 공백 포함 방지
            JOptionPane.showMessageDialog(this, "방 이름은 공백 없이 입력하세요!");
            return;
        }

        if (name.isEmpty()) return;

        out.println("CREATEROOM " + name); // 서버로 생성 요청
        out.flush();
    }

    // 방 입장 요청 전송
    private void joinRoom() {
        String selected = roomList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "방을 선택하세요.");
            return;
        }

        String roomName = selected.split("\\(")[0]; // 괄호 앞의 방 이름만 추출
        out.println("JOINROOM " + roomName);       // 서버에 입장 요청
        out.flush();
    }

    // 서버에서 온 메시지 처리
    public void receiveMessage(String msg) {

        // 방 목록 갱신
        if (msg.startsWith("ROOMLIST")) {
            roomModel.clear();

            if (msg.equals("ROOMLIST EMPTY")) return;

            String[] parts = msg.split(" ");
            for (int i = 1; i < parts.length; i++) {
                roomModel.addElement(parts[i]); // 리스트에 추가
            }
        }

        // 방 생성 완료 알림
        else if (msg.startsWith("ROOMCREATED")) {
            JOptionPane.showMessageDialog(this, "방 생성 완료!");
        }
    }
}
