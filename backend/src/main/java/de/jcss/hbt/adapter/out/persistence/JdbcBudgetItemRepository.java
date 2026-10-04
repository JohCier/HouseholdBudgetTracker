package de.jcss.hbt.adapter.out.persistence;

import de.jcss.hbt.application.port.out.BudgetItemRepository;
import de.jcss.hbt.domain.BudgetItem;
import de.jcss.hbt.domain.BudgetItemId;
import de.jcss.hbt.domain.ExpenseCategory;
import de.jcss.hbt.domain.ItemType;
import de.jcss.hbt.domain.Money;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Driven adapter storing {@link BudgetItem} aggregates in the {@code budget_item} table.
 * Items are rebuilt through the domain factories, so stored data passes the same
 * invariants as new input.
 */
@Repository
public class JdbcBudgetItemRepository implements BudgetItemRepository {

    private static final RowMapper<BudgetItem> ROW_MAPPER = (rs, rowNum) -> {
        BudgetItemId id = BudgetItemId.of(rs.getObject("id", UUID.class));
        String description = rs.getString("description");
        Money amount = Money.of(rs.getBigDecimal("amount"));
        LocalDate bookingDate = rs.getObject("booking_date", LocalDate.class);
        return switch (ItemType.valueOf(rs.getString("type"))) {
            case INCOME -> BudgetItem.income(id, description, amount, bookingDate);
            case EXPENSE -> BudgetItem.expense(id, description, amount,
                    ExpenseCategory.valueOf(rs.getString("category")), bookingDate);
        };
    };

    private final JdbcClient jdbcClient;

    public JdbcBudgetItemRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void add(BudgetItem item) {
        jdbcClient.sql("""
                        INSERT INTO budget_item (id, description, amount, type, category, booking_date)
                        VALUES (:id, :description, :amount, :type, :category, :bookingDate)
                        """)
                .param("id", item.id().value())
                .param("description", item.description())
                .param("amount", item.amount().value())
                .param("type", item.type().name())
                .param("category", item.category().map(Enum::name).orElse(null))
                .param("bookingDate", item.bookingDate())
                .update();
    }

    @Override
    public boolean remove(BudgetItemId id) {
        return jdbcClient.sql("DELETE FROM budget_item WHERE id = :id")
                .param("id", id.value())
                .update() > 0;
    }

    @Override
    public List<BudgetItem> findByMonth(YearMonth month) {
        return jdbcClient.sql("""
                        SELECT id, description, amount, type, category, booking_date
                        FROM budget_item
                        WHERE booking_date BETWEEN :from AND :to
                        ORDER BY booking_date DESC, created_at DESC
                        """)
                .param("from", month.atDay(1))
                .param("to", month.atEndOfMonth())
                .query(ROW_MAPPER)
                .list();
    }
}
