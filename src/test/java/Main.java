import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.dto.GameResult;

public class Main {
    public static void main(String[] args) {

        String league = "2";

        runBatch(league, 1000);
        //runOnce(league, -4505592547209682872L);
    }

    private static void runBatch(String league, int numRounds) {
        int wins0 = 0;
        int wins1 = 0;
        int draws = 0;
        for (int i = 0; i < numRounds; i++) {
            MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

            gameRunner.addAgent(PlayerAStar.class);
            //gameRunner.addAgent(EmptyAI.class);
            gameRunner.addAgent("python.exe config/level1/Boss.py3");
            //gameRunner.addAgent("python.exe config/level2/Boss.py3");

            System.setProperty("league.level", league);

            GameResult result = gameRunner.simulate();
            System.out.println(String.format("Match %d (%d turns): %d-%d, %s", i, result.views.size(), result.scores.get(0),
                    result.scores.get(1), result.uinput.get(0).replace("\n", "")));

            if (result.scores.get(0) > result.scores.get(1)) {
                wins0++;
            } else if (result.scores.get(0) < result.scores.get(1)) {
                wins1++;
            } else {
                draws++;
            }

            if (result.scores.get(0) < 0) {
                int numSummaries = result.summaries.size();
                System.out.println(String.format("summary %d: %s\nsummary %d: %s", numSummaries - 2, result.summaries.get(numSummaries - 2), numSummaries - 1, result.summaries.get(numSummaries - 1)));
                throw new RuntimeException("something fishy with score 0");
            }
            if (result.scores.get(1) < 0) {
                int numSummaries = result.summaries.size();
                System.out.println(String.format("summary %d: %s\nsummary %d: %s", numSummaries - 2, result.summaries.get(numSummaries - 2), numSummaries - 1, result.summaries.get(numSummaries - 1)));
                throw new RuntimeException("something fishy with score 1");
            }
        }
        System.out.println(String.format("User 0 won %d times, user 1 won %d times, %d draws", wins0, wins1, draws));
    }

    private static void runOnce(String league, long seed) {
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        gameRunner.addAgent(PlayerAStar.class);
        //gameRunner.addAgent(EmptyAI.class);
        gameRunner.addAgent("python.exe config/level1/Boss.py3");
        //gameRunner.addAgent("python.exe config/level2/Boss.py3");

        System.setProperty("league.level", league);
        gameRunner.setSeed(seed);

        gameRunner.start();
    }
}
