package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.BudgetItemSubcategory;
import de.jcss.hbt.domain.householdplan.ExpenseCategory;
import de.jcss.hbt.domain.householdplan.HouseholdPlanId;
import de.jcss.hbt.domain.householdplan.ItemSection;
import de.jcss.hbt.domain.householdplan.ItemType;
import de.jcss.hbt.domain.householdplan.SubcategoryOrder;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code subcategory_order} table, shared by the repository and the overview query
 * handler. A section is stored as {@code INCOME} or as the name of the expense category.
 */
final class ItemSectionColumn {

    private static final String INCOME = "INCOME";

    private ItemSectionColumn() {
    }

    static String of(ItemSection section) {
        return section.type() == ItemType.INCOME ? INCOME : section.category().name();
    }

    static ItemSection toSection(String column) {
        return INCOME.equals(column) ? ItemSection.INCOME : ItemSection.expenses(ExpenseCategory.valueOf(column));
    }

    /** The arranged orders of the plan's sections; sections never arranged are missing. */
    static Map<ItemSection, SubcategoryOrder> subcategoryOrdersOf(JdbcClient jdbcClient, HouseholdPlanId planId) {
        Map<ItemSection, List<BudgetItemSubcategory>> subcategories = new LinkedHashMap<>();
        jdbcClient.sql("""
                        SELECT section, subcategory
                        FROM subcategory_order
                        WHERE household_plan_id = :planId
                        ORDER BY section, position
                        """)
                .param("planId", planId.value())
                .query((rs, rowNum) -> new OrderRow(toSection(rs.getString("section")),
                        BudgetItemSubcategory.of(rs.getString("subcategory"))))
                .list()
                .forEach(row -> subcategories.computeIfAbsent(row.section(), section -> new ArrayList<>())
                        .add(row.subcategory()));
        Map<ItemSection, SubcategoryOrder> orders = new LinkedHashMap<>();
        subcategories.forEach((section, order) -> orders.put(section, new SubcategoryOrder(order)));
        return orders;
    }

    private record OrderRow(ItemSection section, BudgetItemSubcategory subcategory) {
    }
}
