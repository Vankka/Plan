package main.java.com.djrapitops.plan.database.tables;

import com.djrapitops.plugin.utilities.Verify;
import main.java.com.djrapitops.plan.api.exceptions.DBCreateTableException;
import main.java.com.djrapitops.plan.data.UserInfo;
import main.java.com.djrapitops.plan.database.databases.SQLDB;
import main.java.com.djrapitops.plan.database.sql.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Rsl1122
 */
public class UsersTable extends UserIDTable {

    public final String statementSelectID;
    private final String columnID = "id";
    private final String columnUUID = "uuid";
    private final String columnRegistered = "registered";
    private final String columnName = "name";
    private final String columnTimesKicked = "times_kicked";
    private String insertStatement;

    public UsersTable(SQLDB db, boolean usingMySQL) {
        super("plan_users", db, usingMySQL);
        statementSelectID = "(" + Select.from(tableName, tableName + "." + columnID).where(columnUUID + "=?").toString() + ")";
        insertStatement = Insert.values(tableName,
                columnUUID,
                columnRegistered,
                columnName);
    }

    @Override
    public void createTable() throws DBCreateTableException {
        createTable(TableSqlParser.createTable(tableName)
                .primaryKeyIDColumn(usingMySQL, columnID)
                .column(columnUUID, Sql.varchar(36)).notNull().unique()
                .column(columnRegistered, Sql.LONG).notNull()
                .column(columnName, Sql.varchar(16)).notNull()
                .column(columnTimesKicked, Sql.INT).notNull().defaultValue("0")
                .primaryKey(usingMySQL, columnID)
                .toString()
        );
    }

    /**
     * @return a {@link Set} of the saved UUIDs.
     * @throws SQLException when an error at retrieving the UUIDs happens
     */
    public Set<UUID> getSavedUUIDs() throws SQLException {
        Set<UUID> uuids = new HashSet<>();

        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnUUID).toString());
            statement.setFetchSize(5000);

            set = statement.executeQuery();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString(columnUUID));
                uuids.add(uuid);
            }
            return uuids;
        } finally {
            close(set, statement);
        }
    }

    /**
     * Remove a user from Users Table.
     *
     * @param uuid the UUID of the user that should be removed.
     * @throws SQLException DB Error
     */
    @Override
    public void removeUser(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement("DELETE FROM " + tableName + " WHERE (" + columnUUID + "=?)");
            statement.setString(1, uuid.toString());

            statement.execute();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    /**
     * @return the name of the column that inherits the ID.
     */
    public String getColumnID() {
        return columnID;
    }

    /**
     * @return the name of the column that inherits the UUID.
     */
    public String getColumnUUID() {
        return columnUUID;
    }

    /**
     * Get UUID of a player.
     *
     * @param playerName Name of a player
     * @return UUID of the player
     * @throws SQLException DB Error
     */
    public UUID getUuidOf(String playerName) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnUUID)
                    .where("UPPER(" + columnName + ")=UPPER(?)")
                    .toString());
            statement.setString(1, playerName);
            set = statement.executeQuery();
            if (set.next()) {
                String uuidS = set.getString(columnUUID);
                return UUID.fromString(uuidS);
            }
            return null;
        } finally {
            close(set, statement);
        }
    }

    public List<Long> getRegisterDates() throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnRegistered).toString());
            set = statement.executeQuery();
            List<Long> registerDates = new ArrayList<>();
            while (set.next()) {
                registerDates.add(set.getLong(columnRegistered));
            }
            return registerDates;
        } finally {
            close(set, statement);
        }
    }

    /**
     * Register a new user (UUID) to the database.
     *
     * @param uuid       UUID of the player.
     * @param registered Register date.
     * @param name       Name of the player.
     * @throws SQLException             DB Error
     * @throws IllegalArgumentException If uuid or name are null.
     */
    public void registerUser(UUID uuid, long registered, String name) throws SQLException {
        Verify.nullCheck(uuid, name);

        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(insertStatement);
            statement.setString(1, uuid.toString());
            statement.setLong(2, registered);
            statement.setString(3, name);

            statement.execute();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    public boolean isRegistered(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, "COUNT(" + columnID + ") as c")
                    .where(columnUUID + "=?")
                    .toString());
            statement.setString(1, uuid.toString());
            set = statement.executeQuery();
            return set.next() && set.getInt("c") >= 1;
        } finally {
            close(set, statement);
        }
    }

    public void updateName(UUID uuid, String name) throws SQLException {
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Update.values(tableName, columnName)
                    .where(columnUUID + "=?")
                    .toString());
            statement.setString(1, name);
            statement.setString(2, uuid.toString());

            statement.execute();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    public int getTimesKicked(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnTimesKicked)
                    .where(columnUUID + "=?")
                    .toString());
            statement.setString(1, uuid.toString());
            set = statement.executeQuery();
            if (set.next()) {
                return set.getInt(columnTimesKicked);
            }
            return 0;
        } finally {
            close(set, statement);
        }
    }

    public void kicked(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement("UPDATE " + tableName + " SET "
                    + columnTimesKicked + "=" + columnTimesKicked + "+ 1" +
                    " WHERE " + columnUUID + "=?");
            statement.setString(1, uuid.toString());

            statement.execute();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    public String getPlayerName(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnName)
                    .where(columnUUID + "=?")
                    .toString());
            statement.setString(1, uuid.toString());
            set = statement.executeQuery();
            if (set.next()) {
                return set.getString(columnName);
            }
            return null;
        } finally {
            close(set, statement);
        }
    }

    /**
     * Gets the names of the players which names or nicknames match {@code name}.
     *
     * @param name the name / nickname.
     * @return a list of distinct names.
     * @throws SQLException when an error at fetching the names happens.
     */
    public List<String> getMatchingNames(String name) throws SQLException {
        String searchString = "%" + name + "%";
        List<String> matchingNames = new ArrayList<>();

        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            NicknamesTable nicknamesTable = db.getNicknamesTable();
            statement = connection.prepareStatement(
                    "SELECT " + columnName + " FROM " + tableName +
                            " WHERE " + columnName + " LIKE LOWER(?)" +
                            " UNION SELECT " + columnName + " FROM " + tableName +
                            " WHERE " + columnID + " =" +
                            " (SELECT " + columnID + " FROM " + nicknamesTable +
                            " WHERE " + nicknamesTable.getColumnNick() + " LIKE LOWER(?))"
            );
            statement.setFetchSize(5000);
            statement.setString(1, searchString);
            statement.setString(2, searchString);

            set = statement.executeQuery();
            while (set.next()) {
                matchingNames.add(set.getString("name"));
            }
            return matchingNames;
        } finally {
            close(set, statement);
        }
    }

    public String getColumnName() {
        return columnName;
    }

    /**
     * Inserts UUIDs, Register dates and Names to the table.
     * <p>
     * This method is for batch operations, and should not be used to add information of users.
     * Use UserInfoTable instead.
     *
     * @param users Users to insert
     * @throws SQLException DB Error
     */
    public void insertUsers(Map<UUID, UserInfo> users) throws SQLException {
        if (Verify.isEmpty(users)) {
            return;
        }
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(insertStatement);
            for (Map.Entry<UUID, UserInfo> entry : users.entrySet()) {
                UUID uuid = entry.getKey();
                UserInfo info = entry.getValue();
                long registered = info.getRegistered();
                String name = info.getName();

                statement.setString(1, uuid.toString());
                statement.setLong(2, registered);
                statement.setString(3, name);
                statement.addBatch();
            }

            statement.executeBatch();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    public Map<UUID, UserInfo> getUsers() throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.all(tableName).toString());
            statement.setFetchSize(20000);
            set = statement.executeQuery();
            Map<UUID, UserInfo> users = new HashMap<>();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString(columnUUID));
                String name = set.getString(columnName);
                long registered = set.getLong(columnRegistered);

                users.put(uuid, new UserInfo(uuid, name, registered, false, false));
            }
            return users;
        } finally {
            close(set, statement);
        }
    }

    public void updateKicked(Map<UUID, Integer> timesKicked) throws SQLException {
        if (Verify.isEmpty(timesKicked)) {
            return;
        }
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement("UPDATE " + tableName + " SET "
                    + columnTimesKicked + "=?" +
                    " WHERE " + columnUUID + "=?");
            for (Map.Entry<UUID, Integer> entry : timesKicked.entrySet()) {
                UUID uuid = entry.getKey();
                int kickCount = entry.getValue();
                statement.setInt(1, kickCount);
                statement.setString(2, uuid.toString());
                statement.addBatch();
            }

            statement.executeBatch();
            commit(connection);
        } finally {
            close(statement);
        }
    }

    public Map<UUID, Integer> getAllTimesKicked() throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnUUID, columnTimesKicked)
                    .toString());
            statement.setFetchSize(20000);
            set = statement.executeQuery();
            Map<UUID, Integer> timesKicked = new HashMap<>();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString(columnUUID));
                int kickCount = set.getInt(columnTimesKicked);

                timesKicked.put(uuid, kickCount);
            }
            return timesKicked;
        } finally {
            close(set, statement);
        }
    }

    public Map<UUID, String> getPlayerNames() throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnUUID, columnName).toString());
            statement.setFetchSize(20000);
            set = statement.executeQuery();
            Map<UUID, String> names = new HashMap<>();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString(columnUUID));
                String name = set.getString(columnName);

                names.put(uuid, name);
            }
            return names;
        } finally {
            close(set, statement);
        }
    }

    public int getPlayerCount() throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement("SELECT COUNT(*) AS player_count FROM " + tableName);
            statement.setFetchSize(5000);

            set = statement.executeQuery();
            if (set.next()) {
                return set.getInt("player_count");
            }
            return 0;
        } finally {
            close(set, statement);
        }
    }

    public Optional<Long> getRegisterDate(UUID uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet set = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(Select.from(tableName, columnRegistered).where(columnUUID + "=?").toString());
            statement.setString(1, uuid.toString());
            set = statement.executeQuery();
            if (set.next()) {
                Optional.of(set.getLong(columnRegistered));
            }
            return Optional.empty();
        } finally {
            close(set, statement);
        }
    }
}
