package Server;

import java.util.ArrayList;
import java.util.List;

/*
 * Room (금수 + 생명 + 찬스 + 기본 오목 + 멀티룸 완전 지원)
 * ------------------------------------------------------------------
 * - 방 생성 후 최대 2명 입장
 * - 첫 번째 플레이어: 흑(B)
 * - 두 번째 플레이어: 백(W)
 * - 금수(6목, 33, 44) 판정 완전 적용
 * - 생명 시스템(잘못된 착수 시 -1)
 * - 찬스(전체 보드 showAll)
 * - RESET, END 처리
 * 
 * ※ 멀티룸 구조(OmokServer/RoomManager/Player)와 100% 호환됨
 * ------------------------------------------------------------------
 */

public class Room {

    private String roomName;
    private RoomManager manager;

    private List<Player> players = new ArrayList<>();

    private final int SIZE = 15;

    // 게임판 / 금수판
    private String[][] board = new String[SIZE][SIZE];
    private boolean[][] isBan = new boolean[SIZE][SIZE];

    // 생명(흑/백)
    private final int MAX_LIFE = 3;
    private int lifeB = MAX_LIFE;
    private int lifeW = MAX_LIFE;

    // 찬스(흑/백)
    private final int MAX_CHANCE = 2;
    private int chanceB = MAX_CHANCE;
    private int chanceW = MAX_CHANCE;

    private char turn = 'B';      // 흑 시작
    private boolean gameOver = false;

    public Room(String roomName, RoomManager manager) {
        this.roomName = roomName;
        this.manager = manager;
    }

    public String getRoomName() { return roomName; }
    public int getPlayerCount() { return players.size(); }
    public List<Player> getPlayers() { return players; }

    public boolean isFull() { return players.size() >= 2; }


    // ---------------------------------------------------------
    //  플레이어 입장
    // ---------------------------------------------------------
    public void addPlayer(Player p) {

        players.add(p);
        p.setRoom(this);

        if (players.size() == 1) {
            p.send("JOINED " + roomName + " B");
        } else if (players.size() == 2) {
            p.send("JOINED " + roomName + " W");
        }

        System.out.println("[Room " + roomName + "] 입장: " + p.getNickname());
    }


    // ---------------------------------------------------------
    //  퇴장 처리
    // ---------------------------------------------------------
    public void removePlayer(Player p) {

        players.remove(p);

        if (players.isEmpty()) {
            manager.removeRoom(roomName);
        }
    }


    // ---------------------------------------------------------
    //  두 명이면 바로 게임 시작
    // ---------------------------------------------------------
    public void startGame() {

        if (players.size() < 2) return;

        Player black = players.get(0);
        Player white = players.get(1);

        black.setInGame(true);
        white.setInGame(true);

        black.send("START B");
        white.send("START W");

        black.send("TURN B");
        white.send("TURN B");

        // 금수 초기화 후 전송
        updateBan();
        sendBanAll();

        System.out.println("[Room " + roomName + "] 게임 시작");
    }


    // ---------------------------------------------------------
    //  클라이언트 게임 메시지를 방에서 처리
    // ---------------------------------------------------------
    public void handleGameMessage(Player p, String msg) {

        if (msg.startsWith("PLACE")) {
            String[] sp = msg.split(" ");
            if (sp.length != 3) return;

            int r = Integer.parseInt(sp[1]);
            int c = Integer.parseInt(sp[2]);

            char color = (players.get(0) == p) ? 'B' : 'W';
            handlePlace(p, color, r, c);
        }

        else if (msg.equals("USECHANCE")) {
            handleChance(p);
        }

        else if (msg.equals("RESET")) {
            reset();
        }

        else if (msg.equals("END")) {
            endGame();
        }
    }


    // ---------------------------------------------------------
    //  돌 두기
    // ---------------------------------------------------------
    private void handlePlace(Player p, char color, int r, int c) {

        if (color != turn) {
            p.send("ERROR NotYourTurn");
            return;
        }

        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return;

        // 금수
        if (color == 'B' && isBan[r][c]) {
            p.send("ERROR FORBIDDEN");
            return;
        }

        // 이미 돌 있음 → 생명 감소
        if (board[r][c] != null) {
            decreaseLife(p);
            return;
        }

        // 정상 착수
        board[r][c] = String.valueOf(color);
        broadcast("MOVE " + r + " " + c + " " + color);

        // 승리 처리
        if (checkWin(r, c, String.valueOf(color))) {
            gameOver = true;
            broadcast("WIN " + color);
            return;
        }

        // 금수 갱신
        updateBan();
        sendBanAll();

        // 턴 변경
        turn = (turn == 'B') ? 'W' : 'B';
        broadcast("TURN " + turn);
    }


    // ---------------------------------------------------------
    //  생명 감소 처리
    // ---------------------------------------------------------
    private void decreaseLife(Player p) {

        char color = (players.get(0) == p) ? 'B' : 'W';

        if (color == 'B') {
            lifeB--;
            broadcast("LIFE B " + lifeB);
            if (lifeB <= 0) {
                broadcast("WIN W");
                gameOver = true;
            }
        } else {
            lifeW--;
            broadcast("LIFE W " + lifeW);
            if (lifeW <= 0) {
                broadcast("WIN B");
                gameOver = true;
            }
        }
    }


    // ---------------------------------------------------------
    //  찬스(전체 보드 보기)
    // ---------------------------------------------------------
    private void handleChance(Player p) {

        char color = (players.get(0) == p) ? 'B' : 'W';

        if (color == 'B') {
            if (chanceB > 0) chanceB--;
            broadcast("CHANCES B " + chanceB);
        } else {
            if (chanceW > 0) chanceW--;
            broadcast("CHANCES W " + chanceW);
        }
    }


    // ---------------------------------------------------------
    //  RESET (초기화)
    // ---------------------------------------------------------
    private void reset() {

        board = new String[SIZE][SIZE];
        isBan = new boolean[SIZE][SIZE];

        lifeB = MAX_LIFE;
        lifeW = MAX_LIFE;
        chanceB = MAX_CHANCE;
        chanceW = MAX_CHANCE;

        turn = 'B';
        gameOver = false;

        broadcast("RESET");
        broadcast("TURN B");
        broadcast("LIFE B " + lifeB);
        broadcast("LIFE W " + lifeW);
        broadcast("CHANCES B " + chanceB);
        broadcast("CHANCES W " + chanceW);

        updateBan();
        sendBanAll();
    }


    // ---------------------------------------------------------
    //  게임 강제 종료
    // ---------------------------------------------------------
    private void endGame() {

        broadcast("END");

        for (Player p : players)
            p.setInGame(false);

        manager.removeRoom(roomName);
    }


    private void broadcast(String msg) {
        for (Player p : players) {
            p.send(msg);
        }
    }



    // ---------------------------------------------------------
    //  금수(33/44/6목) 관련
    // ---------------------------------------------------------
    private void updateBan() {

        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                isBan[i][j] = false;

        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] == null)
                    if (isForbiddenMove(r, c))
                        isBan[r][c] = true;
    }

    private void sendBanAll() {

        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (isBan[r][c])
                    broadcast("BAN " + r + " " + c);
    }


    // ---------------------------------------------------------
    //  금수 판정
    // ---------------------------------------------------------
    private boolean isForbiddenMove(int r, int c) {

        board[r][c] = "B";

        if (isSix(r, c)) {
            board[r][c] = null;
            return true;
        }

        int open3 = 0, open4 = 0;

        int[][] d = {{1,0},{0,1},{1,1},{1,-1}};

        for (int[] dir : d) {

            String line = getLine(r, c, dir[0], dir[1]);

            if (checkOpenThree(line)) open3++;
            if (checkOpenFour(line)) open4++;
        }

        board[r][c] = null;

        if (open3 >= 2) return true; // 33
        if (open4 >= 2) return true; // 44

        return false;
    }


    // 6목 검사
    private boolean isSix(int r, int c) {

        String color = "B";
        int[][] d = {{1,0},{0,1},{1,1},{1,-1}};

        for (int[] dir : d) {
            int cnt = count(r, c, dir[0], dir[1], color)
                    + count(r, c, -dir[0], -dir[1], color) - 1;

            if (cnt > 5) return true;
        }
        return false;
    }

    // 특정 방향 연속 개수
    private int count(int r, int c, int dr, int dc, String color) {
        int cnt = 0;

        while (r >= 0 && r < SIZE &&
               c >= 0 && c < SIZE &&
               color.equals(board[r][c])) {

            cnt++;
            r += dr;
            c += dc;
        }

        return cnt;
    }


    // 11칸 라인 생성
    private String getLine(int r, int c, int dr, int dc) {

        StringBuilder sb = new StringBuilder();

        for (int k = -5; k <= 5; k++) {

            int nr = r + dr * k;
            int nc = c + dc * k;

            if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE)
                sb.append("X");
            else if (board[nr][nc] == null)
                sb.append(".");
            else
                sb.append(board[nr][nc]);
        }

        return sb.toString();
    }

    // 오픈쓰리
    private boolean checkOpenThree(String line) {
        String[] patterns = {
                ".BBB.",
                ".BB.B.",
                ".B.BB.",
                ".B.B.B.",
                "..BB.B..",
                "..B.BB.."
        };

        for (String p : patterns)
            if (line.contains(p))
                return true;

        return false;
    }

    // 오픈포
    private boolean checkOpenFour(String line) {
        String[] patterns = {
                ".BBBB.",
                ".BBB.B.",
                ".BB.BB.",
                ".B.BBB."
        };

        for (String p : patterns)
            if (line.contains(p))
                return true;

        return false;
    }


    // 5목 승리 판정
    private boolean checkWin(int r, int c, String color) {

        return (
                count(r, c, 1, 0, color) + count(r, c,-1, 0,color) -1 >= 5 ||
                count(r, c, 0, 1, color) + count(r, c, 0,-1,color) -1 >= 5 ||
                count(r, c, 1, 1, color) + count(r, c,-1,-1,color) -1 >= 5 ||
                count(r, c, 1,-1, color) + count(r, c,-1, 1,color) -1 >= 5
        );
    }
}
