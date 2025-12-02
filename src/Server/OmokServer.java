package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * OmokServer
 * -------------------------------
 * - 클라이언트를 받아 Player 스레드를 생성
 * - 로비 관련 메시지를 파싱하여 RoomManager에 위임
 * - 실제 방 생성/입장/삭제는 RoomManager가 담당
 * -------------------------------
 */

public class OmokServer {

    private ServerSocket serverSocket;   // 클라이언트 접속을 받는 소켓
    private RoomManager roomManager;     // 방 관리 매니저

    public OmokServer(int port) throws IOException {

        // 서버 소켓 생성 후 포트에 바인딩
        serverSocket = new ServerSocket(port);

        // 방 목록을 관리할 RoomManager 생성
        roomManager = new RoomManager();

        System.out.println("[SERVER] 서버 시작됨: " + port);
    }

    // --------------------------------
    // 서버 루프
    // 클라이언트를 계속 받아 Player 생성
    // --------------------------------
    public void start() {
        try {
            while (true) {

                // 클라이언트 접속 대기 (블로킹)
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] 클라이언트 연결됨");

                // Player 객체 생성 (클라이언트마다 1개)
                Player p = new Player(socket, this);

                // Player는 스레드 단위로 동작
                Thread t = new Thread(p);
                t.start();
            }

        } catch (Exception e) {
            System.out.println("[SERVER] 서버 종료");
        }
    }


    // --------------------------------
    // 로비 메시지 처리 (게임 중이 아닐 때)
    // Player → Server → RoomManager
    // --------------------------------
    public void handleLobbyCommand(Player p, String msg) {

        msg = msg.trim();
        String[] sp = msg.split(" ");

        if (sp.length == 0) return;

        String cmd = sp[0]; // 첫 단어는 명령어

        switch (cmd) {

            // ===========================
            // 닉네임 등록
            // → Player에 저장하고
            // → 방 목록을 전송해 로비 UI 갱신
            // ===========================
        case "NICK": {
            if (sp.length < 2) {
                p.send("ERROR NONICK");
                return;
            }

            p.setNickname(sp[1]);
            System.out.println("[SERVER] 닉네임 설정: " + p.getNickname());

            // 1) 로그인 성공 신호 전송
            p.send("NICKOK");

            // 2) 클라이언트가 showLobby() 실행 후
            //    ClientMain이 ROOMLIST 요청을 보내므로
            //    여기서는 ROOMLIST 안 보냄 (중복 문제 해결)

            return;
        }


            // ===========================
            // 방 목록 요청
            // ===========================
            case "ROOMLIST": {
                p.send(roomManager.getRoomList());
                return;
            }

            // ===========================
            // 방 생성
            // CREATEROOM 방이름
            // ===========================
            case "CREATEROOM": {
                if (sp.length < 2) {
                    p.send("ERROR NONAME");
                    return;
                }

                String roomName = sp[1]; // 공백 없는 단어이므로 OK

                roomManager.createRoom(roomName, p);
                p.send("JOINED " + roomName + " B");
                return;
            }




            // ===========================
            // 방 입장
            // JOINROOM 방이름
            // ===========================
            case "JOINROOM": {
                if (sp.length < 2) {
                    p.send("ERROR NONAME");
                    return;
                }

                String roomName = sp[1]; // 공백 X로 안전

                String result = roomManager.joinRoom(roomName, p);
                p.send(result);
                return;
            }





            // ===========================
            // 알 수 없는 명령어 처리
            // ===========================
            default:
                p.send("ERROR UNKNOWNCMD");
        }
    }


    // 프로그램 시작 지점
    public static void main(String[] args) {

        try {

            // 서버 생성
            OmokServer server = new OmokServer(9999);

            // 접속 대기 시작
            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
