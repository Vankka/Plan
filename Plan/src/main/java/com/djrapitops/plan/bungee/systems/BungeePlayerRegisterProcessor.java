/* 
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package main.java.com.djrapitops.plan.bungee.systems;

import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.bungee.PlanBungee;
import main.java.com.djrapitops.plan.database.tables.UsersTable;
import main.java.com.djrapitops.plan.systems.processing.player.PlayerProcessor;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Processor that registers a new User for all servers to use as UUID - ID reference.
 *
 * @author Rsl1122
 */
public class BungeePlayerRegisterProcessor extends PlayerProcessor {

    private final String name;
    private final long registered;

    public BungeePlayerRegisterProcessor(UUID uuid, String name, long registered) {
        super(uuid);
        this.name = name;
        this.registered = registered;
    }

    @Override
    public void process() {
        UUID uuid = getUUID();
        UsersTable usersTable = PlanBungee.getInstance().getDB().getUsersTable();
        try {
            if (usersTable.isRegistered(uuid)) {
                return;
            }
            usersTable.registerUser(uuid, registered, name);
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
        }
    }
}