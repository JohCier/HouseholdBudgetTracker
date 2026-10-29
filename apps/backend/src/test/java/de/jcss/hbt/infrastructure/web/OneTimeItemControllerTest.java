package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.application.onetimeitem.OneTimeItemApplicationService;
import de.jcss.hbt.domain.exception.OneTimeItemNotFoundException;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemDescription;
import de.jcss.hbt.domain.householdplan.valueobject.BudgetItemNote;
import de.jcss.hbt.domain.householdplan.valueobject.ItemType;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.OwnerId;
import de.jcss.hbt.domain.onetimeitem.valueobject.OneTimeItemId;
import de.jcss.hbt.domain.onetimeitem.command.ChangeOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.CreateOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.command.DeleteOneTimeItemCommand;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeItemOverview;
import de.jcss.hbt.domain.onetimeitem.overview.OneTimeOverviewItem;
import de.jcss.hbt.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The REST API of one-time items (EP-1 to EP-17), with a mocked application service and requests sent on
 * behalf of a signed-in user ({@link #OWNER}).
 */
@WebMvcTest(OneTimeItemController.class)
@Import(SecurityConfig.class)
class OneTimeItemControllerTest {

    private static final OwnerId OWNER = OwnerId.of(UUID.randomUUID());
    private static final OneTimeItemId ITEM_ID = OneTimeItemId.generate();
    private static final String BACK_PAYMENT = """
            {"type": "EXPENSE", "description": "Nebenkostennachzahlung", "amount": "1200.00",
             "date": "2026-03-15", "note": "Abrechnung 2025"}
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc anonymous;

    @MockitoBean
    private OneTimeItemApplicationService items;

    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject(OWNER.value().toString()))))
                .build();
    }

    /** EP-8, EP-9 */
    @Test
    void overviewListsTheItemsOfTheYearWithTheirTotals() throws Exception {
        when(items.getOverview(OWNER, Year.of(2026))).thenReturn(new OneTimeItemOverview(Year.of(2026), List.of(
                new OneTimeOverviewItem(ITEM_ID, ItemType.EXPENSE, BudgetItemDescription.of("Nebenkostennachzahlung"),
                        money("1200.00"), LocalDate.of(2026, 3, 15), BudgetItemNote.ofNullable("Abrechnung 2025")),
                new OneTimeOverviewItem(OneTimeItemId.generate(), ItemType.INCOME,
                        BudgetItemDescription.of("Steuererstattung"), money("800.00"), LocalDate.of(2026, 7, 10), null))));

        mockMvc.perform(get("/api/one-time-items").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(ITEM_ID.value().toString()))
                .andExpect(jsonPath("$.items[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.items[0].description").value("Nebenkostennachzahlung"))
                .andExpect(jsonPath("$.items[0].amount").value("1200.00"))
                .andExpect(jsonPath("$.items[0].date").value("2026-03-15"))
                .andExpect(jsonPath("$.items[0].note").value("Abrechnung 2025"))
                .andExpect(jsonPath("$.items[1].note").isEmpty())
                .andExpect(jsonPath("$.totalIncome").value("800.00"))
                .andExpect(jsonPath("$.totalExpense").value("1200.00"))
                .andExpect(jsonPath("$.balance").value("-400.00"))
                .andExpect(jsonPath("$.expensesExceedIncome").value(true));
    }

    /** EP-3 */
    @Test
    void overviewRejectsAYearOutOfRange() throws Exception {
        mockMvc.perform(get("/api/one-time-items").param("year", "1899")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/one-time-items").param("year", "2201")).andExpect(status().isBadRequest());
        verifyNoInteractions(items);
    }

    /** EP-1, EP-5 */
    @Test
    void createItemAnswersWithItsId() throws Exception {
        when(items.createItem(any())).thenReturn(ITEM_ID);

        mockMvc.perform(post("/api/one-time-items").contentType(MediaType.APPLICATION_JSON).content(BACK_PAYMENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ITEM_ID.value().toString()));

        verify(items).createItem(new CreateOneTimeItemCommand(OWNER, ItemType.EXPENSE,
                BudgetItemDescription.of("Nebenkostennachzahlung"), money("1200.00"), LocalDate.of(2026, 3, 15),
                new BudgetItemNote("Abrechnung 2025")));
    }

    /** EP-1 */
    @Test
    void aBlankNoteMeansNoNote() throws Exception {
        when(items.createItem(any())).thenReturn(ITEM_ID);

        mockMvc.perform(post("/api/one-time-items").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type": "INCOME", "description": "Bonus", "amount": "2000.00", "date": "2027-12-01", "note": " "}
                        """))
                .andExpect(status().isCreated());

        verify(items).createItem(new CreateOneTimeItemCommand(OWNER, ItemType.INCOME, BudgetItemDescription.of("Bonus"),
                money("2000.00"), LocalDate.of(2027, 12, 1), null));
    }

    /** EP-1 */
    @Test
    void createItemRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/one-time-items").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type": "EXPENSE", "description": "", "amount": "0.00"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:hbt:invalid-input"))
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.date").exists());
        verifyNoInteractions(items);
    }

    /** EP-7 */
    @Test
    void changeItemPassesEverythingOn() throws Exception {
        mockMvc.perform(put("/api/one-time-items/{id}", ITEM_ID.value())
                        .contentType(MediaType.APPLICATION_JSON).content(BACK_PAYMENT))
                .andExpect(status().isNoContent());

        verify(items).changeItem(new ChangeOneTimeItemCommand(OWNER, ITEM_ID, ItemType.EXPENSE,
                BudgetItemDescription.of("Nebenkostennachzahlung"), money("1200.00"), LocalDate.of(2026, 3, 15),
                new BudgetItemNote("Abrechnung 2025")));
    }

    /** EP-17 */
    @Test
    void deleteItemAnswersWithoutContent() throws Exception {
        mockMvc.perform(delete("/api/one-time-items/{id}", ITEM_ID.value())).andExpect(status().isNoContent());

        verify(items).deleteItem(new DeleteOneTimeItemCommand(OWNER, ITEM_ID));
    }

    /** EP-4 */
    @Test
    void anItemOfSomeoneElseIsNotFound() throws Exception {
        doThrow(new OneTimeItemNotFoundException(ITEM_ID)).when(items).deleteItem(any());
        doThrow(new OneTimeItemNotFoundException(ITEM_ID)).when(items).changeItem(any());

        mockMvc.perform(delete("/api/one-time-items/{id}", ITEM_ID.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:hbt:one-time-item-not-found"));
        mockMvc.perform(put("/api/one-time-items/{id}", ITEM_ID.value())
                        .contentType(MediaType.APPLICATION_JSON).content(BACK_PAYMENT))
                .andExpect(status().isNotFound());
    }

    @Test
    void needsASignedInUser() throws Exception {
        anonymous.perform(get("/api/one-time-items").param("year", "2026")).andExpect(status().isUnauthorized());
        verifyNoInteractions(items);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
