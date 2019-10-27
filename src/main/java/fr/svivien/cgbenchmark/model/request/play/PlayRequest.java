package fr.svivien.cgbenchmark.model.request.play;

import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Request body for a PLAY in the CG IDE
 */
public class PlayRequest extends ArrayList<Object> {

    private Data data;

    public PlayRequest(String code, String lang, String ide, String gameOptions, List<EnemyConfiguration> enemies) {
        add(ide);
        Multi multi = new Multi();
        multi.gameOptions = gameOptions;
        multi.agentsIds.addAll(enemies.stream().map(EnemyConfiguration::getAgentId).collect(Collectors.toList()));
        data = new Data();
        data.programmingLanguageId = lang;
        data.code = code;
        data.multi = multi;
        add(data);
    }

    public class Data {
        String code;
        String programmingLanguageId;
        Multi multi;
    }

    public class Multi {
        List<Integer> agentsIds = new ArrayList<>();
        String gameOptions;
    }
}
