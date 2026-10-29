package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.Money;
import de.jcss.hbt.domain.householdplan.OwnerId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItem;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.OneTimeItemRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link OneTimeItem} aggregates in the {@code one_time_item} table. Items are rebuilt
 * through {@link OneTimeItem#reconstitute}, so stored data passes the same invariants as new
 * input. Expects to run within the transaction of the calling application service; concurrent
 * changes to the same item are serialized by locking its row in {@link #findById}.
 */
@Repository
public class JdbcOneTimeItemRepository implements OneTimeItemRepository {

    private final JdbcClient jdbcClient;

    public JdbcOneTimeItemRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<OneTimeItem> findById(OneTimeItemId id) {
        return jdbcClient.sql("""
                        SELECT owner_id, type, description, amount, item_date, note
                        FROM one_time_item
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", id.value())
                .query((rs, rowNum) -> OneTimeItem.reconstitute(id,
                        OwnerId.of(rs.getObject("owner_id", UUID.class)),
                        ItemType.valueOf(rs.getString("type")),
                        BudgetItemDescription.of(rs.getString("description")),
                        Money.of(rs.getBigDecimal("amount")),
                        rs.getObject("item_date", LocalDate.class),
                        BudgetItemNote.ofNullable(rs.getString("note"))))
                .optional();
    }

    @Override
    public void add(OneTimeItem item) {
        jdbcClient.sql("""
                        INSERT INTO one_time_item (id, owner_id, type, description, amount, item_date, note)
                        VALUES (:id, :owner, :type, :description, :amount, :date, :note)
                        """)
                .param("id", item.id().value())
                .param("owner", item.owner().value())
                .param("type", item.type().name())
                .param("description", item.description().value())
                .param("amount", item.amount().value())
                .param("date", item.date())
                .param("note", item.note() == null ? null : item.note().value())
                .update();
    }

    @Override
    public void save(OneTimeItem item) {
        jdbcClient.sql("""
                        UPDATE one_time_item
                        SET type = :type, description = :description, amount = :amount, item_date = :date, note = :note
                        WHERE id = :id
                        """)
                .param("id", item.id().value())
                .param("type", item.type().name())
                .param("description", item.description().value())
                .param("amount", item.amount().value())
                .param("date", item.date())
                .param("note", item.note() == null ? null : item.note().value())
                .update();
    }

    @Override
    public void remove(OneTimeItem item) {
        jdbcClient.sql("DELETE FROM one_time_item WHERE id = :id")
                .param("id", item.id().value())
                .update();
    }
}
