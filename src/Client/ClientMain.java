package Client;

import Client.ui.LoginScreen;
import Client.ui.ClientLobby;
import Client.OmokClient;

import javax.swing.*;

public class ClientMain implements Network.MessageListener {

    private JFrame currentUI;      // 현재 표시 중인 화면
    private Network network;       // 서버와 통신 담당
    private String myNickname;     // 내 닉네임 저장

    public ClientMain() {

        network = new Network(this);                    // 메시지 리스너 등록
        boolean ok = network.connect("127.0.0.1", 9999); // 서버 연결

        if (!ok) {
            JOptionPane.showMessageDialog(null, "서버 연결 실패!");
            System.exit(0);
        }

        showLogin();  // 첫 화면: 로그인
    }

    public static void main(String[] args) {
        new ClientMain();
    }

    // 화면 전환 공통 함수
    private void changeUI(JFrame next) {
        if (currentUI != null) currentUI.dispose(); // 기존 화면 닫기
        currentUI = next;
        currentUI.setVisible(true);                // 새 화면 표시
    }

    // 로그인 화면 표시
    private void showLogin() {
        changeUI(new LoginScreen(
                network.getSocket(),
                network.getIn(),
                network.getOut()
        ));
    }

    // 로비 화면 표시
    private void showLobby() {
        changeUI(new ClientLobby(
                network.getSocket(),
                network.getIn(),
                network.getOut()
        ));

        network.send("ROOMLIST"); // 입장 시 방 목록 요청
    }

    // 게임 화면 표시
    private void showGame(char color, String nickname) {
        changeUI(new OmokClient(
                network.getSocket(),
                network.getIn(),
                network.getOut(),
                color,            // 내 돌 색(B/W)
                nickname          // 닉네임
        ));
    }

    // 서버 메시지 처리
    @Override
    public void onMessage(String msg) {

        System.out.println("[CLIENT] RECV: " + msg);

        if (msg.startsWith("NICKOK")) { // 닉네임 등록 성공
            // 닉네임 저장 (LoginScreen에서 입력한 값)
            if (currentUI instanceof LoginScreen loginScreen) {
                myNickname = loginScreen.getNickname();
            }
            showLobby();
            return;
        }

        if (msg.startsWith("JOINED")) { // 방 입장 성공
            String[] sp = msg.split(" ");
            char color = sp[2].charAt(0); // B/W
            showGame(color, myNickname);
            return;
        }

        // 현재 UI가 메시지 처리할 수 있으면 전달
        if (currentUI instanceof Network.MessageListener listener) {
            listener.onMessage(msg);
        }
    }
}
