import java.io.*;
import java.net.*;

public class OmokServer {

    private static final int PORT = 9999;
    private static final int SIZE = 15;
    private static final int MAX_LIFE = 3;
    private static final int MAX_CHANCE = 2;

    private String[][] board = new String[SIZE][SIZE];
    private boolean[][] isBan = new boolean[SIZE][SIZE];

    private int lifeBlack = MAX_LIFE;
    private int lifeWhite = MAX_LIFE;

    private int chanceBlack = MAX_CHANCE;
    private int chanceWhite = MAX_CHANCE;

    private char turn = 'B';
    private boolean gameOver = false;

    private Player blackPlayer;
    private Player whitePlayer;

    public static void main(String[] args) {
        new OmokServer().start();
    }

    public void start() {
        System.out.println("Omok Server Running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // 1) 흑 플레이어
            System.out.println("흑 플레이어 접속 대기...");
            Socket a = serverSocket.accept();
            blackPlayer = new Player(a, 'B');
            System.out.println("흑 접속 완료.");

            // 2) 백 플레이어
            System.out.println("백 플레이어 접속 대기...");
            Socket b = serverSocket.accept();
            whitePlayer = new Player(b, 'W');
            System.out.println("백 접속 완료.");

            // START
            blackPlayer.send("START B");
            whitePlayer.send("START W");

            // 턴
            blackPlayer.send("TURN B");
            whitePlayer.send("TURN B");

            // 초기 금수 좌표 계산하여 전송
            updateBanPositions();
            sendBanPositions();

            new Thread(blackPlayer).start();
            new Thread(whitePlayer).start();

            System.out.println("게임 시작!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Player
    // -----------------------------------------------------------------------
    private class Player implements Runnable {

        Socket socket;
        BufferedReader in;
        PrintWriter out;
        char color; // 'B' or 'W'

        public Player(Socket socket, char color) {
            this.socket = socket;
            this.color = color;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"), true);
            } catch (Exception e) { e.printStackTrace(); }
        }

        public void send(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                String line;
                while (!gameOver && (line = in.readLine()) != null) {
                    handleMessage(this, line.trim());
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료");
            }
        }
    }

    // -----------------------------------------------------------------------
    // 메시지 처리
    // -----------------------------------------------------------------------
    private synchronized void handleMessage(Player p, String msg) {

        if (msg.startsWith("PLACE")) {

            String[] sp = msg.split(" ");
            if (sp.length != 3) {
                p.send("ERROR BadFormat");
                return;
            }
            int r = Integer.parseInt(sp[1]);
            int c = Integer.parseInt(sp[2]);

            handlePlace(p, r, c);
        }

        else if (msg.equals("USECHANCE")) {
            handleUseChance(p);
        }

        else if (msg.equals("RESET")) {
            resetGame();
        }

        else {
            p.send("ERROR UnknownCommand");
        }
    }

    // -----------------------------------------------------------------------
    // 힌트 감소
    // -----------------------------------------------------------------------
    private void handleUseChance(Player p) {

        if (p.color == 'B') {
            if (chanceBlack > 0)
                chanceBlack--;
            broadcast("CHANCES B " + chanceBlack);
        } else {
            if (chanceWhite > 0)
                chanceWhite--;
            broadcast("CHANCES W " + chanceWhite);
        }
    }

    // -----------------------------------------------------------------------
    // 착수 처리 + 금수 검사
    // -----------------------------------------------------------------------
    private void handlePlace(Player p, int r, int c) {

        // 턴 체크
        if (p.color != turn) {
            p.send("ERROR NotYourTurn");
            return;
        }

        // 범위 체크
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            p.send("ERROR OutOfBoard");
            return;
        }

        // 금수 체크(흑만)
        if (p.color == 'B' && isBan[r][c]) {
            p.send("ERROR FORBIDDEN");
            return;
        }

        // 이미 돌 있으면 생명 감소
        if (board[r][c] != null) {
            decreaseLife(p);
            return;
        }

        // 정상 착수
        String color = String.valueOf(p.color);
        board[r][c] = color;

        // 전송
        broadcast("MOVE " + r + " " + c + " " + color);

        // 승리?
        if (checkWin(r, c, color)) {
            gameOver = true;
            broadcast("WIN " + color);
            broadcast("LOSE " + (color.equals("B") ? "W" : "B"));
            return;
        }

        // 금수 업데이트 & 전송
        updateBanPositions();
        sendBanPositions();

        // 턴 변경
        turn = (turn == 'B') ? 'W' : 'B';
        broadcast("TURN " + turn);
    }

    // -----------------------------------------------------------------------
    // 금수 전송 (BAN r c)
    // -----------------------------------------------------------------------
    private void sendBanPositions() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isBan[r][c]) {
                    broadcast("BAN " + r + " " + c);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // 생명 감소
    // -----------------------------------------------------------------------
    private void decreaseLife(Player p) {

        if (p.color == 'B') {
            lifeBlack--;
            broadcast("LIFE B " + lifeBlack);
            if (lifeBlack <= 0) endGame('W');
        } else {
            lifeWhite--;
            broadcast("LIFE W " + lifeWhite);
            if (lifeWhite <= 0) endGame('B');
        }
    }

    private void endGame(char winner) {
        gameOver = true;
        broadcast("WIN " + winner);
        broadcast("LOSE " + (winner == 'B' ? "W" : "B"));
    }

    // -----------------------------------------------------------------------
    // Broadcast
    // -----------------------------------------------------------------------
    private void broadcast(String m) {
        if (blackPlayer != null) blackPlayer.send(m);
        if (whitePlayer != null) whitePlayer.send(m);
        System.out.println("[SERVER] " + m);
    }
    
    private synchronized void resetGame() {

        System.out.println("[SERVER] Resetting game...");

        //  보드 초기화
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                board[r][c] = null;

        //  금수 초기화
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                isBan[r][c] = false;

        //  생명 초기화
        lifeBlack = MAX_LIFE;
        lifeWhite = MAX_LIFE;

        //  chance 초기화
        chanceBlack = MAX_CHANCE;
        chanceWhite = MAX_CHANCE;

        //  턴 초기화 (흑 먼저)
        turn = 'B';

        //  게임 오버 상태 초기화
        gameOver = false;

        //  금수 다시 계산
        updateBanPositions();


        // 기본 상태 재전송
        broadcast("TURN B");
        broadcast("LIFE B " + lifeBlack);
        broadcast("LIFE W " + lifeWhite);
        broadcast("CHANCES B " + chanceBlack);
        broadcast("CHANCES W " + chanceWhite);

        //  금수 재전송
        sendBanPositions();
        
        // 클라이언트에게 RESET 알림
        broadcast("RESET");
    }
    

    // -----------------------------------------------------------------------
    // 승리 판정
    // -----------------------------------------------------------------------
    private boolean checkWin(int r, int c, String color) {
        return (
                count(r, c, 1, 0, color) + count(r, c, -1, 0, color) - 1 >= 5 ||
                count(r, c, 0, 1, color) + count(r, c, 0, -1, color) - 1 >= 5 ||
                count(r, c, 1, 1, color) + count(r, c, -1, -1, color) - 1 >= 5 ||
                count(r, c, 1, -1, color) + count(r, c, -1, 1, color) - 1 >= 5
        );
    }

    private int count(int r, int c, int dr, int dc, String color) {
        int cnt = 0;
        while (r >= 0 && r < SIZE && c >= 0 && c < SIZE &&
               color.equals(board[r][c])) {
            cnt++;
            r += dr;
            c += dc;
        }
        return cnt;
    }

    // -----------------------------------------------------------------------
    // ★ 금수 전체 계산
    // -----------------------------------------------------------------------
    private void updateBanPositions() {

        // 초기화
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                isBan[i][j] = false;

        // 모든 칸 검사
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                if (board[r][c] != null) continue;

                if (isForbiddenMove(r, c))
                    isBan[r][c] = true;
            }
        }
    }

    // -----------------------------------------------------------------------
    // 금수 판정: 6목 + 33 + 44
    // -----------------------------------------------------------------------
    private boolean isForbiddenMove(int r, int c) {

        // 흑만 금수 적용
        board[r][c] = "B";

        // 6목
        if (isSixMove(r, c)) {
            board[r][c] = null;
            return true;
        }

        int openThree = 0;
        int openFour = 0;

        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};

        for (int[] d : dirs) {

            String line = getLineString(r, c, d[0], d[1]);

            if (checkOpenThree(line)) openThree++;
            if (checkOpenFour(line)) openFour++;
        }

        board[r][c] = null;

        if (openThree >= 2) return true; // 33
        if (openFour >= 2) return true;  // 44

        return false;
    }

    // -----------------------------------------------------------------------
    // getLineString (전 방향 11칸)
    // -----------------------------------------------------------------------
    private String getLineString(int r, int c, int dr, int dc) {

        StringBuilder sb = new StringBuilder();

        for (int i = -5; i <= 5; i++) {
            int nr = r + dr * i;
            int nc = c + dc * i;

            if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE)
                sb.append("X");
            else if (board[nr][nc] == null)
                sb.append(".");
            else
                sb.append(board[nr][nc]); // B or W
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // open three patterns
    // -----------------------------------------------------------------------
    private boolean checkOpenThree(String line) {
        String[] p = {
                ".BBB.",
                ".BB.B.",
                ".B.BB.",
                ".B.B.B.",
                "..BB.B..",
                "..B.BB.."
        };

        for (String s : p)
            if (line.contains(s))
                return true;

        return false;
    }

    // -----------------------------------------------------------------------
    // open four patterns
    // -----------------------------------------------------------------------
    private boolean checkOpenFour(String line) {
        String[] p = {
                ".BBBB.",
                ".BBB.B.",
                ".BB.BB.",
                ".B.BBB."
        };

        for (String s : p)
            if (line.contains(s))
                return true;

        return false;
    }

    // -----------------------------------------------------------------------
    // 6목 검사
    // -----------------------------------------------------------------------
    private boolean isSixMove(int r, int c) {

        String color = "B";
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};

        for (int[] d : dirs) {

            int cnt =
                count(r, c, d[0], d[1], color) +
                count(r, c, -d[0], -d[1], color) - 1;

            if (cnt > 5)
                return true;
        }
        return false;
    }
}
