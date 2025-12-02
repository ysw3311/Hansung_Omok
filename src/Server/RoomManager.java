package Server;

import java.util.HashMap;
import java.util.Map;

/*
 * RoomManager
 * -----------------------------------------------------
 * - 서버의 "전체 방 목록"을 관리하는 매니저 클래스
 * - 방 생성, 입장, 삭제, 목록 전달 기능을 모두 담당
 *
 * - Player는 로비 상태에서 OmokServer가 이 RoomManager에 명령을 전달한다.
 * -----------------------------------------------------
 */

public class RoomManager {

    // 전체 방 목록 (방 이름 → Room 객체)
    private Map<String, Room> rooms = new HashMap<>();


    // -----------------------------------------------------
    // 방 목록 요청
    // 클라이언트는 "ROOMLIST ..." 형태의 문자열을 받게 된다.
    // -----------------------------------------------------
    public synchronized String getRoomList() {

        if (rooms.isEmpty())
            return "ROOMLIST EMPTY";

        // ROOMLIST 방이름(현재인원/2) 방이름(현재인원/2)...
        StringBuilder sb = new StringBuilder("ROOMLIST");

        for (String roomName : rooms.keySet()) {
            Room r = rooms.get(roomName);
            int cnt = r.getPlayerCount(); // 현재 인원 수
            sb.append(" ").append(roomName).append("(").append(cnt).append("/2)");
        }

        return sb.toString();
    }


    // -----------------------------------------------------
    // 방 생성
    // CREATE_ROOM 명령을 받은 Player가 host 역할이 된다.
    // -----------------------------------------------------
    public synchronized String createRoom(String roomName, Player host) {

        // 이미 존재하는 방 이름인지 체크
        if (rooms.containsKey(roomName))
            return "ERROR ROOMEXIST";

        // 새 방 생성 후 목록에 등록
        Room room = new Room(roomName, this);
        rooms.put(roomName, room);

        // 방에 host 추가 (첫번째 플레이어 = 흑)
        room.addPlayer(host);

        return "ROOMCREATED " + roomName;
    }


    // -----------------------------------------------------
    // 방 입장
    // 방이 존재하는지, 꽉 차지 않았는지 검증 후 입장
    // -----------------------------------------------------
    public synchronized String joinRoom(String roomName, Player p) {

        if (!rooms.containsKey(roomName))
            return "ERROR NOROOM";

        Room room = rooms.get(roomName);

        // 최대 2명까지 플레이 가능
        if (room.getPlayerCount() >= 2)
            return "ERROR FULL";

        room.addPlayer(p); // 두 번째 플레이어 입장 (백)

        // 입장 후 인원이 2명이 되면 자동으로 게임 시작
        if (room.getPlayerCount() == 2) {
            room.startGame();
        }

        return "JOINED " + roomName;
    }


    // -----------------------------------------------------
    // 방 삭제
    // 방 내부에서 removePlayer() → 인원 0 → RoomManager.removeRoom 호출됨
    // -----------------------------------------------------
    public synchronized void removeRoom(String roomName) {
        rooms.remove(roomName);
        System.out.println("[RoomManager] Removed " + roomName);
    }
}
