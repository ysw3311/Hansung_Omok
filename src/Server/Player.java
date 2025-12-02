package Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * Player
 * ------------------------------------------------------
 * - 서버에 연결된 클라이언트 1명을 표현
 * - 로비 상태 / 게임 상태를 자동 구분하여 처리
 * - 서버에서 받은 메시지를 해석하는 역할은 하지 않음
 *   (해석은 OmokServer 또는 Room이 담당)
 * ------------------------------------------------------
 */

public class Player implements Runnable {

    private Socket socket;
    private OmokServer server;      // 로비 메시지 처리 담당
    private Room room;              // 현재 들어간 방
    private String nickname = "Unknown"; // 기본 닉네임
    private boolean inGame = false; // 게임 중 여부

    private BufferedReader in;
    private PrintWriter out;

    public Player(Socket socket, OmokServer server) {
        this.socket = socket;
        this.server = server;

        try {
            // 입력 스트림
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8")
            );

            // 출력 스트림
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                    true   // auto-flush
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------
    // 서버 → 클라이언트 메시지 전달
    // ------------------------------------------
    public void send(String msg) {
        out.println(msg);
    }

    // ------------------------------------------
    // 방 getter/setter
    // ------------------------------------------
    public void setRoom(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    // ------------------------------------------
    // 게임 중 상태 플래그
    // ------------------------------------------
    public void setInGame(boolean flag) {
        this.inGame = flag;
    }

    public boolean isInGame() {
        return inGame;
    }

    // ------------------------------------------
    // 닉네임 설정/조회
    // ------------------------------------------
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }


    // ------------------------------------------
    // Player 스레드: 클라이언트 메시지 수신 루프
    // ------------------------------------------
    @Override
    public void run() {

        try {
            String msg;

            // 클라이언트로부터 메시지 계속 읽기
            while ((msg = in.readLine()) != null) {

                msg = msg.trim();
                if (msg.isEmpty()) continue;

                System.out.println("[RECV " + nickname + "] " + msg);

                // --------------------------
                // 게임 중이면 Room에서 처리
                // --------------------------
                if (inGame && room != null) {
                    room.handleGameMessage(this, msg);
                }

                // --------------------------
                // 로비 상태 → OmokServer 처리
                // --------------------------
                else {
                    server.handleLobbyCommand(this, msg);
                }
            }

        } catch (Exception e) {
            System.out.println("[Player] 연결 끊김: " + nickname);
        }

        finally {
            // ------------------------------------------
            // 연결 종료 시 방에서 제거 + 상태 초기화
            // ------------------------------------------
            try {

                if (room != null) {
                    room.removePlayer(this);
                }

                inGame = false;  // 안전하게 초기화
                socket.close();

            } catch (Exception ignore) {}
        }
    }
}
