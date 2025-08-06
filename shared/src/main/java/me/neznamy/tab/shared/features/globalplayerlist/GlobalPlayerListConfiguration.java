package me.neznamy.tab.shared.features.globalplayerlist;

import games.synx.connect.api.Connect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.config.file.ConfigurationSection;
import me.neznamy.tab.shared.data.Server;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for storing global playerlist configuration.
 */
@Getter
@RequiredArgsConstructor
public class GlobalPlayerListConfiguration {

    private final boolean othersAsSpectators;
    private final boolean vanishedAsSpectators;
    private final boolean isolateUnlistedServers;
    private final boolean updateLatency;
    @NotNull private final List<Server> spyServers;
    @NotNull private Map<String, List<Server>> sharedServers;

    /**
     * Overrides the existing configuration of server groups, using synx-connect
     * to determine all servers on the network.
     * <p>
     * All servers follow the following pattern: `GROUP/SERVER` e.g `prison/spawn-1`. This
     * uses that format to create groups based on the leading chars before the first /
     */
    public void updateSharedServers() {
        Connect connect = TAB.getInstance().getPlatform().synxConnect();
        if (connect == null) {
            // Connect isn't loaded yet, return
            return;
        }
        Set<String> serverNames = connect.getServerDataService().getAllServers().keySet();
        Map<String, List<Server>> grouped = new HashMap<>();

        // Treating each server ID as GROUP / SERVER
        for (String name : serverNames) {
            // Extracting group assuming it will follow the above naming convention
            int index = name.indexOf('/');
            String group = index == -1 ? name : name.substring(0, index);

            // Full name is expected by proxy
            Server server = Server.byName(name);

            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(server);
        }

        sharedServers = grouped;
    }

    /**
     * Returns instance of this class created from given configuration section. If there are
     * issues in the configuration, console warns are printed.
     *
     * @param   section
     *          Configuration section to load from
     * @return  Loaded instance from given configuration section
     */
    @NotNull
    public static GlobalPlayerListConfiguration fromSection(@NotNull ConfigurationSection section) {
        // Check keys
        section.checkForUnknownKey(Arrays.asList("enabled", "display-others-as-spectators", "display-vanished-players-as-spectators",
                "isolate-unlisted-servers", "update-latency", "spy-servers", "server-groups"));

        ConfigurationSection serverGroupSection = section.getConfigurationSection("server-groups");
        Map<String, List<Server>> sharedServers = new HashMap<>();

        // Shared servers allocation is now handled in getter func via synx-connect
//        Map<String, String> takenServers = new HashMap<>();
//        for (Object serverGroup : serverGroupSection.getKeys()) {
//            String group = serverGroup.toString();
//            List<String> servers = serverGroupSection.getStringList(group, Collections.emptyList());
//            sharedServers.put(group, servers.stream().map(Server::byName).collect(Collectors.toList()));
//            for (String server : servers) {
//                if (takenServers.containsKey(server)) {
//                    section.startupWarn(String.format("Server \"%s\" is defined in global playerlist groups \"%s\" and \"%s\", but it can only be a part of one group.",
//                            server, takenServers.get(server), group));
//                    continue;
//                }
//                takenServers.put(server, group);
//            }
//        }

        return new GlobalPlayerListConfiguration(
                section.getBoolean("display-others-as-spectators", false),
                section.getBoolean("display-vanished-players-as-spectators", true),
                section.getBoolean("isolate-unlisted-servers", false),
                section.getBoolean("update-latency", false),
                section.getStringList("spy-servers", Collections.singletonList("spyserver1")).stream().map(Server::byName).collect(Collectors.toList()),
                sharedServers
        );
    }
}