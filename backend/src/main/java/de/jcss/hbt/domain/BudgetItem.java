package de.jcss.hbt.domain;

import de.jcss.hbt.domain.exception.InvalidBudgetItemException;

import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root representing a single income or expense booking. Items do not share
 * any invariant with each other, so each one is its own consistency boundary.
 * Only ever created through the {@link #income} / {@link #expense} factory methods,
 * which enforce the domain rules:
 * <ul>
 *     <li>description must not be blank and at most {@value #MAX_DESCRIPTION_LENGTH} characters long</li>
 *     <li>amount must be positive</li>
 *     <li>booking date is required</li>
 *     <li>an expense item requires a category; an income item never has one</li>
 * </ul>
 * Identity (equals/hashCode) is based solely on the item id, as is appropriate for
 * a DDD entity.
 */
public final class BudgetItem {

    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private final BudgetItemId id;
    private final String description;
    private final Money amount;
    private final ItemType type;
    private final ExpenseCategory category;
    private final LocalDate bookingDate;

    private BudgetItem(BudgetItemId id, String description, Money amount, ItemType type, ExpenseCategory category,
                       LocalDate bookingDate) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = requireValidDescription(description);
        this.amount = requirePositive(amount);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = category;
        this.bookingDate = requireBookingDate(bookingDate);
    }

    public static BudgetItem income(BudgetItemId id, String description, Money amount, LocalDate bookingDate) {
        return new BudgetItem(id, description, amount, ItemType.INCOME, null, bookingDate);
    }

    public static BudgetItem expense(BudgetItemId id, String description, Money amount, ExpenseCategory category,
                                     LocalDate bookingDate) {
        if (category == null) {
            throw new InvalidBudgetItemException("category is required for expense items");
        }
        return new BudgetItem(id, description, amount, ItemType.EXPENSE, category, bookingDate);
    }

    private static String requireValidDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new InvalidBudgetItemException("description must not be blank");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidBudgetItemException(
                    "description must not be longer than %d characters".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return description;
    }

    private static Money requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidBudgetItemException("amount must be positive");
        }
        return amount;
    }

    private static LocalDate requireBookingDate(LocalDate bookingDate) {
        if (bookingDate == null) {
            throw new InvalidBudgetItemException("booking date is required");
        }
        return bookingDate;
    }

    public BudgetItemId id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public ItemType type() {
        return type;
    }

    public Optional<ExpenseCategory> category() {
        return Optional.ofNullable(category);
    }

    public LocalDate bookingDate() {
        return bookingDate;
    }

    public boolean isBookedIn(Year year) {
        return Year.from(bookingDate).equals(year);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BudgetItem other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BudgetItem{id=%s, description='%s', amount=%s, type=%s, category=%s, bookingDate=%s}"
                .formatted(id, description, amount, type, category, bookingDate);
    }
}
