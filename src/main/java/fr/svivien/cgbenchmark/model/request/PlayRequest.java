package fr.svivien.cgbenchmark.model.request;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for a PLAY in the CG IDE
 */
public class PlayRequest extends ArrayList<Object> {

    public Data data;

    public PlayRequest(String code, String lang, String ide, String gameOptions, int agentId, boolean reverse) {
        add(ide);
        Multi multi = new Multi();
        multi.gameOptions = gameOptions;
        multi.agentsIds.add(agentId);
        multi.agentsIds.add(reverse ? 1 : 0, -1);
        data = new Data();
        data.programmingLanguageId = lang;
        data.code = code;
        data.multi = multi;
        add(data);
    }

    public class Data {
        public String code;
        public String programmingLanguageId;
        public Multi multi;
    }

    public class Multi {
        public List<Integer> agentsIds = new ArrayList<>();
        String gameOptions;
    }
}
