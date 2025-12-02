package Client;

import java.io.*;
import java.net.Socket;

/**
 * Network
 * - 클라이언트의 소켓 통신을 담당
 * - 서버로부터 메시지를 지속적으로 수신하여 Listener(=ClientMain)에게 전달
 */
public class Network {

    // 서버 메시지를 전달받을 리스너 인터페이스
    public interface MessageListener {
        void onMessage(String msg);
    }

    private MessageListener listener;  // 메시지 전달 대상

    private Socket socket;             // 서버 소켓
    private BufferedReader in;         // 입력 스트림(서버 → 클라이언트)
    private PrintWriter out;           // 출력 스트림(클라이언트 → 서버)

    public Network(MessageListener listener) {
        this.listener = listener;      // ClientMain을 listener로 등록
    }

    // 서버 연결 시도
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port); // 서버 접속
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 서버 메시지 읽기
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // 서버로 전송

            // 서버 메시지를 계속 읽는 스레드
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readLine();
                        if (msg == null) break;  // 서버 종료
                        listener.onMessage(msg); // ClientMain으로 메시지 전달
                    }
                } catch (Exception e) {
                    System.out.println("서버 연결 종료");
                }
            }).start();

            return true; // 연결 성공

        } catch (Exception e) {
            return false; // 연결 실패
        }
    }

    // 서버로 메시지 전송
    public void send(String msg) {
        out.println(msg);
    }

    // 소켓 및 스트림 getter
    public Socket getSocket() { return socket; }
    public BufferedReader getIn() { return in; }
    public PrintWriter getOut() { return out; }
}
