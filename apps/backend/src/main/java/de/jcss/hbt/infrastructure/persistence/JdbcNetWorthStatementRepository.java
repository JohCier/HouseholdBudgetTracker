package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.exception.NetWorthStatementAlreadyExistsException;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.networth.NetWorthEntry;
import de.jcss.hbt.domain.networth.NetWorthEntryId;
import de.jcss.hbt.domain.networth.NetWorthEntryName;
import de.jcss.hbt.domain.networth.NetWorthEntryType;
import de.jcss.hbt.domain.networth.NetWorthStatement;
import de.jcss.hbt.domain.networth.NetWorthStatementId;
import de.jcss.hbt.domain.networth.NetWorthStatementRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link NetWorthStatement} aggregates in the {@code net_worth_statement} and
 * {@code net_worth_entry} tables. Entries are rebuilt through the domain, so stored data passes the
 * same invariants as new input. Expects to run within the transaction of the calling application
 * service.
 * <p>
 * The order of the entries is stored in {@code net_worth_entry.position}, the first recorded lowest.
 * A statement has only a handful of entries, so saving simply writes all of them anew.
 * <p>
 * Concurrent changes are prevented by locking the statement row in {@link #findFor}
 * ({@code SELECT ... FOR UPDATE}) until the transaction ends.
 */
@Repository
public class JdbcNetWorthStatementRepository implements NetWorthStatementRepository {

    private final JdbcClient jdbcClient;

    public JdbcNetWorthStatementRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<NetWorthStatement> findFor(OwnerId owner, Year year) {
        return jdbcClient.sql("SELECT id FROM net_worth_statement WHERE owner_id = :owner AND year = :year FOR UPDATE")
                .param("owner", owner.value())
                .param("year", year.getValue())
                .query((rs, rowNum) -> NetWorthStatementId.of(rs.getObject("id", UUID.class)))
                .optional()
                .map(id -> NetWorthStatement.reconstitute(id, owner, year, findEntries(id)));
    }

    private List<NetWorthEntry> findEntries(NetWorthStatementId id) {
        return jdbcClient.sql("""
                        SELECT id, type, name, amount
                        FROM net_worth_entry
                        WHERE net_worth_statement_id = :id
                        ORDER BY position
                        """)
                .param("id", id.value())
                .query((rs, rowNum) -> NetWorthEntry.reconstitute(
                        NetWorthEntryId.of(rs.getObject("id", UUID.class)),
                        NetWorthEntryType.valueOf(rs.getString("type")),
                        NetWorthEntryName.of(rs.getString("name")),
                        Money.of(rs.getBigDecimal("amount"))))
                .list();
    }

    @Override
    public void add(NetWorthStatement statement) {
        try {
            jdbcClient.sql("INSERT INTO net_worth_statement (id, owner_id, year) VALUES (:id, :owner, :year)")
                    .param("id", statement.id().value())
                    .param("owner", statement.owner().value())
                    .param("year", statement.year().getValue())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new NetWorthStatementAlreadyExistsException(statement.year());
        }
        insertEntries(statement);
    }

    @Override
    public void save(NetWorthStatement statement) {
        deleteEntries(statement);
        insertEntries(statement);
    }

    /** The entries go with the statement through {@code ON DELETE CASCADE}. */
    @Override
    public void remove(NetWorthStatement statement) {
        jdbcClient.sql("DELETE FROM net_worth_statement WHERE id = :id")
                .param("id", statement.id().value())
                .update();
    }

    private void deleteEntries(NetWorthStatement statement) {
        jdbcClient.sql("DELETE FROM net_worth_entry WHERE net_worth_statement_id = :id")
                .param("id", statement.id().value())
                .update();
    }

    private void insertEntries(NetWorthStatement statement) {
        List<NetWorthEntry> entries = statement.entries();
        for (int position = 0; position < entries.size(); position++) {
            NetWorthEntry entry = entries.get(position);
            jdbcClient.sql("""
                            INSERT INTO net_worth_entry (id, net_worth_statement_id, type, name, amount, position)
                            VALUES (:id, :statementId, :type, :name, :amount, :position)
                            """)
                    .param("id", entry.id().value())
                    .param("statementId", statement.id().value())
                    .param("type", entry.type().name())
                    .param("name", entry.name().value())
                    .param("amount", entry.amount().value())
                    .param("position", position)
                    .update();
        }
    }
}
